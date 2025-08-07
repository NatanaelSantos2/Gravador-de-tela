package com.example.screenrecorder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaMuxerWrapper {
    private static final String TAG = "MediaMuxerWrapper";

    private MediaMuxer mediaMuxer;
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;

    private final AtomicBoolean muxerStarted = new AtomicBoolean(false);
    private final AtomicBoolean videoReady = new AtomicBoolean(false);
    private final AtomicBoolean audioReady = new AtomicBoolean(false);

    private final String outputPath;

    public MediaMuxerWrapper() throws IOException {
        String fileName = "Gravacao_" + System.currentTimeMillis() + ".mp4";
        File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), fileName);
        outputPath = outputFile.getAbsolutePath();

        mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    public int addVideoTrack(MediaFormat format) {
        if (videoTrackIndex == -1) {
            videoTrackIndex = mediaMuxer.addTrack(format);
            videoReady.set(true);
        }
        return videoTrackIndex;
    }

    public int addAudioTrack(MediaFormat format) {
        if (audioTrackIndex == -1) {
            audioTrackIndex = mediaMuxer.addTrack(format);
            audioReady.set(true);
        }
        return audioTrackIndex;
    }

    public synchronized void startMuxerIfReady() {
        if (!muxerStarted.get() && videoReady.get() && audioReady.get()) {
            mediaMuxer.start();
            muxerStarted.set(true);
            Log.d(TAG, "MediaMuxer started.");
        }
    }

    public synchronized void writeSampleData(int trackIndex, ByteBuffer buffer, MediaCodec.BufferInfo info, boolean isAudio) {
        if (muxerStarted.get()) {
            mediaMuxer.writeSampleData(trackIndex, buffer, info);
        }
    }

    public synchronized void stopMuxer() {
        try {
            if (muxerStarted.get()) {
                mediaMuxer.stop();
                mediaMuxer.release();
                muxerStarted.set(false);
                Log.d(TAG, "MediaMuxer stopped. File saved to: " + outputPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao parar MediaMuxer", e);
        }
    }

    public String getOutputPath() {
        return outputPath;
    }
}
