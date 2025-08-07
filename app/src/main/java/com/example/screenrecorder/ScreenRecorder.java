package com.example.screenrecorder;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenRecorder {
    private static final String TAG = "ScreenRecorder";
    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 60;
    private static final int IFRAME_INTERVAL = 1;
    private static final int BIT_RATE = 9000 * 9000;

    private int width;
    private int height;
    private int dpi;

    private MediaProjection mediaProjection;
    private MediaCodec videoEncoder;
    private Surface inputSurface;
    private VirtualDisplay virtualDisplay;

    private MediaMuxerWrapper muxerWrapper;
    private int videoTrackIndex = -1;
    private boolean isRecording = false;

    public ScreenRecorder(MediaProjection projection, MediaMuxerWrapper muxerWrapper, WindowManager windowManager) {
        this.mediaProjection = projection;
        this.muxerWrapper = muxerWrapper;

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        dpi = metrics.densityDpi;
        width = metrics.widthPixels;
        height = metrics.heightPixels;
    }

    public void start() throws IOException {
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d(TAG, "MediaProjection stopped");
                stop();
            }
        }, null);

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        videoEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        inputSurface = videoEncoder.createInputSurface();
        videoEncoder.start();

        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecorderDisplay",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface, null, null);

        isRecording = true;
        new Thread(this::recordLoop).start();
    }


    private void recordLoop() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (isRecording) {
            int index = videoEncoder.dequeueOutputBuffer(bufferInfo, 10000);
            if (index >= 0) {
                ByteBuffer encodedData = videoEncoder.getOutputBuffer(index);
                if (encodedData != null && bufferInfo.size > 0) {
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);

                    if (videoTrackIndex == -1) {
                        MediaFormat newFormat = videoEncoder.getOutputFormat();
                        videoTrackIndex = muxerWrapper.addVideoTrack(newFormat);
                        muxerWrapper.startMuxerIfReady();
                    }

                    muxerWrapper.writeSampleData(videoTrackIndex, encodedData, bufferInfo, false);
                }
                videoEncoder.releaseOutputBuffer(index, false);
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Handle only once, when format changes
                if (videoTrackIndex == -1) {
                    MediaFormat newFormat = videoEncoder.getOutputFormat();
                    videoTrackIndex = muxerWrapper.addVideoTrack(newFormat);
                    muxerWrapper.startMuxerIfReady();
                }
            }
        }

        release();
    }

    public void stop() {
        isRecording = false;
    }

    private void release() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (videoEncoder != null) {
            videoEncoder.stop();
            videoEncoder.release();
            videoEncoder = null;
        }

        if (inputSurface != null) {
            inputSurface.release();
            inputSurface = null;
        }
    }
}
