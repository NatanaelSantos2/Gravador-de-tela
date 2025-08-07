package com.example.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingControl {

    private final Context context;
    private final WindowManager windowManager;
    private View floatView;
    private TextView timeText;
    private final Handler timerHandler = new Handler();
    private long startTime;

    public FloatingControl(Context context) {
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void showFloatingButton() {
        if (floatView != null) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        floatView = inflater.inflate(R.layout.floating_button, null);
        timeText = floatView.findViewById(R.id.timeText);

        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 50;
        params.y = 200;

        windowManager.addView(floatView, params);

        floatView.setOnClickListener(v -> {
            context.stopService(new Intent(context, MyForegroundService.class));
            removeFloatingButton();
        });

        startTime = System.currentTimeMillis();
        timerHandler.post(updateTimerRunnable);
    }

    public void removeFloatingButton() {
        if (floatView != null) {
            windowManager.removeView(floatView);
            floatView = null;
            timerHandler.removeCallbacks(updateTimerRunnable);
        }
    }

    private final Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            String time = String.format("%02d:%02d", minutes, seconds);
            timeText.setText(time);

            timerHandler.postDelayed(this, 1000);
        }
    };
}
