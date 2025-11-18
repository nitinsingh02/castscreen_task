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

    private BroadcastReceiver wifiReceiver;
    private BroadcastReceiver serviceStateReceiver;
    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_mirroring);

        // initialize projection manager early so initCastUi can use it safely
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // init views and behavior
        initViews();
        initCastUi();
        setupWifiReceiver();
        setupServiceStateReceiver();
        checkWifiStatus();
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
        String ip = getLocalIp();
        if (tvIpAddress != null) {
            tvIpAddress.setText("http://" + ip + ":8080");
        }

        if (ivCopy != null) {
            ivCopy.setOnClickListener(v -> {
                // copy url to clipboard
                String url = tvIpAddress != null ? tvIpAddress.getText().toString() : "";
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData cd = android.content.ClipData.newPlainText("screencast_url", url);
                if (cm != null) cm.setPrimaryClip(cd);
                Toast.makeText(this, "Copied URL", Toast.LENGTH_SHORT).show();

                // start projection intent to request screen capture permission
                if (projectionManager != null) {
                    Intent intent = projectionManager.createScreenCaptureIntent();
                    startActivityForResult(intent, REQUEST_CODE);
                } else {
                    Toast.makeText(this, "Projection not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (ivStopCast != null) {
            ivStopCast.setOnClickListener(v -> {
                // stop capture service
                stopService(new Intent(this, ScreenCaptureService.class));
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
                ivStopCast.setVisibility(running ? View.VISIBLE : View.GONE);
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
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceStateReceiver,
                new IntentFilter(ScreenCaptureService.ACTION_CAST_STATUS));

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
        // ensure service cleanup if needed
        stopService(new Intent(this, ScreenCaptureService.class));
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



/*
package com.example.celebrareproject;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.net.NetworkInterface;
import java.util.Enumeration;

public class browserMirroring extends AppCompatActivity {

    private static final int SERVER_PORT = 8080;

    private ImageView backimg, broWifi, ivCopy, ivStopCast;
    private TextView broWifiStatus, tvIpAddress;

    private MediaProjectionManager projectionManager;
    private ActivityResultLauncher<Intent> projectionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_mirroring);

        // views (use your existing IDs)
        backimg = findViewById(R.id.backimg);
        broWifi = findViewById(R.id.broWifi);
        broWifiStatus = findViewById(R.id.broWifiStatus);
        tvIpAddress = findViewById(R.id.tvIpAddress);
        ivCopy = findViewById(R.id.ivCopy);
        ivStopCast = findViewById(R.id.ivStopCast);

        ivStopCast.setVisibility(View.GONE); // hidden until cast starts

        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        projectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent svc = new Intent(this, ScreenCaptureService.class);
                        svc.putExtra("resultCode", result.getResultCode());
                        svc.putExtra("data", result.getData());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc);
                        else startService(svc);

                        ivStopCast.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Casting started", Toast.LENGTH_SHORT).show();
                        updateIpDisplay();
                    } else {
                        Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        backimg.setOnClickListener(v -> onBackPressed());

        ivCopy.setOnClickListener(v -> {
            String url = tvIpAddress.getText().toString();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("cast_url", url));
            Toast.makeText(this, "Copied URL", Toast.LENGTH_SHORT).show();

            // Request screen capture permission and start casting
            if (projectionManager != null) {
                Intent intent = projectionManager.createScreenCaptureIntent();
                projectionLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Projection not available", Toast.LENGTH_SHORT).show();
            }
        });

        ivStopCast.setOnClickListener(v -> {
            stopService(new Intent(this, ScreenCaptureService.class));
            ivStopCast.setVisibility(View.GONE);
            Toast.makeText(this, "Casting stopped", Toast.LENGTH_SHORT).show();
        });

        // Basic wifi status check (keeps your earlier behavior)
        updateWifiStatus();
        updateIpDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWifiStatus();
        updateIpDisplay();
    }

    private void updateWifiStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean wifiConnected = false;
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI) wifiConnected = true;
        }
        if (wifiConnected) {
            broWifiStatus.setText("Wi-Fi Connected");
            broWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            broWifi.setImageResource(R.drawable.ic_wifi);
        } else {
            broWifiStatus.setText("Wi-Fi Not Connected");
            broWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            broWifi.setImageResource(R.drawable.ic_wifi_off);
        }
    }

    private void updateIpDisplay() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ip = "0.0.0.0";
        if (wm != null) ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        tvIpAddress.setText("http://" + ip + ":" + SERVER_PORT);
    }

    // fallback IP enumeration (if wifi API fails)
    private String getLocalIpFallback() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (!intf.isUp()) continue;
                for (Enumeration<java.net.InetAddress> enumIp = intf.getInetAddresses(); enumIp.hasMoreElements();) {
                    java.net.InetAddress addr = enumIp.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) return addr.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "0.0.0.0";
    }
}




*/
/*
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.NetworkInterface;
import java.util.Enumeration;

public class browserMirroring extends AppCompatActivity {

    private static final int REQUEST_CODE = 1000;

    private ImageView backimg, broWifi;
    private TextView broWifiStatus;
    private TextView tvIpAddress;
    private ImageView ivCopy;
    private ImageView ivStopCast;

    private BroadcastReceiver wifiReceiver;
    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_mirroring);

        // initialize projection manager early so initCastUi can use it safely
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // init views and behavior
        initViews();
        initCastUi();
        setupWifiReceiver();
        checkWifiStatus();
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
        String ip = getLocalIp();
        if (tvIpAddress != null) {
            tvIpAddress.setText("http://" + ip + ":8080");
        }

        if (ivCopy != null) {
            ivCopy.setOnClickListener(v -> {
                // copy url to clipboard
                String url = tvIpAddress != null ? tvIpAddress.getText().toString() : "";
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData cd = android.content.ClipData.newPlainText("screencast_url", url);
                if (cm != null) cm.setPrimaryClip(cd);
                Toast.makeText(this, "Copied URL", Toast.LENGTH_SHORT).show();

                // start projection intent to request screen capture permission
                if (projectionManager != null) {
                    Intent intent = projectionManager.createScreenCaptureIntent();
                    startActivityForResult(intent, REQUEST_CODE);
                } else {
                    Toast.makeText(this, "Projection not available", Toast.LENGTH_SHORT).show();
                }

                // show stop button when cast started (visibility will be toggled in onActivityResult)
                if (ivStopCast != null) ivStopCast.setVisibility(View.VISIBLE);
            });
        }

        if (ivStopCast != null) {
            ivStopCast.setOnClickListener(v -> {
                // stop services
                stopService(new Intent(this, ScreenCaptureService.class));
                Toast.makeText(this, "Screen cast stopped", Toast.LENGTH_SHORT).show();

                Intent stopIntent = new Intent(this, ScreenCastService.class);
                stopIntent.setAction(ScreenCastService.ACTION_STOP);
                startService(stopIntent);

                ivStopCast.setVisibility(View.GONE);
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

    private void checkWifiStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean connected = false;
        if (cm != null) {
            // getActiveNetworkInfo is deprecated in API 29+, but works for broad compatibility here
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI) {
                connected = true;
            }
        }

        if (connected) {
            broWifiStatus.setText("Wi-Fi Connected");
            broWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            broWifi.setImageResource(R.drawable.ic_wifi);
        } else {
            broWifiStatus.setText("Wi-Fi Not Connected");
            broWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ensure service cleanup if needed
        stopService(new Intent(this, ScreenCaptureService.class));
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
*/

