package com.example.celebrareproject;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class FloatingPaintService extends Service {

    private static final String CHANNEL_ID = "FloatingPaintChannel_v2";
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    // UI
    private DrawingView drawingView;
    private ImageView btnClose, btnUndo, btnPencil, btnColor;
    private LinearLayout colorPanel;
    private View[] colorSwatches = new View[8];
    private SeekBar seekStroke;
    private TextView tvStrokeLabel;

    // state
    private boolean pencilEnabled = true;
    private int selectedColorIndex = -1;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        startForeground(2001, buildNotification("Paint overlay active"));

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_paint, null);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        // add to window
        windowManager.addView(floatingView, params);

        // bind views (IDs must match your layout)
        drawingView = floatingView.findViewById(R.id.drawingView);
        btnClose = floatingView.findViewById(R.id.btnClosePaint);
        btnUndo = floatingView.findViewById(R.id.btnUndo);
        btnPencil = floatingView.findViewById(R.id.btnPencil);
        btnColor = floatingView.findViewById(R.id.btnColor);

        colorPanel = floatingView.findViewById(R.id.colorPanel);
        colorSwatches[0] = floatingView.findViewById(R.id.color_1);
        colorSwatches[1] = floatingView.findViewById(R.id.color_2);
        colorSwatches[2] = floatingView.findViewById(R.id.color_3);
        colorSwatches[3] = floatingView.findViewById(R.id.color_4);
        colorSwatches[4] = floatingView.findViewById(R.id.color_5);
        colorSwatches[5] = floatingView.findViewById(R.id.color_6);
        colorSwatches[6] = floatingView.findViewById(R.id.color_7);
        colorSwatches[7] = floatingView.findViewById(R.id.color_8);

        seekStroke = floatingView.findViewById(R.id.seekStroke);
        tvStrokeLabel = floatingView.findViewById(R.id.tvStrokeLabel);

        // initial states
        colorPanel.setVisibility(View.GONE);
        seekStroke.setProgress(8);
        tvStrokeLabel.setText("Size: 8");
        drawingView.setStrokeWidth(8f);

        // ensure DrawingView has setDrawingEnabled method (it should)
        drawingView.setDrawingEnabled(pencilEnabled);

        // toolbar listeners
        btnClose.setOnClickListener(v -> stopSelf());

        btnUndo.setOnClickListener(v -> drawingView.undo());

        btnPencil.setOnClickListener(v -> {
            pencilEnabled = !pencilEnabled;
            drawingView.setDrawingEnabled(pencilEnabled);
            if (pencilEnabled) {
                btnPencil.clearColorFilter();
                Toast.makeText(this, "Pencil enabled", Toast.LENGTH_SHORT).show();
            } else {
                btnPencil.setColorFilter(0xFF888888);
                Toast.makeText(this, "Pencil disabled", Toast.LENGTH_SHORT).show();
            }
        });

        btnColor.setOnClickListener(v -> {
            if (colorPanel.getVisibility() == View.VISIBLE) hideColorPanel(); else showColorPanel();
        });

        // color swatches logic (colors match layout)
        final int[] colors = new int[] {
                0xFFFF3B30, // red
                0xFFFF9500, // orange
                0xFFFFCC00, // yellow
                0xFF34C759, // green
                0xFF4CD964, // cyan/light green
                0xFF007AFF, // blue
                0xFF5856D6, // violet
                0xFF000000  // black
        };

        // initialize swatches look and click listeners
        for (int i = 0; i < colorSwatches.length; i++) {
            final int idx = i;
            View sw = colorSwatches[i];
            // set initial plain background based on layout tag or color array
            sw.setBackground(createSwatchDrawable(colors[i], false));
            sw.setOnClickListener(v -> {
                // user picked a color
                drawingView.setEraser(false);
                drawingView.setColor(colors[idx]);
                drawingView.setStrokeWidth(Math.max(2f, seekStroke.getProgress()));
                hideColorPanel();
                setSelectedSwatch(idx);
            });
        }

        // stroke seekbar
        seekStroke.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int p = Math.max(1, progress);
                tvStrokeLabel.setText("Size: " + p);
                drawingView.setStrokeWidth(p);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // allow dragging when touching top area; otherwise touches go to DrawingView
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return false; // allow drawing view to get touches normally
                    case MotionEvent.ACTION_MOVE:
                        // only allow moving if user started near top 200px (toolbar area)
                        if (event.getRawY() < dpToPx(200)) {
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            try { windowManager.updateViewLayout(floatingView, params); } catch (Exception ignored) {}
                            return true;
                        }
                        return false;
                }
                return false;
            }
        });

        // select default swatch (black)
        setSelectedSwatch(7);
    }

    // Create notification
    private Notification buildNotification(String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Paint overlay")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setOngoing(true);
        return builder.build();
    }

    // Notification channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(CHANNEL_ID, "Paint overlay", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(chan);
        }
    }

    // animate show
    private void showColorPanel() {
        colorPanel.setVisibility(View.VISIBLE);
        colorPanel.setTranslationY(dpToPx(40));
        colorPanel.setAlpha(0f);
        colorPanel.animate().translationY(0).alpha(1f).setDuration(220).start();
    }

    // animate hide
    private void hideColorPanel() {
        colorPanel.animate().translationY(dpToPx(40)).alpha(0f).setDuration(180).withEndAction(() -> {
            colorPanel.setVisibility(View.GONE);
            colorPanel.setTranslationY(0);
            colorPanel.setAlpha(1f);
        }).start();
    }

    // highlight the selected swatch by giving it a white stroke
    private void setSelectedSwatch(int idx) {
        selectedColorIndex = idx;
        final int[] colors = new int[] {
                0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFF34C759,
                0xFF4CD964, 0xFF007AFF, 0xFF5856D6, 0xFF000000
        };
        for (int i = 0; i < colorSwatches.length; i++) {
            View sw = colorSwatches[i];
            if (i == idx) {
                sw.setBackground(createSwatchDrawable(colors[i], true));
                sw.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
            } else {
                sw.setBackground(createSwatchDrawable(colors[i], false));
                sw.setPadding(0,0,0,0);
            }
        }
    }

    // helper: create an oval/rounded drawable for swatch; when selected add white stroke
    private GradientDrawable createSwatchDrawable(int color, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dpToPx(8));
        d.setColor(color);
        if (selected) {
            d.setStroke(dpToPx(3), Color.WHITE);
        } else {
            d.setStroke(0, Color.TRANSPARENT);
        }
        return d;
    }

    // utility dp->px
    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { if (floatingView != null) windowManager.removeView(floatingView); } catch (Exception ignored) {}
        stopForeground(true);
    }

    @Override
    public IBinder onBind(android.content.Intent intent) {
        return null;
    }
}
