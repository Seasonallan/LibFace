package com.example.myapplication;


import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.baidu.idl.face.example.R;
import com.key.Key;

public class DemoFaceLoginActivity extends AppCompatActivity {

    TextView verifyTvStatus;
    TextView verifyTvResult;
    LinearLayout verifyLlFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action);

        verifyTvStatus = findViewById(R.id.verify_tv_status);
        verifyTvResult = findViewById(R.id.verify_tv_result);
        verifyLlFace = findViewById(R.id.verify_ll_face);

        setTitle("人脸登录");

        bindImoView();
    }


    private IMoBridge.RecognizePanel recognizePanel;
    private boolean currentStatus = true;


    @Override
    public void onResume() {
        super.onResume();
        if (recognizePanel != null) {
            recognizePanel.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (recognizePanel != null) {
            recognizePanel.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recognizePanel != null) {
            recognizePanel.onDestroy();
        }
        IMoBridge.release();
    }

    private void bindImoView() {
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
                                    verifyTvStatus.setText("请将脸移至框内");
                                    Toast.makeText(DemoFaceLoginActivity.this, "未检测到人脸", Toast.LENGTH_SHORT).show();
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
                        return DemoListActivity.uid;
                    }

                    @Override
                    protected void onFaceRecorded(String id, Bitmap bitmap) {
                    }

                    @Override
                    protected void onFaceRecognized(float score, Bitmap bitmap, String id) {
                        final String cacheBitmap = IMoBridge.saveBitmapCache(getApplication().getCacheDir(), bitmap, "face");
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
                        AlertDialog alertDialog1 = new AlertDialog.Builder(DemoFaceLoginActivity.this)
                                .setTitle("识别失败")//标题
                                .setMessage("无法识别您的脸部或出现异常")//内容
                                .setIcon(R.mipmap.ic_launcher)//图标
                                .setPositiveButton("重新识别", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        recognizePanel.startFaceCheck();
                                    }
                                })
                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
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
                Toast.makeText(DemoFaceLoginActivity.this, code == -1 ? "当前设备不支持人脸识别" : "人脸功能初始化失败", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}