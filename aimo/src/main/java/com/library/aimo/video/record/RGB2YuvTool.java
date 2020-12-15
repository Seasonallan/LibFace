package com.library.aimo.video.record;

import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

public class RGB2YuvTool {

    public static int[] getMediaCodecList() {
        //获取解码器列表
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo codecInfo = null;
        for (int i = 0; i < numCodecs && codecInfo == null; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (!info.isEncoder()) {
                continue;
            }
            String[] types = info.getSupportedTypes();
            boolean found = false;
            //轮训所要的解码器
            for (int j = 0; j < types.length && !found; j++) {
                if (types[j].equals("video/avc")) {
                    found = true;
                }
            }
            if (!found) {
                continue;
            }
            codecInfo = info;
        }
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
        return capabilities.colorFormats;
    }


    public static byte[] getNV12(int inputWidth, int inputHeight, Bitmap scaled, int colorFormat) {
        // Reference (Variation) : https://gist.github.com/wobbals/5725412

        int[] argb = new int[inputWidth * inputHeight];

        //Log.i(TAG, "scaled : " + scaled);
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];

        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: // yuv420sp
                RGB2YuvTool.encodeYUV420SP(yuv, argb, inputWidth, inputHeight);
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar: // yuv420p
                RGB2YuvTool.encodeYUV420P(yuv, argb, inputWidth, inputHeight);
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar: // yuv420psp
                RGB2YuvTool.encodeYUV420PSP(yuv, argb, inputWidth, inputHeight);
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar: // yuv420pp
                RGB2YuvTool.encodeYUV420PP(yuv, argb, inputWidth, inputHeight);
                break;
        }

        return yuv;
    }


    public static void encodeYUV420PSP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
//        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;
//                R = (argb[index] & 0xff000000) >>> 24;
//                G = (argb[index] & 0xff0000) >> 16;
//                B = (argb[index] & 0xff00) >> 8;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                V = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128; // Previously U
                U = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128; // Previously V

                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : (Math.min(Y, 255)));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[yIndex + 1] = (byte) ((V < 0) ? 0 : (Math.min(V, 255)));
                    yuv420sp[yIndex + 3] = (byte) ((U < 0) ? 0 : (Math.min(U, 255)));
                }
                if (index % 2 == 0) {
                    yIndex++;
                }
                index++;
            }
        }
    }

    public static void encodeYUV420PP(byte[] yuv420sp, int[] argb, int width, int height) {

        int yIndex = 0;
        int vIndex = yuv420sp.length / 2;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;
//                R = (argb[index] & 0xff000000) >>> 24;
//                G = (argb[index] & 0xff0000) >> 16;
//                B = (argb[index] & 0xff00) >> 8;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                V = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128; // Previously U
                U = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128; // Previously V

                int i1 = (Y < 0) ? 0 : ((Y > 255) ? 255 : Y);
                if (j % 2 == 0 && index % 2 == 0) {// 0
                    yuv420sp[yIndex++] = (byte) i1;
                    yuv420sp[yIndex + 1] = (byte) ((V < 0) ? 0 : (Math.min(V, 255)));
                    yuv420sp[vIndex + 1] = (byte) ((U < 0) ? 0 : (Math.min(U, 255)));
                    yIndex++;

                } else if (j % 2 == 0 && index % 2 == 1) { //1
                    yuv420sp[yIndex++] = (byte) i1;
                } else if (j % 2 == 1 && index % 2 == 0) { //2
                    yuv420sp[vIndex++] = (byte) i1;
                    vIndex++;
                } else if (j % 2 == 1 && index % 2 == 1) { //3
                    yuv420sp[vIndex++] = (byte) i1;
                }
                index++;
            }
        }
    }

    public static void encodeYUV420P(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uIndex = frameSize;
        int vIndex = frameSize + width * height / 4;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;
//                R = (argb[index] & 0xff000000) >>> 24;
//                G = (argb[index] & 0xff0000) >> 16;
//                B = (argb[index] & 0xff00) >> 8;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                V = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128; // Previously U
                U = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128; // Previously V


                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : (Math.min(Y, 255)));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[vIndex++] = (byte) ((U < 0) ? 0 : (Math.min(U, 255)));
                    yuv420sp[uIndex++] = (byte) ((V < 0) ? 0 : (Math.min(V, 255)));
                }


                index++;
            }
        }
    }


    public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;
//                R = (argb[index] & 0xff000000) >>> 24;
//                G = (argb[index] & 0xff0000) >> 16;
//                B = (argb[index] & 0xff00) >> 8;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                V = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128; // Previously U
                U = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128; // Previously V

                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : (Math.min(Y, 255)));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : (Math.min(V, 255)));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : (Math.min(U, 255)));
                }

                index++;
            }
        }
    }

}
