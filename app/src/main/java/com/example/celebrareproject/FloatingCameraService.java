package com.example.celebrareproject;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.camera2.*;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FloatingCameraService extends Service {

    private static final String CHANNEL_ID = "FloatingCameraChannel";
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private CameraDevice cameraDevice;
    private CameraCaptureSession session;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFrontCamera = true;
    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;

    private CameraCaptureSession captureSession;
    private Surface recorderSurface;
    private Surface previewSurface;
    private String videoPath;


    private ImageButton btnRecord, btnSwitch, btnClose;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        startForeground(1, getNotification("Camera Ready"));

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_camera, null);

        params = new WindowManager.LayoutParams(
                500,
                600,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 300;

        surfaceView = floatingView.findViewById(R.id.surfaceView);
        btnRecord = floatingView.findViewById(R.id.btnRecord);
        btnSwitch = floatingView.findViewById(R.id.btnSwitch);
        btnClose = floatingView.findViewById(R.id.btnClose);

        holder = surfaceView.getHolder();

        windowManager.addView(floatingView, params);

        btnRecord.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });


        // Allow user to drag window
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
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

        btnRecord.setOnClickListener(v -> {
            if (!isRecording) startRecording();
            else stopRecording();
        });

        btnSwitch.setOnClickListener(v -> switchCamera());
        btnClose.setOnClickListener(v -> stopSelf());

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                openCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                closeCamera();
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Floating Camera",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Floating Camera")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();
    }

    private void openCamera() {
        try {
            cameraId = getCameraId(isFrontCamera);
            if (cameraId == null) {
                Log.e("FloatingCameraService", "No suitable camera found");
                return;
            }

            if (checkSelfPermission(android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        startPreview();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        camera.close();
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        camera.close();
                    }
                }, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String getCameraId(boolean front) throws CameraAccessException {
        for (String id : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (front && facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return id;
            } else if (!front && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return null;
    }


  /*  private void openCamera() {
        try {
            cameraId = cameraManager.getCameraIdList()[isFrontCamera ? 1 : 0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    startPreview();
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    private void startPreview() {
        try {
            Surface surface = holder.getSurface();
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session_) {
                    session = session_;
                    try {
                        session.setRepeatingRequest(builder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {}
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (session != null) {
            session.close();
            session = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

  /*  private void startRecording() {
        if (cameraDevice == null) return;

        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "FloatingRecorder");
            if (!dir.exists()) dir.mkdirs();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(dir, "VID_" + timeStamp + ".mp4");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(file.getAbsolutePath());
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncodingBitRate(5_000_000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(1280, 720);
            mediaRecorder.prepare();

            Surface recorderSurface = mediaRecorder.getSurface();
            Surface previewSurface = holder.getSurface();

            final CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(previewSurface);
            builder.addTarget(recorderSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, recorderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session_) {
                    session = session_;
                    try {
                        session.setRepeatingRequest(builder.build(), null, null);
                        mediaRecorder.start();
                        isRecording = true;
                        btnRecord.setImageResource(android.R.drawable.ic_media_pause);
                        updateNotification("Recording...");
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {}
            }, null);
        } catch (IOException | CameraAccessException e) {
            e.printStackTrace();
        }
    }*/

    private void startRecording() {
        try {
            if (cameraDevice == null) return;

            closePreviewSession();
            setupMediaRecorder();

            SurfaceHolder holder = surfaceView.getHolder();
            previewSurface = holder.getSurface();
            recorderSurface = mediaRecorder.getSurface();

            final CaptureRequest.Builder builder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(previewSurface);
            builder.addTarget(recorderSurface);

            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(previewSurface);
            surfaces.add(recorderSurface);

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        session.setRepeatingRequest(builder.build(), null, null);
                        mediaRecorder.start();
                        isRecording = true;
                        btnRecord.setImageResource(android.R.drawable.ic_media_pause);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e("Camera", "CaptureSession configuration failed");
                }
            }, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        try {
            if (!isRecording) return;
            isRecording = false;
            btnRecord.setImageResource(android.R.drawable.ic_media_play);

            captureSession.stopRepeating();
            captureSession.abortCaptures();
            mediaRecorder.stop();
            mediaRecorder.reset();

            Toast.makeText(this, "Saved: " + videoPath, Toast.LENGTH_LONG).show();

            // reopen preview after stop
            createCameraPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void createCameraPreview() {
        try {
            SurfaceHolder holder = surfaceView.getHolder();
            previewSurface = holder.getSurface();

            final CaptureRequest.Builder builder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                session.setRepeatingRequest(builder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        }
                    }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void closePreviewSession() {
        if (captureSession != null) {
            try {
                captureSession.close();
            } catch (Exception ignored) {}
            captureSession = null;
        }
    }

    private void setupMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "FloatingRecorder");
        if (!dir.exists()) dir.mkdirs();

        videoPath = new File(dir, System.currentTimeMillis() + ".mp4").getAbsolutePath();
        mediaRecorder.setOutputFile(videoPath);

        mediaRecorder.setVideoEncodingBitRate(8_000_000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(1280, 720);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mediaRecorder.prepare();
    }



   /* private void stopRecording() {
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRecording = false;
        btnRecord.setImageResource(android.R.drawable.ic_media_play);
        updateNotification("Camera Ready");
        startPreview();
    }*/

    private void switchCamera() {
        isFrontCamera = !isFrontCamera;
        closeCamera();
        openCamera();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1, getNotification(text));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeCamera();
        if (windowManager != null && floatingView != null) {
            windowManager.removeView(floatingView);
        }
        stopForeground(true);
    }
}
