# Gold Motion True3D — Stage 1 Prototype

This branch contains the first isolated compositor prototype for replacing Gold Motion's per-layer `android.graphics.Camera` / `android.graphics.Matrix` pseudo-3D path with a real shared 3D camera pipeline inspired by Open Motion.

## Scope of Stage 1

Stage 1 deliberately does **not** replace the entire Gold Motion renderer yet. It proves the core compositor in isolation:

- one textured quad representing a Gold Motion layer result;
- 4x4 Model matrix;
- 4x4 camera World matrix;
- `View = inverse(CameraWorld)`;
- perspective Projection matrix from FOV/aspect/near/far;
- `MVP = Projection * View * Model`;
- XYZ camera rotation;
- XYZ layer rotation;
- real OpenGL depth testing;
- world-space XZ floor grid;
- preview-only rendering; no export path is touched.

The main implementation is `True3DPreviewView.java`. `Stage1HarnessActivity.java` is only a test harness so the compositor can be verified before it is connected to Gold Motion's editor preview.

## Coordinate convention

The prototype uses:

- X = horizontal/right;
- Y = vertical/up;
- Z = depth;
- zero-rotation camera looks toward `-Z`;
- floor grid lies on `Y = 0` (XZ plane).

Open Motion-style composition order is preserved:

```text
model       = T * Rz * Ry * Rx * S
cameraWorld = T * Rz * Ry * Rx
view        = inverse(cameraWorld)
projection  = perspective(FOV, aspect, near, far)
MVP         = projection * view * model
```

The camera never edits a layer's stored XYZ rotation. Relative orientation exists only through the View matrix.

## Why this is different from the current Gold Motion renderer

The existing Gold Motion XYZ implementation still reduces 3D transforms into independent `android.graphics.Camera.rotateX/rotateY()` calls and a 3x3 `android.graphics.Matrix` for each layer. That can produce foreshortening but there is no single shared 4x4 View/Projection space for the whole scene.

Stage 1 renders the grid and textured layer through exactly the same `Projection * View` matrix and enables a real depth buffer. If the camera rotates, both the grid and the layer move in one consistent world space.

## Running the harness

When transplanted into an Android project, register the harness Activity temporarily:

```xml
<activity
    android:name="com.openmotion.prototype.true3d.Stage1HarnessActivity"
    android:screenOrientation="sensorLandscape" />
```

Launch it directly. Drag one finger to rotate camera pitch/yaw. The checkerboard quad and floor grid must maintain one coherent perspective.

This harness is **not** intended to remain in the production Gold Motion build.

## Gold Motion integration point

The eventual editor integration should mount `True3DPreviewView` inside the existing preview container. For Stage 1, the old Gold renderer may remain behind/disabled while the test compositor is active.

The API intentionally exposes the minimum bridge needed later:

```java
preview.setCamera(x, y, z, rx, ry, rz, fov);
preview.setQuadTransform(x, y, z, rx, ry, rz, sx, sy, sz);
preview.setQuadSize(width, height);
preview.setLayerBitmap(bitmap);
preview.setShowGrid(true);
```

For the first Gold test, a single Gold-rendered layer can be copied to a `Bitmap` and passed through `setLayerBitmap()`. That copy is deliberately acceptable only for Stage 1 validation.

The next integration step should replace the Bitmap copy with a zero-copy texture/FBO handoff:

```text
Gold source/effects/mask renderer
          ↓
      GL texture
          ↓
 True3D textured quad
          ↓
 Model/View/Projection + depth
          ↓
 editor preview / export FBO
```

## Preview grid

The grid is generated as actual line geometry on the XZ plane and goes through the same View/Projection matrix as the textured quad. It is therefore a world-space reference, not a 2D perspective decoration.

Grid rendering exists only in `True3DPreviewView`; Stage 1 does not modify Gold Motion's export path. This guarantees the grid cannot appear in export at this stage.

## Acceptance criteria for Stage 1

1. Rotating the camera X/Y/Z changes both grid and quad consistently.
2. Rotating the quad itself changes its orientation without changing the camera.
3. Moving the quad in Z changes perspective size naturally.
4. FOV changes the projection of both grid and quad together.
5. Depth testing is active.
6. The floor grid never becomes a project layer.
7. No legacy Gold Motion project serialization is changed.
8. Export remains untouched.

## Stage 2 boundary

Do not start Stage 2 until Stage 1 passes visually on-device.

Stage 2 will locate Gold Motion's post-effects layer texture/FBO and feed real image/text/shape output into this compositor. The old per-layer final Canvas transform can then be bypassed one layer type at a time.
