package com.example.celebrareproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class dashboard extends AppCompatActivity {

    private TextView tvWifiStatus;
    private ImageView ivWifiStatus;
    private LinearLayout wifiLayout;
    private RelativeLayout backorange , backglobal;
    private SwitchCompat switchFloatingTools;

    private boolean isUserInteracting = false;

    private BroadcastReceiver wifiReceiver;

    private Switch overlaySwitch;
    private static final int REQUEST_OVERLAY_PERMISSION = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        overlaySwitch = findViewById(R.id.switch_overlay);

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
            tvWifiStatus.setText("Wi-Fi Connected");
            tvWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            ivWifiStatus.setImageResource(R.drawable.ic_wifi);
        } else {
            tvWifiStatus.setText("Wi-Fi Not Connected");
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
}
