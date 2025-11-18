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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    public static final String CHANNEL_ID = "cast_channel";
    public static final String ACTION_CAST_STATUS = "com.example.ACTION_CAST_STATUS";
    public static final String ACTION_CAST_STATUS_RUNNING = "running";

    private MediaProjection mediaProjection;
    private ScreenStreamServer server;
    private ImageReader imageReader;
    private boolean capturing = false;
    private android.hardware.display.VirtualDisplay virtualDisplay;
    private HandlerThread captureThread;
    private Handler captureHandler;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // create and start foreground notification once
        Notification notification = createNotification();
        startForeground(1, notification);

        int resultCode = intent != null ? intent.getIntExtra("resultCode", 0) : 0;
        Intent data = intent != null ? intent.getParcelableExtra("data") : null;

        MediaProjectionManager mgr =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mgr == null || data == null) {
            Log.e(TAG, "MediaProjectionManager or data is null, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        mediaProjection = mgr.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e(TAG, "mediaProjection is null, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        // start background thread for image processing
        captureThread = new HandlerThread("ScreenCaptureThread");
        captureThread.start();
        captureHandler = new Handler(captureThread.getLooper());

        startCapture();

        // broadcast that service started (UI can show stop button)
        Intent started = new Intent(ACTION_CAST_STATUS);
        started.putExtra(ACTION_CAST_STATUS_RUNNING, true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(started);

        return START_STICKY;
    }

    private void startCapture() {
        try {
            server = new ScreenStreamServer(8080);
            server.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to start ScreenStreamServer", e);
        }

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm == null) {
            Log.e(TAG, "WindowManager is null");
            return;
        }
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        final int width = metrics.widthPixels;
        final int height = metrics.heightPixels;
        final int density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        Surface surface = imageReader.getSurface();

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "CastDisplay",
                width, height, density,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
        );

        imageReader.setOnImageAvailableListener(reader -> {
            if (capturing) return;
            capturing = true;

            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image == null) return;

                Image.Plane[] planes = image.getPlanes();
                if (planes == null || planes.length == 0) return;

                Image.Plane plane = planes[0];
                ByteBuffer buffer = plane.getBuffer();
                int pixelStride = plane.getPixelStride();
                int rowStride = plane.getRowStride();
                if (pixelStride == 0) return;

                int rowPadding = rowStride - pixelStride * width;
                int bitmapWidth = width + rowPadding / pixelStride;

                Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);

                Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                cropped.compress(Bitmap.CompressFormat.JPEG, 65, baos);
                byte[] jpeg = baos.toByteArray();

                if (server != null) {
                    server.setLatestFrame(jpeg);
                }

                cropped.recycle();
                bitmap.recycle();
                baos.close();
            } catch (Exception e) {
                Log.e(TAG, "Error processing image", e);
            } finally {
                if (image != null) image.close();
                capturing = false;
            }
        }, captureHandler);

        // log accessible URL using IPv4 conversion
        WifiManager wm2 = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = ipFromInt(wm2 != null ? wm2.getConnectionInfo().getIpAddress() : 0);
        if (!TextUtils.isEmpty(ip) && !"0.0.0.0".equals(ip)) {
            Log.i(TAG, "Open: http://" + ip + ":8080");
        } else {
            Log.i(TAG, "IP not available yet or not connected to Wi-Fi");
        }
    }

    private String ipFromInt(int ip) {
        return ((ip & 0xff)) + "." + ((ip >> 8) & 0xff) + "." +
                ((ip >> 16) & 0xff) + "." + ((ip >> 24) & 0xff);
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Screen Cast", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Cast Running")
                .setContentText("Tap to open cast")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // broadcast that service stopped (UI can hide stop button)
        Intent stopped = new Intent(ACTION_CAST_STATUS);
        stopped.putExtra(ACTION_CAST_STATUS_RUNNING, false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(stopped);

        try {
            if (imageReader != null) {
                imageReader.setOnImageAvailableListener(null, null);
                imageReader.close();
                imageReader = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error closing imageReader", e);
        }

        try {
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error releasing virtualDisplay", e);
        }

        try {
            if (server != null) {
                server.stop();
                server = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error stopping server", e);
        }

        try {
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error stopping mediaProjection", e);
        }

        if (captureThread != null) {
            captureThread.quitSafely();
            captureThread = null;
            captureHandler = null;
        }

        Log.i(TAG, "ScreenCaptureService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}



/*
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
        } catch (IOException e) {
            e.printStackTrace();
        }

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
*/
