package com.example.screenrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MyForegroundService extends Service {

    private static final String TAG = "MyForegroundService";
    private static final String CHANNEL_ID = "ScreenRecordChannel";
    private static final int NOTIF_ID = 1;

    private MediaProjection mediaProjection;
    private MediaMuxerWrapper muxerWrapper;
    private ScreenRecorder screenRecorder;
    private AudioRecorderThread audioRecorderThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIF_ID, getNotification());

        int resultCode = intent.getIntExtra("code", -1);
        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpm.getMediaProjection(resultCode, data);

        try {
            muxerWrapper = new MediaMuxerWrapper();

            screenRecorder = new ScreenRecorder(mediaProjection, muxerWrapper, (WindowManager) getSystemService(WINDOW_SERVICE));
            audioRecorderThread = new AudioRecorderThread(mediaProjection, muxerWrapper);

            screenRecorder.start();
            audioRecorderThread.start();

        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar gravação", e);
            stopSelf();
        }

        return START_NOT_STICKY;
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
            if (screenRecorder != null) {
                screenRecorder.stop();
            }

            if (audioRecorderThread != null) {
                audioRecorderThread.stopRecording();
            }

            if (muxerWrapper != null) {
                muxerWrapper.stopMuxer();
            }

            if (mediaProjection != null) {
                mediaProjection.stop();
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao parar gravação", e);
        }
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
