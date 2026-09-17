package com.openmotion.prototype.true3d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.AttributeSet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Stage 1 prototype compositor for Gold Motion.
 *
 * Goals:
 * - one textured 3D quad
 * - Model -> View -> Projection 4x4 pipeline
 * - XYZ camera rotation
 * - depth test
 * - preview-only world grid
 *
 * This class deliberately does NOT touch export. It is meant to be mounted only
 * inside Gold Motion's editor preview container while the existing renderer is
 * kept intact for all non-prototype paths.
 */
public final class True3DPreviewView extends GLSurfaceView {
    private final True3DRenderer renderer;

    public True3DPreviewView(Context context) {
        this(context, null);
    }

    public True3DPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        // Explicit 16-bit depth buffer; GLSurfaceView defaults are not guaranteed
        // to provide depth, and Stage 1 must prove actual depth testing.
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setPreserveEGLContextOnPause(true);
        renderer = new True3DRenderer();
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void setCamera(float x, float y, float z,
                          float rxDeg, float ryDeg, float rzDeg,
                          float fovDeg) {
        renderer.setCamera(x, y, z, rxDeg, ryDeg, rzDeg, fovDeg);
        requestRender();
    }

    public void setQuadTransform(float x, float y, float z,
                                 float rxDeg, float ryDeg, float rzDeg,
                                 float sx, float sy, float sz) {
        renderer.setQuadTransform(x, y, z, rxDeg, ryDeg, rzDeg, sx, sy, sz);
        requestRender();
    }

    public void setQuadSize(float widthWorld, float heightWorld) {
        renderer.setQuadSize(widthWorld, heightWorld);
        requestRender();
    }

    /**
     * Later integration point: pass the final Gold Motion layer result here.
     * Stage 1 accepts a Bitmap so the 3D math can be validated before we wire a
     * zero-copy GL texture/FBO handoff from Gold's compositor.
     */
    public void setLayerBitmap(Bitmap bitmap) {
        renderer.setLayerBitmap(bitmap);
        requestRender();
    }

    public void setShowGrid(boolean show) {
        renderer.setShowGrid(show);
        requestRender();
    }

    private static final class True3DRenderer implements Renderer {
        private static final String QUAD_VS =
                "uniform mat4 uMVP;\n" +
                "attribute vec3 aPosition;\n" +
                "attribute vec2 aTexCoord;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main(){\n" +
                "  gl_Position = uMVP * vec4(aPosition, 1.0);\n" +
                "  vTexCoord = aTexCoord;\n" +
                "}";

        private static final String QUAD_FS =
                "precision mediump float;\n" +
                "uniform sampler2D uTexture;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main(){\n" +
                "  vec4 c = texture2D(uTexture, vTexCoord);\n" +
                "  if (c.a < 0.01) discard;\n" +
                "  gl_FragColor = c;\n" +
                "}";

        private static final String LINE_VS =
                "uniform mat4 uVP;\n" +
                "attribute vec3 aPosition;\n" +
                "void main(){ gl_Position = uVP * vec4(aPosition, 1.0); }";

        private static final String LINE_FS =
                "precision mediump float;\n" +
                "uniform vec4 uColor;\n" +
                "void main(){ gl_FragColor = uColor; }";

        private final Object stateLock = new Object();

        private int viewportWidth = 1;
        private int viewportHeight = 1;

        // Coordinate convention kept intentionally close to Open Motion:
        // Y is up, camera looks along -Z at zero rotation, floor is XZ at Y=0.
        private float camX = 0f;
        private float camY = 350f;
        private float camZ = 1400f;
        private float camRx = -12f;
        private float camRy = 0f;
        private float camRz = 0f;
        private float fovDeg = 50f;

        private float quadX = 0f;
        private float quadY = 280f;
        private float quadZ = 0f;
        private float quadRx = 0f;
        private float quadRy = 0f;
        private float quadRz = 0f;
        private float quadSx = 1f;
        private float quadSy = 1f;
        private float quadSz = 1f;
        private float quadWidth = 600f;
        private float quadHeight = 338f;

