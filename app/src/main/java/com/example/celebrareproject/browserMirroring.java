package com.example.celebrareproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.net.NetworkInterface;
import java.util.Enumeration;

public class browserMirroring extends AppCompatActivity {

    private static final int REQUEST_CODE = 1000;

    private ImageView backimg, broWifi;
    private TextView broWifiStatus;
    private TextView tvIpAddress;
    private ImageView ivCopy;
    private ImageView ivStopCast;

    private Button startScreen, stopScreen;

    private BroadcastReceiver wifiReceiver;
    private BroadcastReceiver serviceStateReceiver;
    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_mirroring);

        // views
        startScreen = findViewById(R.id.startScreen);
        stopScreen = findViewById(R.id.stopScreen);

        // initialize projection manager early so initCastUi can use it safely
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // init views and behavior
        initViews();
        initCastUi();
        setupWifiReceiver();
        setupServiceStateReceiver();
        checkWifiStatus();
    }

    @Override
    protected void attachBaseContext(Context base) {
        // preserve locale behavior if you use LocaleHelper in your app
        try {
            String lang = LocaleHelper.getPersistedLanguage(base);
            Context newContext = LocaleHelper.setLocale(base, lang);
            super.attachBaseContext(newContext);
        } catch (Exception e) {
            super.attachBaseContext(base);
        }
    }

    private void initViews() {
        backimg = findViewById(R.id.backimg);
        broWifi = findViewById(R.id.broWifi);
        broWifiStatus = findViewById(R.id.broWifiStatus);
        tvIpAddress = findViewById(R.id.tvIpAddress);
        ivCopy = findViewById(R.id.ivCopy);
        ivStopCast = findViewById(R.id.ivStopCast);

        backimg.setOnClickListener(v -> onBackPressed());
    }

    private void initCastUi() {
        // initial IP display
        String ip = getLocalIp();
        if (tvIpAddress != null) {
            tvIpAddress.setText("http://" + ip + ":8080");
        }

        // COPY button: only copy the url now
        if (ivCopy != null) {
            ivCopy.setOnClickListener(v -> {
                String url = tvIpAddress != null ? tvIpAddress.getText().toString() : "";
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData cd = android.content.ClipData.newPlainText("screencast_url", url);
                if (cm != null) cm.setPrimaryClip(cd);
                Toast.makeText(this, "Copied URL", Toast.LENGTH_SHORT).show();
            });
        }

        // START button: request screen capture permission (flow handled in onActivityResult)
        if (startScreen != null) {
            startScreen.setOnClickListener(v -> {
                if (projectionManager != null) {
                    Intent intent = projectionManager.createScreenCaptureIntent();
                    startActivityForResult(intent, REQUEST_CODE);
                } else {
                    Toast.makeText(this, "Projection not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // STOP button: stop the capture service and update UI
        if (stopScreen != null) {
            stopScreen.setOnClickListener(v -> {
                try {
                    stopService(new Intent(this, ScreenCaptureService.class));
                } catch (Exception ignored) {}
                if (ivStopCast != null) ivStopCast.setVisibility(View.GONE);
                Toast.makeText(this, "Screen cast stopped", Toast.LENGTH_SHORT).show();
            });
        }

        // ivStopCast (existing UI element) still acts as alternative stop control
        if (ivStopCast != null) {
            ivStopCast.setOnClickListener(v -> {
                try {
                    stopService(new Intent(this, ScreenCaptureService.class));
                } catch (Exception ignored) {}
                ivStopCast.setVisibility(View.GONE);
                Toast.makeText(this, "Screen cast stopped", Toast.LENGTH_SHORT).show();
            });
            ivStopCast.setVisibility(View.GONE); // default hidden
        }
    }

    private void setupWifiReceiver() {
        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkWifiStatus();
            }
        };
    }

    private void setupServiceStateReceiver() {
        serviceStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean running = intent != null && intent.getBooleanExtra(ScreenCaptureService.ACTION_CAST_STATUS_RUNNING, false);
                if (ivStopCast != null) ivStopCast.setVisibility(running ? View.VISIBLE : View.GONE);
            }
        };
    }

    private void checkWifiStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean connected = false;
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network network = cm.getActiveNetwork();
                if (network != null) {
                    android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                    if (caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                        connected = true;
                    }
                }
            } else {
                NetworkInfo info = cm.getActiveNetworkInfo();
                if (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI) {
                    connected = true;
                }
            }
        }

        if (connected) {
            broWifiStatus.setText("Wi-Fi Connected");
            broWifiStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            broWifi.setImageResource(R.drawable.ic_wifi);
        } else {
            broWifiStatus.setText("Wi-Fi Not Connected");
            broWifiStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            broWifi.setImageResource(R.drawable.ic_wifi_off);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register connectivity receiver
        if (wifiReceiver != null) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(wifiReceiver, filter);
        }

        // register service state receiver (Local)
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(serviceStateReceiver,
                    new IntentFilter(ScreenCaptureService.ACTION_CAST_STATUS));
        } catch (Exception ignored) {}

        checkWifiStatus();
        // update IP display
        if (tvIpAddress != null) tvIpAddress.setText("http://" + getLocalIp() + ":8080");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister receiver safely
        try {
            if (wifiReceiver != null) unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException ignored) { }

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceStateReceiver);
        } catch (IllegalArgumentException ignored) { }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // NOTE: do not forcibly stop the service here if you want casting to survive
        // when the user navigates away. Keep this empty (or remove the stopService call).
        // If you prefer to stop service when activity is destroyed, uncomment below:
        // stopService(new Intent(this, ScreenCaptureService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // user granted screen capture permission
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Intent svc = new Intent(this, ScreenCaptureService.class);
            svc.putExtra("resultCode", resultCode);
            svc.putExtra("data", data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }

            // show stop button when cast started
            if (ivStopCast != null) ivStopCast.setVisibility(View.VISIBLE);

            // update IP display again (service has started)
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null && tvIpAddress != null) {
                String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                tvIpAddress.setText("http://" + ip + ":8080");
            }
        }
    }

    private String getLocalIp() {
        try {
            // prefer WifiManager
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null) {
                int ipInt = wm.getConnectionInfo().getIpAddress();
                if (ipInt != 0) {
                    return ((ipInt & 0xff)) + "." + ((ipInt >> 8) & 0xff) + "." +
                            ((ipInt >> 16) & 0xff) + "." + ((ipInt >> 24) & 0xff);
                }
            }

            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (!intf.isUp()) continue;
                for (Enumeration<java.net.InetAddress> enumIp = intf.getInetAddresses(); enumIp.hasMoreElements();) {
                    java.net.InetAddress addr = enumIp.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "0.0.0.0";
    }
}
