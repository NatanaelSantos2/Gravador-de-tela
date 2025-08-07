package com.example.screenrecorder;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioRecorderThread extends Thread {
    private static final String TAG = "AudioRecorderThread";

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int BIT_RATE = 128000;

    private final MediaProjection mediaProjection;
    private final MediaMuxerWrapper muxerWrapper;

    private AudioRecord audioRecord;
    private MediaCodec audioEncoder;
    private int audioTrackIndex = -1;

    private volatile boolean isRecording = true;

    public AudioRecorderThread(MediaProjection projection, MediaMuxerWrapper muxerWrapper) {
        this.mediaProjection = projection;
        this.muxerWrapper = muxerWrapper;
    }

    @Override
    public void run() {
        try {
            initAudioComponents();

            audioRecord.startRecording();
            audioEncoder.start();

            byte[] buffer = new byte[4096];
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (isRecording) {
                int readBytes = audioRecord.read(buffer, 0, buffer.length);
                if (readBytes > 0) {
                    int inputIndex = audioEncoder.dequeueInputBuffer(10000);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = audioEncoder.getInputBuffer(inputIndex);
                        if (inputBuffer != null) {
                            inputBuffer.clear();
                            inputBuffer.put(buffer, 0, readBytes);
                            long presentationTimeUs = System.nanoTime() / 1000;
                            audioEncoder.queueInputBuffer(inputIndex, 0, readBytes, presentationTimeUs, 0);
                        }
                    }
                }

                int outputIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 10000);
                while (outputIndex >= 0) {
                    ByteBuffer outputBuffer = audioEncoder.getOutputBuffer(outputIndex);
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                        if (audioTrackIndex == -1) {
                            MediaFormat format = audioEncoder.getOutputFormat();
                            audioTrackIndex = muxerWrapper.addAudioTrack(format);
                            muxerWrapper.startMuxerIfReady();
                        }

                        muxerWrapper.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo, true);
                    }

                    audioEncoder.releaseOutputBuffer(outputIndex, false);
                    outputIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 0);
                }
            }

            release();

        } catch (Exception e) {
            Log.e(TAG, "Erro ao gravar áudio interno", e);
        }
    }

    private void initAudioComponents() throws IOException {
        // Configure AudioRecord with AudioPlaybackCapture
        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build();

        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord.Builder()
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build())
                .setAudioPlaybackCaptureConfig(config)
                .setBufferSizeInBytes(minBufferSize)
                .build();

        MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 2);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);

        audioEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        audioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    public void stopRecording() {
        isRecording = false;
    }

    private void release() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        if (audioEncoder != null) {
            audioEncoder.stop();
            audioEncoder.release();
            audioEncoder = null;
        }
    }
}
