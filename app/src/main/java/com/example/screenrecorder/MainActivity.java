package com.example.screenrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE = 1000;
    private MediaProjectionManager mProjectionManager;
    private Intent mResultData;
    private int mResultCode;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        Button startButton = findViewById(R.id.startBtn);
        Button stopButton = findViewById(R.id.stopBtn);

        startButton.setOnClickListener(v -> {
            if (!isRecording) {
                Intent captureIntent = mProjectionManager.createScreenCaptureIntent();
                startActivityForResult(captureIntent, REQUEST_CODE);
            }
        });

        stopButton.setOnClickListener(v -> {
            if (isRecording) {
                stopService(new Intent(this, MyForegroundService.class));
                isRecording = false;
                Toast.makeText(this, "Gravação parada", Toast.LENGTH_SHORT).show();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
        }


        // REMOVIDO: Permissão de áudio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mResultCode = resultCode;
                mResultData = data;
                startRecordingService();
            } else {
                Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecordingService() {
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        serviceIntent.putExtra("code", mResultCode);
        serviceIntent.putExtra("data", mResultData);
        startForegroundService(serviceIntent);
        isRecording = true;
        Toast.makeText(this, "Gravação iniciada", Toast.LENGTH_SHORT).show();
    }
}
