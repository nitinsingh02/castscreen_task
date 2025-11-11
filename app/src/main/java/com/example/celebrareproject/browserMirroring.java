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

import androidx.appcompat.app.AppCompatActivity;

public class browserMirroring extends AppCompatActivity {

    public ImageView backimg , broWifi;
    public TextView broWifiStatus,tvIpAddress;
    private BroadcastReceiver wifiReceiver;

    private static final int REQUEST_CODE = 1000;

    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_mirroring);

       backimg = findViewById(R.id.backimg);
       broWifi = findViewById(R.id.broWifi);
       broWifiStatus = findViewById(R.id.broWifiStatus);
       tvIpAddress = findViewById(R.id.tvIpAddress);

        checkWifiStatusb();

       broWifi.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {

           }
       });

        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);


       backimg.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               onBackPressed();

           }
       });

        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkWifiStatusb();
            }
        };

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    private void checkWifiStatusb() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (wifiInfo != null && wifiInfo.isConnected()) {
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
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(wifiReceiver, filter);
        checkWifiStatusb();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Intent svc = new Intent(this, ScreenCaptureService.class);
            svc.putExtra("resultCode", resultCode);
            svc.putExtra("data", data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            }

            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            tvIpAddress.setText("Open on laptop:\nhttp://" + ip + ":8080");
        }
    }
}