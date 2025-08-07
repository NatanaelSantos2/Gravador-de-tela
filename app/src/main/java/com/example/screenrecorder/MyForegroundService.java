package com.example.screenrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;

public class MyForegroundService extends Service {

    private static final String CHANNEL_ID = "ScreenRecordChannel";
    private static final int NOTIF_ID = 1;

    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private MediaProjection.Callback projectionCallback;
    private boolean isRecording = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIF_ID, getNotification());

        int resultCode = intent.getIntExtra("code", -1);
        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpm.getMediaProjection(resultCode, data);

        startRecording();

        return START_NOT_STICKY;
    }

    private void startRecording() {
        try {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            int screenDensity = metrics.densityDpi;
            Point size = new Point();
            wm.getDefaultDisplay().getRealSize(size);

            mediaRecorder = new MediaRecorder();
            // REMOVIDO: mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    + "/Gravacao_" + System.currentTimeMillis() + ".mp4";
            mediaRecorder.setOutputFile(path);

            mediaRecorder.setVideoSize(size.x, size.y);
            mediaRecorder.setVideoEncodingBitRate(3500 * 1000);
            mediaRecorder.setVideoFrameRate(60);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            // REMOVIDO: mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            mediaRecorder.prepare();

            projectionCallback = new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    stopRecording();
                }
            };
            mediaProjection.registerCallback(projectionCallback, null);

            virtualDisplay = mediaProjection.createVirtualDisplay("ScreenRec",
                    size.x, size.y, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.getSurface(), null, null);

            mediaRecorder.start();
            isRecording = true;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Gravando tela")
                .setContentText("Toque para voltar ao app")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Gravação de Tela",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void stopRecording() {
        try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }

            if (mediaProjection != null) {
                if (projectionCallback != null) {
                    mediaProjection.unregisterCallback(projectionCallback);
                }
                mediaProjection.stop();
                mediaProjection = null;
            }

            isRecording = false;
            stopForeground(true);
            stopSelf();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
