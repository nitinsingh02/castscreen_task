package com.example.celebrareproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import java.util.ArrayList;

public class supportDevice extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_device);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Supported Devices");
        }

        RecyclerView rv = findViewById(R.id.rvDevices);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<Device> list = new ArrayList<>();

        list.add(new Device(
                "Smart TV",
                "WI-FI",
                "MIRACAST",
                "- There may be a confirmation on the TV Screen when you connect, click Allow.\n• Stay on the corresponding video source page.",
                R.drawable.ic_launcher_background,
                false
        ));

        list.add(new Device("Google Chromecast",
                "WIRELESS ADAPTER",
                null,
                "- Stay on the corresponding video source page.",
                R.drawable.ic_launcher_background, false));   // 2

        list.add(new Device("PC",
                "WI-FI",
                "BROWSER",
                "- Any PC with a web browser.(Chromaebook, Laptop, Tablet etc.)\n Use browser mirroring to mirror screen.",
                R.drawable.ic_launcher_background, true));           // 3

        list.add(new Device("Amazon Fire Stick",
                "WIRELESS ADAPTER",
                null,
                "- Open setting and find the Display and sound option. \n - Click Enable display mirroring and stay on that page.",
                R.drawable.ic_launcher_background , false));         // 4

        list.add(new Device("Roku",
                "WIRELESS ADAPTER",
                null,
                "-There may be a confirmation on the TV screen when you connect, click Allow. \n" +
                        "- Stay on the corresponding video source page.",
                R.drawable.ic_launcher_background, false));      // 5

        list.add(new Device("Smart Box",
                "WI-FI",
                "BROWSER",
                "- Smart Box with a web browser.(PS4,PS5, XBOX...) \n" +
                        "- Use the browser mirroring to mirror screen.",
                R.drawable.ic_launcher_background, true));       // 6

        list.add(new Device("Anycast",
                "WIRELESS ADAPTER",
                null,
                "- There may be several devices appearing on the list, you need to try each one to check which source can be used,\n -Stay on the corresponding video source page",
                R.drawable.ic_launcher_background, false));  // 7

        SupportedDeviceAdapter adapter = new SupportedDeviceAdapter(this, list);
        rv.setAdapter(adapter);
    }
}

/*
// ---------- Add 7 items ----------
// Replace R.drawable.* with your actual drawable resource names (put images into res/drawable)
        list.add(new Device(
                "Smart TV",
                "WI-FI",
                "MIRACAST",
                "- There may be a confirmation on the TV Screen when you connect, click Allow.\n• Stay on the corresponding video source page.",
                R.drawable.ic_launcher_background,
                false
                ));

                list.add(new Device("Google Chromecast",
                "WIRELESS ADAPTER",
                null,
                "• Stay on the corresponding video source page.",
                R.drawable.ic_launcher_background, false));   // 2

                list.add(new Device("PC",
                "WI-FI",
                "BROWSER",
                "- Any PC with a web browser.(Chromaebook, Laptop, Tablet etc.)\n Use browser mirroring to mirror screen.",
                R.drawable.ic_launcher_background, true));           // 3

                list.add(new Device("Amazon Fire Stick",
                "WIRELESS ADAPTER",
                null,
                "- Open setting and find the Display and sound option. \n - Click Enable display mirroring and stay on that page.",
                R.drawable.ic_launcher_background , false));         // 4

                list.add(new Device("Roku",
                "WIRELESS ADAPTER",
                null,
                "-There may be a confirmation on the TV screen when you connect, click Allow. \n" +
                "- Stay on the corresponding video source page.",
                R.drawable.ic_launcher_background, false));      // 5

                list.add(new Device("Smart Box",
                "WI-FI",
                "BROWSER",
                "- Smart Box with a web browser.(PS4,PS5, XBOX...) \n" +
                "- Use the browser mirroring to mirror screen.",
                R.drawable.ic_launcher_background, true));       // 6

                list.add(new Device("Anycast",
                "WIRELESS ADAPTER",
                null,
                "- There may be several devices appearing on the list, you need to try each one to check which source can be used,\n -Stay on the corresponding video source page",
                R.drawable.ic_launcher_background, false));  // 7
*/
