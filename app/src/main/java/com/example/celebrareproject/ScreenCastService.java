package com.example.celebrareproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Robust ScreenCastService:
 * - probes for a working ImageReader format (RGBA -> YUV)
 * - converts frames accordingly and streams MJPEG on port 5621
 *
 * Requires:
 * - PermissionActivity to request MediaProjection permission
 * - Service declared with android:foregroundServiceType="mediaProjection" in manifest
 */
public class ScreenCastService extends Service {
    private static final String TAG = "ScreenCastService";

    public static final String ACTION_START_SERVER = "com.example.celebrareproject.ACTION_START_SERVER";
    public static final String ACTION_STOP = "com.example.celebrareproject.ACTION_STOP";
    public static final String ACTION_ON_PERMISSION_RESULT = "com.example.celebrareproject.ACTION_ON_PERMISSION_RESULT";

    public static final String EXTRA_RESULT_CODE = "extra_result_code";
    public static final String EXTRA_RESULT_INTENT = "extra_result_intent";

    // networking
    private ServerSocket serverSocket;
    private volatile boolean serverRunning = false;
    private final ExecutorService exec = Executors.newCachedThreadPool();

    // projection
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private Surface surface;
    private HandlerThread imageHandlerThread;
    private Handler imageHandler;
    private volatile boolean projecting = false;

    // capture params
    private int displayWidth, displayHeight, densityDpi;
    private int captureWidth, captureHeight;
    private final int JPEG_QUALITY = 70;
    private final long FRAME_INTERVAL_MS = 100; // ~10 fps default

    // chosen image format (one of ImageFormat constants)
    private int chosenImageFormat = -1; // ImageFormat.RGBA_8888 or ImageFormat.YUV_420_888

    // notification
    private static final String CHANNEL_ID = "scast_channel";
    private static final int NOTIF_ID = 4001;

