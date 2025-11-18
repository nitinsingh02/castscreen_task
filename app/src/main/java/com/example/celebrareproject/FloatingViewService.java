package com.example.celebrareproject;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class FloatingViewService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private ImageView mainButton,imgEdit,imgVideo,imgHome,imgClose;
    private LinearLayout menuLayout;
    private boolean isMenuVisible = false;

    private long touchStartTime;
    private static final int CLICK_THRESHOLD = 200;

    private static final int REQUEST_CODE_PERMISSIONS = 101;



    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not binding
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);


        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null);

         params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 200;

        windowManager.addView(floatingView, params);

        mainButton = floatingView.findViewById(R.id.floating_button);
        menuLayout = floatingView.findViewById(R.id.floating_menu);
        imgEdit = floatingView.findViewById(R.id.imgEdit);
        imgVideo = floatingView.findViewById(R.id.imgVideo);
        imgHome = floatingView.findViewById(R.id.imgHome);
        imgClose = floatingView.findViewById(R.id.imgClose);

        mainButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchStartTime = System.currentTimeMillis();
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_UP:
                        long clickDuration = System.currentTimeMillis() - touchStartTime;
                        if (clickDuration < CLICK_THRESHOLD) {
                            toggleMenu();
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        // --- Menu button listeners ---
        imgClose.setOnClickListener(v -> {
            menuLayout.setVisibility(View.GONE);
            isMenuVisible = false;
        });

        imgEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start paint overlay service (assumes overlay permission already granted)
                Intent serviceIntent = new Intent(getApplicationContext(), FloatingPaintService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }
        });
        imgVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                requestPermissions();
                if (checkPermissions()) {
                    startCameraService();
                    Toast.makeText(FloatingViewService.this, "clicked", Toast.LENGTH_SHORT).show();

                } else {
                    requestPermissions();
                    Toast.makeText(FloatingViewService.this, "Video clicked", Toast.LENGTH_SHORT).show();

                }
            }
        });
        imgHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), dashboard.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }


    private void toggleMenu() {
        if (isMenuVisible) {
            menuLayout.setVisibility(View.GONE);
        } else {
            menuLayout.setVisibility(View.VISIBLE);
        }
        isMenuVisible = !isMenuVisible;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
    }

    private boolean checkPermissions() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M ||
                (checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermissions() {
        Intent intent = new Intent(getApplicationContext(), PermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void startCameraService() {
        Intent serviceIntent = new Intent(getApplicationContext(), FloatingCameraService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
