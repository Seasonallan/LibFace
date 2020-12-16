package com.example.myapplication;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.key.Key;
import com.library.aimo.api.StaticOpenApi;
import com.library.aimo.util.BitmapUtils;
import com.library.aimo.video.record.VideoEncoder;

import java.util.Arrays;

public class DemoFaceActionActivity extends AppCompatActivity {

    final int REQUEST_STORAGE_PERMISSION = 404;
    final String PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

    TextView verifyTvStatus;
    TextView verifyTvResult;
    LinearLayout verifyRlFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action);

        verifyTvStatus = findViewById(R.id.verify_tv_status);
        verifyTvResult = findViewById(R.id.verify_tv_result);
        verifyRlFace = findViewById(R.id.verify_rl_face);

        if (ContextCompat.checkSelfPermission(this, PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            onPermissionGet();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{PERMISSION},
                    REQUEST_STORAGE_PERMISSION);
        }

    }

    /**
     * 检查权限后的回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION:
                if (permissions.length != 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "打开相机失败，请允许权限后再试", Toast.LENGTH_SHORT).show();
                } else {
                    onPermissionGet();
                }
                break;
        }
    }

    private IMoBridge.RecognizePanel recognizePanel;
    private boolean currentStatus = true;

    private void onPermissionGet() {
        IMoBridge.init(getApplication(), Key.key, new IMoBridge.IImoInitListener() {
            @Override
            public void onSuccess() {
                recognizePanel = new IMoBridge.RecognizePanel(DemoFaceActionActivity.this) {

                    @Override
                    protected int getCoverColor() {
                        return 0xffffffff;
                    }

                    @Override
                    protected void onFaceRectStatus(boolean isRight) {
                        if (!isRight) {
                            if (currentStatus) {
                                currentStatus = false;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        verifyTvStatus.setText(R.string.face_not_rect);
                                    }
                                });
                            }
                            //ToastSimple.show(getResources().getString(R.string.face_not_rect), 1);
                        } else {
                            if (!currentStatus) {
                                currentStatus = true;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        recognizePanel.startRandomAction(true);
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    protected void onFaceNotRecognized() {
                        if (currentStatus) {
                            currentStatus = false;
                            verifyTvStatus.setText(R.string.face_not_rect);
                            Toast.makeText(DemoFaceActionActivity.this, getResources().getString(R.string.face_not_recognized), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    protected void onActionChanged(int currentAction, int nextAction) {
                        switch (nextAction) {
                            case 1:
                                verifyTvStatus.setText(R.string.face_record_tip_head_turn_left);
                                break;
                            case 2:
                                verifyTvStatus.setText(R.string.face_record_tip_head_turn_right);
                                break;
                            case 4:
                                verifyTvStatus.setText(R.string.face_record_tip_nod);
                                break;
                            case 32:
                                verifyTvStatus.setText(R.string.face_record_tip_mouth_open);
                                break;
                            case 8:
                                verifyTvStatus.setText(R.string.face_record_tip_blink);
                                break;
                        }
                    }

                    @Override
                    protected boolean isFaceRecognized() {
                        return false;
                    }

                    @Override
                    protected String getLocalCacheId() {
                        return null;
                    }


                    ProgressDialog progressDialog;

                    @Override
                    protected void onFaceRecorded(String id, Bitmap bitmap) {
                        final String cacheBitmap = BitmapUtils.saveBitmapCache(getApplication().getCacheDir(), bitmap, "face");
                        final float[] features = IMoBridge.getBitmapFeature(bitmap);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                verifyTvStatus.setText(R.string.verify_checking);
                                recognizePanel.onPause();
                                progressDialog = new ProgressDialog(DemoFaceActionActivity.this);
                                progressDialog.setTitle("tip");
                                progressDialog.setMessage("Loading...");
                                progressDialog.setCancelable(true);
                                progressDialog.show();
                                //上传图片
                                new VideoEncoder(recognizePanel.getCacheDir(), recognizePanel.getTime()) {
                                    @Override
                                    public void finish() {
                                        super.finish();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (progressDialog != null) {
                                                    progressDialog.dismiss();
                                                }
                                                StringBuffer stringBuffer = new StringBuffer();
                                                stringBuffer.append("图片地址：" + cacheBitmap);
                                                stringBuffer.append("\n");
                                                stringBuffer.append("视频地址：" + VideoEncoder.getOutputVideo().toString());
                                                stringBuffer.append("\n");
                                                stringBuffer.append("视频时长：" + recognizePanel.getTime() + " 秒");
                                                stringBuffer.append("\n");
                                                stringBuffer.append("图片特征值：" + Arrays.toString(features));

                                                verifyTvResult.setText(stringBuffer.toString());
                                            }
                                        });
                                    }
                                }.start();
                            }
                        });
                    }

                    @Override
                    protected void onFaceRecognized(Bitmap bitmap, String token) {
                    }

                    @Override
                    protected void showRecognitionTimeoutDialog() {
                        VideoEncoder.clearCaches(recognizePanel.getCacheDir());
                        AlertDialog alertDialog1 = new AlertDialog.Builder(DemoFaceActionActivity.this)
                                .setTitle(R.string.verify_error_title)//标题
                                .setMessage(R.string.verify_error_recognized_fail)//内容
                                .setIcon(R.mipmap.ic_launcher)//图标
                                .setPositiveButton(R.string.verify_retry, new DialogInterface.OnClickListener() {//添加"Yes"按钮
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        recognizePanel.startRandomAction(false);
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {//添加取消
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        finish();
                                    }
                                })

                                .create();
                        alertDialog1.show();
                    }
                };
                recognizePanel.onCreate();
                verifyRlFace.addView(recognizePanel.parentView);
                recognizePanel.startRandomAction(false);
                VideoEncoder.clearCaches(recognizePanel.getCacheDir());
            }

            @Override
            public void onFail(int code) {
                Toast.makeText(DemoFaceActionActivity.this, getResources().getString(code == -1 ? R.string.aimo_not_support : R.string.face_sdk_init_fail), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}