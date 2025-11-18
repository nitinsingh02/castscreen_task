package com.example.celebrareproject;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class settingPage extends AppCompatActivity {

    RelativeLayout btnShareMessenger, btnShareTelegram, btnShareWhatsapp, btnMore;
    ImageView iv_back;
    LinearLayout lytFeedback;

    private LinearLayout languageRow;
    private TextView tvLanguageValue;
    private String[] languageNames;
    private String[] languageCodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_page);

        // find views
        btnShareMessenger = findViewById(R.id.btnShareMessenger);
        btnShareTelegram = findViewById(R.id.btnShareTelegram);
        btnShareWhatsapp = findViewById(R.id.btnShareWhatsapp);
        btnMore = findViewById(R.id.btnMore);
        iv_back = findViewById(R.id.iv_back);

        languageRow = findViewById(R.id.language_row);
        tvLanguageValue = findViewById(R.id.tvLanguageValue);
        lytFeedback = findViewById(R.id.lytFeedback);

        Resources res = getResources();
        languageNames = res.getStringArray(R.array.language_names);
        languageCodes = res.getStringArray(R.array.language_codes);

        // Feedback click -> show dialog
        lytFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFeedbackDialog();
            }
        });

        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // initialize language display
        String savedCode = LocaleHelper.getPersistedLanguage(this); // "" means Auto
        int selectedIndex = findIndexForCode(savedCode);
        tvLanguageValue.setText(languageNames[selectedIndex]);

        // language picker
        languageRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int currentIndex = findIndexForCode(LocaleHelper.getPersistedLanguage(settingPage.this));

                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(settingPage.this);
                builder.setTitle(getString(R.string.select_language));
                builder.setSingleChoiceItems(languageNames, currentIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // which = index selected
                        String code = languageCodes[which];

                        // persist selection
                        LocaleHelper.persistLanguage(settingPage.this, code);

                        // apply locale to current context
                        LocaleHelper.setLocale(settingPage.this, code);

                        // update the UI element
                        tvLanguageValue.setText(languageNames[which]);

                        // restart activity to apply to all UI strings (simple approach)
                        dialog.dismiss();
                        recreate(); // or restart app for global refresh
                    }
                });

                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
            }
        });

        // Share: Messenger
        btnShareMessenger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String packageName = "com.facebook.orca"; // Messenger package name
                String shareText = "I found an amazing screen mirroring app to cast phone to TV! Download it here: https://play.google.com/store/apps/details?id=" + getPackageName();

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                sendIntent.setType("text/plain");
                sendIntent.setPackage(packageName);

                try {
                    startActivity(sendIntent); // Try to open Messenger
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(settingPage.this, "Messenger is not installed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Share: Telegram
        btnShareTelegram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String packageName = "org.telegram.messenger"; // Telegram package
                String shareText = "I found an amazing screen mirroring app to cast phone to TV! Download it here: https://play.google.com/store/apps/details?id=" + getPackageName();

                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                sendIntent.setType("text/plain");
                sendIntent.setPackage(packageName);

                try {
                    startActivity(sendIntent);  // Try opening Telegram
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(settingPage.this, "Telegram is not installed", Toast.LENGTH_SHORT).show();
                }
            }

        });

        // Share: WhatsApp
        btnShareWhatsapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String packageName = "com.whatsapp"; // WhatsApp package
                String shareText = "I found an amazing screen mirroring app to cast phone to TV! Download it here: https://play.google.com/store/apps/details?id=" + getPackageName();

                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                sendIntent.setType("text/plain");
                sendIntent.setPackage(packageName);

                try {
                    startActivity(sendIntent);  // Try opening WhatsApp
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(settingPage.this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // More: chooser
        btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String appLink = "https://play.google.com/store/apps/details?id=" + getPackageName();

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this App!");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "I found an amazing screen mirroring app to cast phone to TV! Download it here:\n" + appLink);

                startActivity(Intent.createChooser(shareIntent, "Share App using"));
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        String lang = LocaleHelper.getPersistedLanguage(base);
        Context newContext = LocaleHelper.setLocale(base, lang);
        super.attachBaseContext(newContext);
    }

    private int findIndexForCode(String code) {
        if (code == null) code = "";
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equalsIgnoreCase(code)) return i;
        }
        return 0; // default to Auto (index 0)
    }

    // ----------------------------------------
    // Email opening using mailto: URI (reliable for subject+body)
    // ----------------------------------------
    private void openEmailClient(String to, String subject, String body) {
        try {
            // Build mailto URI with encoded subject and body
            String uriText = "mailto:" + Uri.encode(to) +
                    "?subject=" + Uri.encode(subject) +
                    "&body=" + Uri.encode(body);
            Uri mailUri = Uri.parse(uriText);

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(mailUri);

            // Use chooser to let user pick preferred mail app
            startActivity(Intent.createChooser(emailIntent, getString(R.string.feedback_send)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, getString(R.string.no_email_app), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // fallback for pre-JB devices to estimate total memory by reading /proc/meminfo
    private double getTotalMemoryLegacy() {
        double total = 0;
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("/proc/meminfo"));
            String line = br.readLine(); // first line: MemTotal: xxxx kB
            if (line != null) {
                line = line.replaceAll("[^0-9]", "");
                if (!line.isEmpty()) {
                    total = Double.parseDouble(line) / 1024.0; // MB
                }
            }
            br.close();
        } catch (Exception ignored) {
        }
        return total;
    }

    // Builds a multi-line device & environment information string
    private String buildDeviceInfo() {
        StringBuilder sb = new StringBuilder();

        // Basic device info
        sb.append("Brand: ").append(Build.BRAND).append("\n");
        sb.append("Model: ").append(Build.MODEL).append("\n");
        sb.append("Device: ").append(Build.DEVICE).append("\n");
        sb.append("Hardware: ").append(Build.HARDWARE).append("\n");
        sb.append("CPU ABIs: ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sb.append(TextUtils.join(", ", Build.SUPPORTED_ABIS));
        } else {
            sb.append(Build.CPU_ABI).append(", ").append(Build.CPU_ABI2);
        }
        sb.append("\n");

        // Android OS
        sb.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");

        // CPU cores
        int cores = Runtime.getRuntime().availableProcessors();
        sb.append("CPU cores: ").append(cores).append("\n");

        // Screen metrics (px and dp)
        DisplayMetrics dm = new DisplayMetrics();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        } else {
            getWindowManager().getDefaultDisplay().getMetrics(dm);
        }
        int widthPx = dm.widthPixels;
        int heightPx = dm.heightPixels;
        float density = dm.density;
        int densityDpi = dm.densityDpi;
        float widthDp = widthPx / density;
        float heightDp = heightPx / density;

        sb.append("Screen: ").append(widthPx).append(" x ").append(heightPx).append(" px")
                .append(" (").append(Math.round(widthDp)).append(" x ").append(Math.round(heightDp)).append(" dp)")
                .append("\n");
        sb.append("Density: ").append(density).append(" (dpi ").append(densityDpi).append(")\n");

        // Memory info
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        if (activityManager != null) {
            activityManager.getMemoryInfo(memInfo);

            double availMB = memInfo.availMem / 1024.0 / 1024.0;
            double totalMB = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) ?
                    memInfo.totalMem / 1024.0 / 1024.0 : getTotalMemoryLegacy();

            DecimalFormat df = new DecimalFormat("#.##");
            sb.append("Total RAM: ").append(df.format(totalMB)).append(" MB\n");
            sb.append("Available RAM: ").append(df.format(availMB)).append(" MB\n");
            sb.append("Low memory threshold: ").append(memInfo.threshold / 1024 / 1024).append(" MB\n");
            sb.append("Is low memory device: ").append(memInfo.lowMemory).append("\n");
        }

        // GMT / timezone info + current GMT time
        java.util.TimeZone tz = java.util.TimeZone.getDefault();
        int offsetMillis = tz.getOffset(System.currentTimeMillis());
        int offsetHours = offsetMillis / (1000 * 60 * 60);
        int offsetMinutes = Math.abs((offsetMillis / (1000 * 60)) % 60);
        sb.append("Time zone: ").append(tz.getID())
                .append(" (GMT").append(offsetHours >= 0 ? "+" : "").append(offsetHours)
                .append(":").append(String.format(Locale.getDefault(), "%02d", offsetMinutes)).append(")\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'Z", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        sb.append("Current GMT time: ").append(sdf.format(new Date())).append("\n");

        // App and package info (optional)
        try {
            String pkg = getPackageName();
            String version = getPackageManager().getPackageInfo(pkg, 0).versionName;
            sb.append("App package: ").append(pkg).append("\n");
            sb.append("App version: ").append(version).append("\n");
        } catch (Exception e) {
            // ignore
        }

        return sb.toString();
    }

    private void showFeedbackDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_feedback, null);

        final EditText etFeedback = dialogView.findViewById(R.id.et_feedback);
        TextView tvTitle = dialogView.findViewById(R.id.tv_feedback_title);
        Button btnCancel = dialogView.findViewById(R.id.btn_feedback_cancel);
        Button btnSend = dialogView.findViewById(R.id.btn_feedback_send);

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dialog.dismiss(); }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String userText = etFeedback.getText() != null ? etFeedback.getText().toString().trim() : "";

                // Build device info and full body
                String deviceInfo = buildDeviceInfo();

                String subject;
                try {
                    subject = getString(R.string.feedback_email_subject);
                } catch (Exception e) {
                    subject = "App Feedback";
                }

                String to;
                try {
                    to = getString(R.string.feedback_email_to);
                } catch (Exception e) {
                    to = "support@yourdomain.com"; // fallback - replace with your real email
                }

                String body = new StringBuilder()
                        .append("User feedback:\n")
                        .append(userText.isEmpty() ? "(empty)" : userText)
                        .append("\n\n")
                        .append("---- Device & App info ----\n")
                        .append(deviceInfo)
                        .toString();

                // open email app with mailto URI (ensures subject+body appear)
                openEmailClient(to, subject, body);
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