    @Override
    public void onCreate() {
        super.onCreate();

        // display metrics
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getRealMetrics(dm);
        displayWidth = dm.widthPixels;
        displayHeight = dm.heightPixels;
        densityDpi = dm.densityDpi;

        if (displayWidth > 1280) {
            float scale = 1280f / displayWidth;
            captureWidth = 1280;
            captureHeight = Math.max(640, (int) (displayHeight * scale));
        } else {
            captureWidth = displayWidth;
            captureHeight = displayHeight;
        }

        createNotificationChannel();
        Log.i(TAG, "onCreate: display " + displayWidth + "x" + displayHeight + " capture " + captureWidth + "x" + captureHeight);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        if (ACTION_START_SERVER.equals(action)) {
            startServer();
            showForegroundNotification("ScreenCast server running");
        } else if (ACTION_STOP.equals(action)) {
            stopProjectionAndServer();
            stopSelf();
        } else if (ACTION_ON_PERMISSION_RESULT.equals(action)) {
            // ensure foreground before MediaProjection (Android requirement)
            showForegroundNotification("Starting projection");

            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra(EXTRA_RESULT_INTENT);
            if (resultCode == Activity.RESULT_OK && data != null) {
                startProjectionWithProbe(resultCode, data);
            } else {
                stopProjectionAndServer();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopProjectionAndServer();
        exec.shutdownNow();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ------------------ server ------------------
    private void startServer() {
        if (serverRunning) return;
        serverRunning = true;
        exec.execute(() -> {
            try {
                serverSocket = new ServerSocket(5621);
                Log.i(TAG, "Server listening on 5621");
                while (serverRunning) {
                    Socket client = serverSocket.accept();
                    exec.execute(() -> handleClient(client));
                }
            } catch (Exception e) {
                Log.w(TAG, "Server error: " + e.getMessage(), e);
            } finally {
                serverRunning = false;
            }
        });
    }

    private void stopServer() {
        serverRunning = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
    }

    // ------------------ projection probing ------------------
    /**
     * Try to create ImageReader with several formats. We attempt RGBA_8888 first (fast path),
     * then YUV_420_888. If a format works we create the virtual display using that ImageReader.
     */
    @SuppressLint("WrongConstant")
    private void startProjectionWithProbe(int resultCode, Intent data) {
        if (projecting) return;
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (mpm == null) {
            Log.e(TAG, "MediaProjectionManager null");
            return;
        }
        mediaProjection = mpm.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e(TAG, "mediaProjection null");
            return;
        }

        // create handler thread
        imageHandlerThread = new HandlerThread("img-reader");
        imageHandlerThread.start();
        imageHandler = new Handler(imageHandlerThread.getLooper());

        // attempt formats in order
        final int[] tryFormats = new int[] { ImageFormat.FLEX_RGBA_8888, ImageFormat.YUV_420_888 };
        boolean ok = false;
        for (int fmt : tryFormats) {
            try {
                ImageReader tmpReader = ImageReader.newInstance(captureWidth, captureHeight, fmt, 2);
                // attempt to create virtual display quickly and then release it if OK
                Surface tmpSurface = tmpReader.getSurface();
                // create a temp virtual display and immediately release -> but we want persistent one below
                mediaProjection.createVirtualDisplay("probe", captureWidth, captureHeight, densityDpi, 0, tmpSurface, null, imageHandler);
                // if no exception, choose this format
                // Important: stop the temporary display created by createVirtualDisplay; it will be reclaimed when we create the real one, but safe to stop projection below and re-create proper reader
                // Actually createVirtualDisplay returns VirtualDisplay which we don't hold — however on some devices calling twice is ok. We'll now release tmpReader and continue to create final reader.
                tmpReader.close();
                ok = true;
                chosenImageFormat = fmt;
                Log.i(TAG, "Probed image format OK: " + fmt);
                break;
            } catch (UnsupportedOperationException | IllegalArgumentException e) {
                Log.i(TAG, "Format " + fmt + " not supported: " + e.getMessage());
                // try next
            } catch (Exception e) {
                Log.w(TAG, "Probe error for format " + fmt + ": " + e.getMessage(), e);
            }
        }

        // If no format succeeded, fallback to YUV attempt directly (some devices behave differently)
        if (!ok) {
            try {
                chosenImageFormat = ImageFormat.YUV_420_888;
                Log.i(TAG, "Falling back to YUV_420_888");
            } catch (Exception e) {
                Log.e(TAG, "No fallback format available", e);
                stopProjectionAndServer();
                return;
            }
        }

        // Now create final ImageReader and virtual display using chosen format
        try {
            imageReader = ImageReader.newInstance(captureWidth, captureHeight, chosenImageFormat, 2);
            surface = imageReader.getSurface();

            mediaProjection.createVirtualDisplay("screencast", captureWidth, captureHeight, densityDpi,
                    0, surface, null, imageHandler);

            // keep the ImageReader alive; clients will acquireLatestImage when streaming
            imageReader.setOnImageAvailableListener(reader -> {
                Image img = null;
                try { img = reader.acquireLatestImage(); }
                finally { if (img != null) img.close(); }
            }, imageHandler);

            projecting = true;
            Log.i(TAG, "Projection started with format: " + chosenImageFormat);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create ImageReader/VirtualDisplay with format " + chosenImageFormat, e);
            stopProjectionAndServer();
        }
    }

    private void stopProjectionAndServer() {
        projecting = false;
        try {
            if (imageReader != null) {
                imageReader.setOnImageAvailableListener(null, null);
                imageReader.close();
                imageReader = null;
            }
        } catch (Exception ignored) {}

        try {
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
        } catch (Exception ignored) {}

        try {
            if (imageHandlerThread != null) {
                imageHandlerThread.quitSafely();
                imageHandlerThread = null;
                imageHandler = null;
            }
        } catch (Exception ignored) {}

        stopServer();
        Log.i(TAG, "Stopped projection & server");
    }

    // ------------------ client handler ------------------
    private void handleClient(Socket socket) {
        try (Socket s = socket;
             java.io.InputStream in = s.getInputStream();
             OutputStream out = s.getOutputStream()) {

            byte[] buf = new byte[4096];
            int r = in.read(buf);
            if (r <= 0) return;
            String req = new String(buf, 0, r);
            Log.i(TAG, "Request: " + req.split("\n")[0]);

            if (req.contains("GET /stream")) {
                if (!projecting) {
                    Intent it = new Intent(getApplicationContext(), PermissionActivity.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(it);

                    int waited = 0;
                    while (!projecting && waited < 30000) {
                        Thread.sleep(200);
                        waited += 200;
                    }
                }

                if (!projecting || imageReader == null) {
                    String resp = "HTTP/1.1 503 Service Unavailable\r\nContent-Type:text/plain\r\nConnection: close\r\n\r\nProjection not available";
                    out.write(resp.getBytes());
                    out.flush();
                    return;
                }

                String header = "HTTP/1.1 200 OK\r\n" +
                        "Connection: close\r\n" +
                        "Cache-Control: no-cache\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Content-Type: multipart/x-mixed-replace; boundary=--boundary\r\n\r\n";
                out.write(header.getBytes());
                out.flush();

                // stream loop
                while (!s.isClosed() && projecting) {
                    Image img = null;
                    try {
                        img = imageReader.acquireLatestImage();
                        if (img == null) {
                            Thread.sleep(Math.max(10, FRAME_INTERVAL_MS));
                            continue;
                        }

                        byte[] jpegBytes = null;

                        if (chosenImageFormat == ImageFormat.FLEX_RGB_888) {
                            // convert RGBA buffer -> Bitmap -> JPEG
                            Image.Plane[] planes = img.getPlanes();
                            ByteBuffer buffer = planes[0].getBuffer();
                            int pixelStride = planes[0].getPixelStride();
                            int rowStride = planes[0].getRowStride();
                            int width = img.getWidth();
                            int height = img.getHeight();
                            // create bitmap with correct stride handling
                            Bitmap bmp = Bitmap.createBitmap(rowStride / pixelStride, height, Bitmap.Config.ARGB_8888);
                            bmp.copyPixelsFromBuffer(buffer);
                            Bitmap cropped = Bitmap.createBitmap(bmp, 0, 0, width, height);
                            bmp.recycle();

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
                            jpegBytes = baos.toByteArray();
                            baos.close();
                            cropped.recycle();

                        } else { // YUV_420_888
                            // convert YUV -> NV21 -> compress
                            byte[] nv21 = convertYUV420ToNV21(img);
                            if (nv21 == null) {
                                img.close();
                                Thread.sleep(50);
                                continue;
                            }
                            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, img.getWidth(), img.getHeight(), null);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            yuvImage.compressToJpeg(new Rect(0, 0, img.getWidth(), img.getHeight()), JPEG_QUALITY, baos);
                            jpegBytes = baos.toByteArray();
                            baos.close();
                        }

                        if (jpegBytes != null) {
                            String partHeader = "--boundary\r\n" +
                                    "Content-Type: image/jpeg\r\n" +
                                    "Content-Length: " + jpegBytes.length + "\r\n\r\n";
                            out.write(partHeader.getBytes());
                            out.write(jpegBytes);
                            out.write("\r\n".getBytes());
                            out.flush();
                        }

                        Thread.sleep(FRAME_INTERVAL_MS);
                    } catch (Exception e) {
                        Log.w(TAG, "Stream err: " + e.getMessage());
                        break;
                    } finally {
                        if (img != null) img.close();
                    }
                }
            } else {
                String page = "<html><body style='margin:0;background:#000;color:#fff;'><div style='padding:8px;color:#ffb86b;'>Open /stream to view</div>" +
                        "<img src=\"/stream\" style='width:100%;height:auto;'/>" +
                        "</body></html>";
                String resp = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: " + page.getBytes().length + "\r\n\r\n" + page;
                out.write(resp.getBytes());
                out.flush();
            }

        } catch (Exception e) {
            Log.w(TAG, "Client handler exception: " + e.getMessage(), e);
        } finally {
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Convert YUV_420_888 Image to NV21 byte[] for use with YuvImage.
     * Handles rowStride and pixelStride.
     */
    private byte[] convertYUV420ToNV21(Image image) {
        if (image == null) return null;
        Image.Plane[] planes = image.getPlanes();
        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer yBuf = planes[0].getBuffer();
        ByteBuffer uBuf = planes[1].getBuffer();
        ByteBuffer vBuf = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        byte[] nv21 = new byte[width * height * 3 / 2];
        int pos = 0;

        // copy Y
        byte[] row = new byte[yRowStride];
        for (int rowIdx = 0; rowIdx < height; rowIdx++) {
            yBuf.position(rowIdx * yRowStride);
            int length = Math.min(yRowStride, width);
            yBuf.get(row, 0, yRowStride);
            System.arraycopy(row, 0, nv21, pos, width);
            pos += width;
        }

        // interleave VU (NV21)
        byte[] uRow = new byte[uvRowStride];
        byte[] vRow = new byte[uvRowStride];
        int chromaHeight = height / 2;
        int chromaWidth = width / 2;
        for (int rowIdx = 0; rowIdx < chromaHeight; rowIdx++) {
            uBuf.position(rowIdx * uvRowStride);
            vBuf.position(rowIdx * uvRowStride);
            uBuf.get(uRow, 0, Math.min(uvRowStride, uRow.length));
            vBuf.get(vRow, 0, Math.min(uvRowStride, vRow.length));
            for (int col = 0; col < chromaWidth; col++) {
                int uIndex = col * uvPixelStride;
                int vIndex = col * uvPixelStride;
                byte u = uRow[uIndex];
                byte v = vRow[vIndex];
                nv21[pos++] = v;
                nv21[pos++] = u;
            }
        }
        return nv21;
    }

    // ------------------ notifications & util ------------------
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "ScreenCast", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void showForegroundNotification(String text) {
        String ip = getLocalIpAddress();
        String content = (ip == null || ip.isEmpty() || ip.equals("0.0.0.0")) ? "Open server on port 5621" : "Open http://" + ip + ":5621 on PC";

        Intent stop = new Intent(this, ScreenCastService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent pStop = PendingIntent.getService(this, 2, stop, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ScreenCast Service")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pStop)
                .setOngoing(true);

        startForeground(NOTIF_ID, b.build());
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (!intf.isUp()) continue;
                for (Enumeration<InetAddress> enumIp = intf.getInetAddresses(); enumIp.hasMoreElements();) {
                    InetAddress addr = enumIp.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "0.0.0.0";
    }
}
