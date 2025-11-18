package com.example.celebrareproject;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class PermissionActivity extends Activity {
    private static final int REQUEST_CODE = 200;
    public static final int REQ_CODE_CAPTURE = 12345;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MediaProjectionManager m = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (m != null) {
            Intent i = m.createScreenCaptureIntent();
            startActivityForResult(i, REQ_CODE_CAPTURE);
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }

        requestPermissions(new String[]{
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            Toast.makeText(this, "Permissions granted. Starting camera...", Toast.LENGTH_SHORT).show();
            startService(new Intent(this, FloatingCameraService.class));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // forward result to service
        Intent forward = new Intent(this, ScreenCastService.class);
        forward.setAction(ScreenCastService.ACTION_ON_PERMISSION_RESULT);
        forward.putExtra(ScreenCastService.EXTRA_RESULT_CODE, resultCode);
        forward.putExtra(ScreenCastService.EXTRA_RESULT_INTENT, data);
        startService(forward);
        finish();
    }
}
