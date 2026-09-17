package com.openmotion.prototype.true3d;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

/**
 * Minimal test harness for the Stage 1 compositor.
 *
 * One-finger drag rotates the camera yaw/pitch. The test quad and floor grid are
 * rendered by the exact same Model/View/Projection path, so perspective and
 * camera rotation can be validated independently from Gold Motion's old Canvas
 * renderer.
 */
public final class Stage1HarnessActivity extends Activity {
    private True3DPreviewView preview;
    private float yaw;
    private float pitch = -12f;
    private float lastX;
    private float lastY;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        preview = new True3DPreviewView(this);
        preview.setCamera(0f, 350f, 1400f, pitch, yaw, 0f, 50f);
        preview.setQuadTransform(0f, 280f, 0f, 0f, 0f, 0f, 1f, 1f, 1f);
        preview.setShowGrid(true);
        preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getX();
                        lastY = event.getY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - lastX;
                        float dy = event.getY() - lastY;
                        lastX = event.getX();
                        lastY = event.getY();
                        yaw += dx * 0.15f;
                        pitch += dy * 0.15f;
                        pitch = Math.max(-89f, Math.min(89f, pitch));
                        preview.setCamera(0f, 350f, 1400f, pitch, yaw, 0f, 50f);
                        return true;
                    default:
                        return true;
                }
            }
        });
        setContentView(preview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        preview.onResume();
    }

    @Override
    protected void onPause() {
        preview.onPause();
        super.onPause();
    }
}