        private boolean showGrid = true;
        private Bitmap pendingBitmap;

        private int quadProgram;
        private int lineProgram;
        private int textureId;

        private int quadMvpLoc;
        private int quadPosLoc;
        private int quadUvLoc;
        private int quadSamplerLoc;

        private int lineVpLoc;
        private int linePosLoc;
        private int lineColorLoc;

        private FloatBuffer quadBuffer;
        private FloatBuffer minorGrid;
        private FloatBuffer majorGrid;
        private FloatBuffer axisX;
        private FloatBuffer axisZ;
        private int minorGridVertexCount;
        private int majorGridVertexCount;

        private final float[] cameraWorld = new float[16];
        private final float[] view = new float[16];
        private final float[] projection = new float[16];
        private final float[] vp = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.055f, 0.06f, 0.075f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            quadProgram = GlUtil.linkProgram(QUAD_VS, QUAD_FS);
            lineProgram = GlUtil.linkProgram(LINE_VS, LINE_FS);

            quadMvpLoc = GLES20.glGetUniformLocation(quadProgram, "uMVP");
            quadPosLoc = GLES20.glGetAttribLocation(quadProgram, "aPosition");
            quadUvLoc = GLES20.glGetAttribLocation(quadProgram, "aTexCoord");
            quadSamplerLoc = GLES20.glGetUniformLocation(quadProgram, "uTexture");

            lineVpLoc = GLES20.glGetUniformLocation(lineProgram, "uVP");
            linePosLoc = GLES20.glGetAttribLocation(lineProgram, "aPosition");
            lineColorLoc = GLES20.glGetUniformLocation(lineProgram, "uColor");

            textureId = createTexture();
            rebuildQuadBuffer();
            rebuildGrid(5000f, 100f, 500f);

            Bitmap fallback = makeTestBitmap(512, 288);
            uploadTexture(fallback);
            fallback.recycle();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            viewportWidth = Math.max(1, width);
            viewportHeight = Math.max(1, height);
            GLES20.glViewport(0, 0, viewportWidth, viewportHeight);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            maybeUploadPendingBitmap();

            final float cx, cy, cz, crx, cry, crz, fov;
            final float qx, qy, qz, qrx, qry, qrz, qsx, qsy, qsz;
            final boolean grid;
            synchronized (stateLock) {
                cx = camX; cy = camY; cz = camZ;
                crx = camRx; cry = camRy; crz = camRz; fov = fovDeg;
                qx = quadX; qy = quadY; qz = quadZ;
                qrx = quadRx; qry = quadRy; qrz = quadRz;
                qsx = quadSx; qsy = quadSy; qsz = quadSz;
                grid = showGrid;
            }

            // Open Motion reference order:
            // cameraWorld = T * Rz * Ry * Rx
            Matrix.setIdentityM(cameraWorld, 0);
            Matrix.translateM(cameraWorld, 0, cx, cy, cz);
            Matrix.rotateM(cameraWorld, 0, crz, 0f, 0f, 1f);
            Matrix.rotateM(cameraWorld, 0, cry, 0f, 1f, 0f);
            Matrix.rotateM(cameraWorld, 0, crx, 1f, 0f, 0f);
            if (!Matrix.invertM(view, 0, cameraWorld, 0)) {
                Matrix.setIdentityM(view, 0);
            }

            float aspect = (float) viewportWidth / (float) viewportHeight;
            Matrix.perspectiveM(projection, 0, clamp(fov, 5f, 150f), aspect, 1f, 100000f);
            Matrix.multiplyMM(vp, 0, projection, 0, view, 0);

            if (grid) {
                drawGrid(vp);
            }

