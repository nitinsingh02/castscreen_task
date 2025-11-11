package com.example.celebrareproject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Stack;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1000;
    private MediaProjectionManager projectionManager;
    private TextView tvInfo;
    public Button dashpage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Wi-Fi Screen Cast");
        }

         dashpage =findViewById(R.id.dashpage);

        dashpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, dashboard.class);
                startActivity(intent);

            }
        });

        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);
        tvInfo = findViewById(R.id.tvInfo);

        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);


        btnStart.setOnClickListener(v -> {
            stopService(new Intent(this, ScreenCaptureService.class));
            Intent intent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_CODE);
        });
        btnStop.setOnClickListener(v -> {

            stopService(new Intent(this, ScreenCaptureService.class));
            Toast.makeText(this, "Screen cast stopped", Toast.LENGTH_SHORT).show();
            tvInfo.setText("Your IP will appear here");
        });
      //  btnStop.setOnClickListener(v -> stopService(new Intent(this, ScreenCaptureService.class)));
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
            tvInfo.setText("Open on laptop:\nhttp://" + ip + ":8080");
        }
    }
}