package com.library.aimo.video.record;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;

import com.library.aimo.EasyLibUtils;
import com.library.aimo.util.ImoLog;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 图片合成视频
 */
public class VideoBuilder extends Thread {

    private final int mFrameRate;
    private final File out;
    private final int bitRate;

    private MediaCodec mediaCodec;
    public boolean isRunning;
    private MediaMuxer mediaMuxer;
    private int mTrackIndex;
    private boolean mMuxerStarted;
    private int colorFormat;

    public static File[] sort(File[] s) {
        //中间值
        File temp = null;
        //外循环:我认为最小的数,从0~长度-1
        for (int j = 0; j < s.length - 1; j++) {
            //最小值:假设第一个数就是最小的
            String min = s[j].getName();
            //记录最小数的下标的
            int minIndex = j;
            //内循环:拿我认为的最小的数和后面的数一个个进行比较
            for (int k = j + 1; k < s.length; k++) {
                //找到最小值
                if (Long.parseLong(min.substring(0, min.indexOf("."))) > Long.parseLong(s[k].getName().substring(0, s[k].getName().indexOf(".")))) {
                    //修改最小
                    min = s[k].getName();
                    minIndex = k;
                }
            }
            //当退出内层循环就找到这次的最小值
            //交换位置
            temp = s[j];
            s[j] = s[minIndex];
            s[minIndex] = temp;
        }
        return s;
    }

    public static void clearCaches(File cacheDir) {
        if (cacheDir != null) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File itemFile : files) {
                    itemFile.delete();
                }
            }
        }
    }

    private File cacheDirs;

    public static File getOutputVideo() {
        return new File(EasyLibUtils.getApp().getCacheDir(), "video_encoded.mp4");
        // return new File(Environment.getExternalStorageDirectory(), "auf/video_encoded.mp4");
    }

    public VideoBuilder(File file, int time) {
        this.cacheDirs = file;
        File[] files = file.listFiles();
        files = sort(files);
        inputs = new CopyOnWriteArrayList<>();
        for (File itemFile : files) {
            inputs.add(itemFile.toString());
        }
        totalSize = inputs.size();

        mFrameRate = totalSize / time;

        out = getOutputVideo();

        out.deleteOnExit();

        out.getParentFile().mkdirs();
        try {
            out.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.bitRate = 0;

        this.isRunning = false;
        this.mTrackIndex = 0;
        this.mMuxerStarted = false;

        init(480, 640);

        mediaCodec.start();
    }


    private void init(int width, int height) {

        int bitRate0 = bitRate;
        if (bitRate == 0) {
            bitRate0 = width * height * 16;//Bitmap.Config.RGB_565
        }

        int[] formats = RGB2YuvTool.getMediaCodecList();

        lab:
        for (int format : formats) {
            switch (format) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: // yuv420sp
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar: // yuv420p
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar: // yuv420psp
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar: // yuv420pp
                    colorFormat = format;
                    break lab;
            }
        }

        if (colorFormat <= 0) {
            colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        }


        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);//COLOR_FormatYUV420SemiPlanar
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate0);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

//
//        对于planar的YUV格式，先连续存储所有像素点的Y，紧接着存储所有像素点的U，随后是所有像素点的V。
//        对于packed的YUV格式，每个像素点的Y,U,V是连续交*存储的。
//
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            //创建生成MP4初始化对象
            mediaMuxer = new MediaMuxer(out.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        isRunning = true;
    }


    public void finish() {
        isRunning = false;
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
        if (mediaMuxer != null) {
            try {
                if (mMuxerStarted) {
                    mediaMuxer.stop();
                    mediaMuxer.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private CopyOnWriteArrayList<String> inputs;


    int totalSize = 0;


    private int getSize(int size) {
        return size / 4 * 4;
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFrameRate;
    }


    private void drainEncoder(boolean endOfStream, MediaCodec.BufferInfo bufferInfo) {
        final int TIMEOUT_USEC = 10000;

        ByteBuffer[] buffers = null;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            buffers = mediaCodec.getOutputBuffers();
        }

        if (endOfStream) {
            try {
                mediaCodec.signalEndOfInputStream();
            } catch (Exception e) {
            }
        }

        while (true) {
            int encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break; // out of while
                } else {
                    ImoLog.i("no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }

                MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                mTrackIndex = mediaMuxer.addTrack(mediaFormat);
                mediaMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                ImoLog.i("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
            } else {
                ByteBuffer outputBuffer = null;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = buffers[encoderStatus];
                } else {
                    outputBuffer = mediaCodec.getOutputBuffer(encoderStatus);
                }

                if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer "
                            + encoderStatus + " was null");
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    ImoLog.i("ignoring BUFFER_FLAG_CODEC_CONFIG");
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    try {
                        mediaMuxer.writeSampleData(mTrackIndex, outputBuffer, bufferInfo);
                    } catch (Exception e) {
                        ImoLog.i("Too many frames");
                    }

                }

                mediaCodec.releaseOutputBuffer(encoderStatus, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        ImoLog.i("reached end of stream unexpectedly");
                    } else {
                        ImoLog.i("end of stream reached");
                    }
                    break; // out of while
                }
            }
        }
    }

    @Override
    public void run() {
        final int TIMEOUT_USEC = 10000;
        isRunning = true;
        long generateIndex = 0;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        ByteBuffer[] buffers = null;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            buffers = mediaCodec.getInputBuffers();
        }

        while (isRunning) {

            int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                long ptsUsec = computePresentationTime(generateIndex);
                if (inputs.size() == 0) {
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, ptsUsec,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    isRunning = false;
                    drainEncoder(true, info);

                } else {
                    String filePath = inputs.remove(0);
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    ImoLog.e("当前进度：" + generateIndex + "/" + totalSize);
                    if (bitmap != null) {
                        byte[] input = RGB2YuvTool.getNV12(getSize(bitmap.getWidth()), getSize(bitmap.getHeight()), bitmap, colorFormat);//AvcEncoder.this.getNV21(bitmap);

                        //有效的空的缓存区
                        ByteBuffer inputBuffer = null;
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                            inputBuffer = buffers[inputBufferIndex];
                        } else {
                            inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);//inputBuffers[inputBufferIndex];
                        }
                        inputBuffer.clear();
                        inputBuffer.put(input);
                        //将数据放到编码队列
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, ptsUsec, 0);
                        drainEncoder(false, info);
                    }
                }

                generateIndex++;
            } else {
                ImoLog.i("input buffer not available");
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        finish();
        clearCaches(cacheDirs);

    }
}