            // model = T * Rz * Ry * Rx * S
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, qx, qy, qz);
            Matrix.rotateM(model, 0, qrz, 0f, 0f, 1f);
            Matrix.rotateM(model, 0, qry, 0f, 1f, 0f);
            Matrix.rotateM(model, 0, qrx, 1f, 0f, 0f);
            Matrix.scaleM(model, 0, qsx, qsy, qsz);
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0);

            drawQuad(mvp);
        }

        void setCamera(float x, float y, float z,
                       float rx, float ry, float rz, float fov) {
            synchronized (stateLock) {
                camX = x; camY = y; camZ = z;
                camRx = rx; camRy = ry; camRz = rz;
                fovDeg = fov;
            }
        }

        void setQuadTransform(float x, float y, float z,
                              float rx, float ry, float rz,
                              float sx, float sy, float sz) {
            synchronized (stateLock) {
                quadX = x; quadY = y; quadZ = z;
                quadRx = rx; quadRy = ry; quadRz = rz;
                quadSx = sx; quadSy = sy; quadSz = sz;
            }
        }

        void setQuadSize(float width, float height) {
            synchronized (stateLock) {
                quadWidth = Math.max(1f, width);
                quadHeight = Math.max(1f, height);
            }
            // Buffer rebuild is intentionally tiny; safe to defer until draw.
            rebuildQuadBuffer();
        }

        void setShowGrid(boolean value) {
            synchronized (stateLock) {
                showGrid = value;
            }
        }

        void setLayerBitmap(Bitmap bitmap) {
            synchronized (stateLock) {
                pendingBitmap = bitmap;
            }
        }

        private void rebuildQuadBuffer() {
            final float w;
            final float h;
            synchronized (stateLock) {
                w = quadWidth * 0.5f;
                h = quadHeight * 0.5f;
            }
            // XYZ + UV. V is flipped because Android Bitmap top-left differs from GL UV.
            float[] v = {
                    -w, -h, 0f,  0f, 1f,
                     w, -h, 0f,  1f, 1f,
                    -w,  h, 0f,  0f, 0f,
                     w,  h, 0f,  1f, 0f
            };
            quadBuffer = floatBuffer(v);
        }

        private void drawQuad(float[] matrix) {
            GLES20.glUseProgram(quadProgram);
            GLES20.glUniformMatrix4fv(quadMvpLoc, 1, false, matrix, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(quadSamplerLoc, 0);

            quadBuffer.position(0);
            GLES20.glEnableVertexAttribArray(quadPosLoc);
            GLES20.glVertexAttribPointer(quadPosLoc, 3, GLES20.GL_FLOAT, false, 5 * 4, quadBuffer);
            quadBuffer.position(3);
            GLES20.glEnableVertexAttribArray(quadUvLoc);
            GLES20.glVertexAttribPointer(quadUvLoc, 2, GLES20.GL_FLOAT, false, 5 * 4, quadBuffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(quadPosLoc);
            GLES20.glDisableVertexAttribArray(quadUvLoc);
        }

        private void drawGrid(float[] viewProjection) {
            GLES20.glUseProgram(lineProgram);
            GLES20.glUniformMatrix4fv(lineVpLoc, 1, false, viewProjection, 0);
            GLES20.glEnableVertexAttribArray(linePosLoc);

            drawLines(minorGrid, minorGridVertexCount, 0.36f, 0.39f, 0.46f, 0.26f);
            drawLines(majorGrid, majorGridVertexCount, 0.48f, 0.52f, 0.62f, 0.48f);
            drawLines(axisX, 2, 0.90f, 0.25f, 0.28f, 0.72f);
            drawLines(axisZ, 2, 0.20f, 0.55f, 1.00f, 0.72f);

            GLES20.glDisableVertexAttribArray(linePosLoc);
        }

        private void drawLines(FloatBuffer buffer, int count, float r, float g, float b, float a) {
            if (buffer == null || count <= 0) return;
            buffer.position(0);
            GLES20.glVertexAttribPointer(linePosLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, buffer);
            GLES20.glUniform4f(lineColorLoc, r, g, b, a);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, count);
        }

        private void rebuildGrid(float extent, float minorStep, float majorStep) {
            FloatArrayBuilder minor = new FloatArrayBuilder();
            FloatArrayBuilder major = new FloatArrayBuilder();
            int steps = (int) (extent / minorStep);
            for (int i = -steps; i <= steps; i++) {
                float p = i * minorStep;
                boolean isMajor = Math.abs(p % majorStep) < 0.001f;
                FloatArrayBuilder target = isMajor ? major : minor;
                // Lines parallel to Z.
                target.line(p, 0f, -extent, p, 0f, extent);
                // Lines parallel to X.
                target.line(-extent, 0f, p, extent, 0f, p);
            }
            minorGrid = floatBuffer(minor.toArray());
            majorGrid = floatBuffer(major.toArray());
            minorGridVertexCount = minor.size / 3;
            majorGridVertexCount = major.size / 3;
            axisX = floatBuffer(new float[]{-extent, 0f, 0f, extent, 0f, 0f});
            axisZ = floatBuffer(new float[]{0f, 0f, -extent, 0f, 0f, extent});
        }

        private int createTexture() {
            int[] ids = new int[1];
            GLES20.glGenTextures(1, ids, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            return ids[0];
        }

        private void maybeUploadPendingBitmap() {
            Bitmap b;
            synchronized (stateLock) {
                b = pendingBitmap;
                pendingBitmap = null;
            }
            if (b != null && !b.isRecycled()) {
                uploadTexture(b);
            }
        }

        private void uploadTexture(Bitmap bitmap) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        }

        private static Bitmap makeTestBitmap(int width, int height) {
            Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.rgb(27, 31, 47));
            c.drawColor(p.getColor());
            int cell = 32;
            for (int y = 0; y < height; y += cell) {
                for (int x = 0; x < width; x += cell) {
                    p.setColor(((x / cell + y / cell) & 1) == 0
                            ? Color.rgb(79, 91, 145)
                            : Color.rgb(42, 49, 78));
                    c.drawRect(x, y, Math.min(width, x + cell), Math.min(height, y + cell), p);
                }
            }
            p.setColor(Color.WHITE);
            p.setTextSize(42f);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText("TRUE 3D STAGE 1", width * 0.5f, height * 0.52f, p);
            return b;
        }

        private static FloatBuffer floatBuffer(float[] values) {
            FloatBuffer out = ByteBuffer.allocateDirect(values.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            out.put(values).position(0);
            return out;
        }

        private static float clamp(float x, float lo, float hi) {
            return Math.max(lo, Math.min(hi, x));
        }
    }

    private static final class GlUtil {
        static int linkProgram(String vertexSource, String fragmentSource) {
            int vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vs);
            GLES20.glAttachShader(program, fs);
            GLES20.glLinkProgram(program);
            int[] ok = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, ok, 0);
            if (ok[0] == 0) {
                String log = GLES20.glGetProgramInfoLog(program);
                GLES20.glDeleteProgram(program);
                throw new RuntimeException("GL link failed: " + log);
            }
            GLES20.glDeleteShader(vs);
            GLES20.glDeleteShader(fs);
            return program;
        }

        private static int compileShader(int type, String source) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] ok = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0);
            if (ok[0] == 0) {
                String log = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                throw new RuntimeException("GL shader compile failed: " + log);
            }
            return shader;
        }
    }

    private static final class FloatArrayBuilder {
        private float[] data = new float[256];
        private int size;

        void line(float ax, float ay, float az, float bx, float by, float bz) {
            add(ax); add(ay); add(az);
            add(bx); add(by); add(bz);
        }

        private void add(float v) {
            if (size == data.length) {
                float[] next = new float[data.length * 2];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = v;
        }

        float[] toArray() {
            float[] out = new float[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }
    }
}
