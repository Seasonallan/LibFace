package com.example.myapplication;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.key.Key;
import com.library.aimo.util.BitmapUtils;
import com.library.aimo.video.record.VideoEncoder;

import java.util.Arrays;

public class DemoFaceLoginActivity extends AppCompatActivity {

    final int REQUEST_STORAGE_PERMISSION = 404;
    final String PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

    TextView verifyTvTitle;
    TextView verifyTvStatus;
    TextView verifyTvResult;
    LinearLayout verifyLlFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action);

        verifyTvTitle = findViewById(R.id.verify_tv_title);
        verifyTvStatus = findViewById(R.id.verify_tv_status);
        verifyTvResult = findViewById(R.id.verify_tv_result);
        verifyLlFace = findViewById(R.id.verify_ll_face);

        verifyTvTitle.setText("人脸登录");
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
                recognizePanel = new IMoBridge.RecognizePanel(DemoFaceLoginActivity.this) {

                    @Override
                    protected int getCoverColor() {
                        return 0xffffffff;
                    }

                    @Override
                    protected void onFaceRectStatus(boolean isRight) {
                    }

                    @Override
                    protected void onFaceNotRecognized() {
                        if (currentStatus) {
                            currentStatus = false;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    verifyTvStatus.setText(R.string.face_not_rect);
                                    Toast.makeText(DemoFaceLoginActivity.this, getResources().getString(R.string.face_not_recognized), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    protected void onActionChanged(int currentAction, int nextAction) {
                    }

                    @Override
                    protected boolean isFaceRecognized() {
                        return true;
                    }

                    @Override
                    protected String getLocalCacheId() {
                        return "10024";
                    }

                    @Override
                    protected void onFaceRecorded(String id, Bitmap bitmap) {
                    }

                    @Override
                    protected void onFaceRecognized(Bitmap bitmap, String id) {
                        final String cacheBitmap = BitmapUtils.saveBitmapCache(getApplication().getCacheDir(), bitmap, "face");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                verifyTvStatus.setText("人脸与本地对比成功");
                                recognizePanel.onPause();

                                StringBuffer stringBuffer = new StringBuffer();
                                stringBuffer.append("人脸对比成功");
                                stringBuffer.append("\n");
                                stringBuffer.append("图片地址：" + cacheBitmap);
                                stringBuffer.append("\n");
                                stringBuffer.append("id：" + id);

                                verifyTvResult.setText(stringBuffer.toString());
                            }
                        });
                    }

                    @Override
                    protected void showRecognitionTimeoutDialog() {
                        VideoEncoder.clearCaches(recognizePanel.getCacheDir());
                        AlertDialog alertDialog1 = new AlertDialog.Builder(DemoFaceLoginActivity.this)
                                .setTitle(R.string.verify_error_title)//标题
                                .setMessage(R.string.verify_error_recognized_fail)//内容
                                .setIcon(R.mipmap.ic_launcher)//图标
                                .setPositiveButton(R.string.verify_retry, new DialogInterface.OnClickListener() {//添加"Yes"按钮
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        recognizePanel.startFaceCheck();
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
                int widthHeight = (int) (240 * getResources().getDisplayMetrics().density);
                verifyLlFace.addView(recognizePanel.onCreate(), new LinearLayout.LayoutParams(widthHeight, widthHeight));
                recognizePanel.startFaceCheck();
            }

            @Override
            public void onFail(int code) {
                Toast.makeText(DemoFaceLoginActivity.this, getResources().getString(code == -1 ? R.string.aimo_not_support : R.string.face_sdk_init_fail), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}