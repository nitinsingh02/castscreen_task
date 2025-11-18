package com.example.celebrareproject;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private Paint drawPaint;
    private float strokeWidth = 8f;
    private int paintColor = Color.RED;

    private Path currentPath;
    private final List<Stroke> strokes = new ArrayList<>();

    // Undo/redo structure (simple undo only)
    private static class Stroke {
        final Path path;
        final Paint paint;
        Stroke(Path p, Paint paint) { this.path = p; this.paint = paint; }
    }

    // Background control
    private boolean backgroundOpaque = false; // false = transparent
    private int backgroundColor = 0x88FFFFFF; // semi-opaque white by default

    // drawing enabled flag (used by pencil toggle)
    private boolean drawingEnabled = true;

    public DrawingView(Context context) { super(context); init(); }
    public DrawingView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setStrokeWidth(strokeWidth);
        drawPaint.setColor(paintColor);
    }

    // -------------------------
    // Public API used by service
    // -------------------------
    public void setColor(int color) {
        paintColor = color;
        // if not eraser mode, update paint color
        if (drawPaint.getXfermode() == null) drawPaint.setColor(paintColor);
    }

    public void setStrokeWidth(float width) {
        strokeWidth = width;
        drawPaint.setStrokeWidth(strokeWidth);
    }

    /**
     * Enable eraser mode. When eraser=true we draw with CLEAR Xfermode (transparent).
     */
    public void setEraser(boolean eraser) {
        if (eraser) {
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            // Keep stroke width as set
            drawPaint.setStrokeWidth(strokeWidth);
        } else {
            drawPaint.setXfermode(null);
            drawPaint.setColor(paintColor);
            drawPaint.setStrokeWidth(strokeWidth);
        }
    }

    public void clear() {
        strokes.clear();
        currentPath = null;
        invalidate();
    }

    public void undo() {
        if (!strokes.isEmpty()) {
            strokes.remove(strokes.size() - 1);
            invalidate();
        }
    }

    /**
     * Toggle whether exported bitmap will include semi-opaque background.
     * Also updates view background for visual feedback.
     */
    public void setBackgroundOpaque(boolean opaque) {
        backgroundOpaque = opaque;
        if (backgroundOpaque) {
            setBackgroundColor(backgroundColor);
        } else {
            setBackgroundColor(Color.TRANSPARENT);
        }
        invalidate();
    }

    public boolean isBackgroundOpaque() {
        return backgroundOpaque;
    }

    /**
     * Export the current drawing as a Bitmap. Returns null if view has no size yet.
     */
    @Nullable
    public Bitmap exportBitmap() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return null;

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        if (backgroundOpaque) {
            Paint bg = new Paint();
            bg.setStyle(Paint.Style.FILL);
            bg.setColor(backgroundColor);
            canvas.drawRect(0, 0, w, h, bg);
        } // else leave transparent

        // draw strokes
        for (Stroke s : strokes) {
            canvas.drawPath(s.path, s.paint);
        }
        // draw current path on top (if any)
        if (currentPath != null) {
            canvas.drawPath(currentPath, drawPaint);
        }
        return bmp;
    }

    /**
     * Enable or disable drawing. When disabled, onTouchEvent returns false so touches pass through.
     */
    public void setDrawingEnabled(boolean enabled) {
        drawingEnabled = enabled;
    }

    // -------------------------
    // Touch & drawing
    // -------------------------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If drawing disabled (pencil off), ignore touches so overlay can be moved / other UI can receive them.
        if (!drawingEnabled) return false;

        final float x = event.getX();
        final float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentPath != null) {
                    // copy paint for this stroke
                    Paint p = new Paint(drawPaint);
                    Path finished = new Path(currentPath);
                    strokes.add(new Stroke(finished, p));
                    currentPath = null;
                    invalidate();
                }
                return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // strokes
        for (Stroke s : strokes) {
            canvas.drawPath(s.path, s.paint);
        }
        // current path
        if (currentPath != null) {
            canvas.drawPath(currentPath, drawPaint);
        }
    }
}
