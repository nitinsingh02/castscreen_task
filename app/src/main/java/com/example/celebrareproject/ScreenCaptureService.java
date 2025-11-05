package com.example.celebrareproject;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private static final String CHANNEL_ID = "cast_channel";
    private MediaProjection mediaProjection;
    private ScreenStreamServer server;
    private ImageReader imageReader;
    private boolean capturing = false;


    public int onStartCommand(Intent intent, int flags, int startId){
        Notification notification = createNotification();
        startForeground(1, createNotification());
        int resultCode = intent.getIntExtra("resultCode", 0);
        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager mgr =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mgr.getMediaProjection(resultCode, data);
        startCapture();
        return START_STICKY;
    }
    private void startCapture() {
        try {
            server = new ScreenStreamServer(8080);
            server.start();
        } catch (IOException e) { e.printStackTrace(); }

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        int width = metrics.widthPixels, height = metrics.heightPixels, density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        Surface surface = imageReader.getSurface();

        mediaProjection.createVirtualDisplay("CastDisplay",
                width, height, density, 0, surface, null, null);

        imageReader.setOnImageAvailableListener(reader -> {
            if (capturing) return;
            capturing = true;
            Image image = reader.acquireLatestImage();
            if (image != null) {
                try {
                    Image.Plane plane = image.getPlanes()[0];
                    ByteBuffer buffer = plane.getBuffer();
                    int pixelStride = plane.getPixelStride();
                    int rowStride = plane.getRowStride();
                    int rowPadding = rowStride - pixelStride * width;

                    Bitmap bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                    server.setLatestFrame(baos.toByteArray());
                    bitmap.recycle();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    image.close();
                    capturing = false;
                }
            }
        }, null);

        WifiManager wm2 = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm2.getConnectionInfo().getIpAddress());
        Log.i("CAST", "Open: http://" + ip + ":8080");
    }

    private Notification createNotification() {
        String CHANNEL_ID = "cast_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Screen Cast", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Cast Running")
                .setContentText("Visit http://<your-ip>:8080")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();
    }


    @Override public void onDestroy() {
        super.onDestroy();
        try {
            if (server != null) {
                server.stop();
                server = null;
            }
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (server != null) server.stop();
        if (mediaProjection != null) mediaProjection.stop();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
