package com.example.celebrareproject;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class dashboard extends AppCompatActivity {

    private TextView tvWifiStatus;
    private ImageView ivWifiStatus, ivShare, btnSetting;
    private LinearLayout wifiLayout;
    private RelativeLayout backorange, backglobal;
    private SwitchCompat switchFloatingTools;
    private Button btnTutorial;

    private FrameLayout main;

    private boolean isUserInteracting = false;

    private BroadcastReceiver wifiReceiver;

    private Switch overlaySwitch;
    private static final int REQUEST_OVERLAY_PERMISSION = 1234;

    public static final String ACTION_LOCALE_CHANGED = "com.example.celebrareproject.ACTION_LOCALE_CHANGED";


    private final BroadcastReceiver localeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Recreate activity to apply new locale resources
            recreate();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(localeReceiver, new IntentFilter(ACTION_LOCALE_CHANGED));
        WindowCompat.setDecorFitsSystemWindows(getWindow(),
                false);
        final View main = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(getWindow(), main);

        wic.setAppearanceLightStatusBars(false);

       /* ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });*/



        overlaySwitch = findViewById(R.id.switch_overlay);
        ivShare = findViewById(R.id.ivShare);
        btnSetting = findViewById(R.id.btnSetting);
        btnTutorial = findViewById(R.id.btnTutorial);

        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(dashboard.this, settingPage.class);
                startActivity(intent);
            }
        });

        btnTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(dashboard.this, tutorialPage.class);
                startActivity(intent);
            }
        });

        ivShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Your App Link (Play Store link or website link)
                String appLink = "https://play.google.com/store/apps/details?id=com.example.yourapp";

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this App!");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "I found an amazing screen mirroring app to cast phone to TV! Download it here:\n" + appLink);

                startActivity(Intent.createChooser(shareIntent, "Share App using"));
            }
        });

        overlaySwitch.setChecked(Settings.canDrawOverlays(this));
        overlaySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!Settings.canDrawOverlays(dashboard.this)) {
                        showPermissionDialog();
                    } else {
                        startFloatingService();
                    }
                } else {
                    stopFloatingService();
                }
            }
        });

        tvWifiStatus = findViewById(R.id.tvWifiStatus);
        ivWifiStatus = findViewById(R.id.ivWifiStatus);
        wifiLayout = findViewById(R.id.wifiLayout);
        backorange = findViewById(R.id.backorange);
        backglobal = findViewById(R.id.backglobal);

        backorange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    // This opens the system "Cast" screen (for Miracast / Chromecast devices)
                    Intent intent = new Intent(Settings.ACTION_CAST_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    // Fallback for older Android versions that may not support ACTION_CAST_SETTINGS
                    Toast.makeText(dashboard.this, "Cast settings not available on this device", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();

                }}
            });

        backglobal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(dashboard.this, browserMirroring.class);
                startActivity(intent);
            }
        });

            checkWifiStatus();

        wifiLayout.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            startActivity(intent);
        });

        // Create the broadcast receiver
        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkWifiStatus();
            }
        };

    }
    public void onUserInteraction() {
        super.onUserInteraction();
        isUserInteracting = true;
    }

    private void checkWifiStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (wifiInfo != null && wifiInfo.isConnected()) {
            tvWifiStatus.setText(getString(R.string.wifi_connectes));
            tvWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            ivWifiStatus.setImageResource(R.drawable.ic_wifi);
        } else {
            tvWifiStatus.setText(getString(R.string.wifi_disconnectes));
            tvWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            ivWifiStatus.setImageResource(R.drawable.ic_wifi_off);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(wifiReceiver, filter);
        checkWifiStatus();

    }
    private void showPermissionDialog() {

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        Button btnOpen = dialogView.findViewById(R.id.btnOpen);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnOpen.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            overlaySwitch.setChecked(false);
            dialog.dismiss();
        });

        dialog.show();

    }

    //floating
    private void startFloatingService() {
        Intent intent = new Intent(this, FloatingViewService.class);
        startService(intent);
    }

    private void stopFloatingService() {
        Intent intent = new Intent(this, FloatingViewService.class);
        stopService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localeReceiver);
        super.onDestroy();
    }

    // Apply locale to base context so resources are loaded in correct language
    @Override
    protected void attachBaseContext(Context newBase) {
        String lang = LocaleHelper.getPersistedLanguage(newBase);
        // If Auto (empty) we use system default
        Context context = LocaleHelper.setLocale(newBase, lang);
        super.attachBaseContext(context);
    }
}
