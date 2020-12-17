package com.example.myapplication;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.key.Key;

import java.util.Arrays;

public class DemoFaceActionActivity extends AppCompatActivity {

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

        setTitle("动作识别验证（交互式活体）");

        bindImoView();

    }



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


    private IMoBridge.RecognizePanel recognizePanel;
    private boolean currentStatus = true;

    private void bindImoView() {
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
                                        verifyTvStatus.setText("请将脸移至框内");
                                    }
                                });
                            }
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
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    verifyTvStatus.setText("请将脸移至框内");
                                    Toast.makeText(DemoFaceActionActivity.this, "未检测到人脸", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    protected void onActionChanged(int currentAction, int nextAction) {
                        switch (nextAction) {
                            case 1:
                                verifyTvStatus.setText("请向左转头");
                                break;
                            case 2:
                                verifyTvStatus.setText("请向右转头");
                                break;
                            case 4:
                                verifyTvStatus.setText("请点一点头");
                                break;
                            case 32:
                                verifyTvStatus.setText("请张一张嘴巴");
                                break;
                            case 8:
                                verifyTvStatus.setText("请眨一眨眼睛");
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
                        final String cacheBitmap = IMoBridge.saveBitmapCache(getApplication().getCacheDir(), bitmap, "face");
                        final float[] features = IMoBridge.getBitmapFeature(bitmap);
                        IMoBridge.saveLocalFace(DemoListActivity.uid, features); //保存人脸特征值，用于人脸登录
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                verifyTvStatus.setText("success");
                                recognizePanel.onPause();
                                progressDialog = new ProgressDialog(DemoFaceActionActivity.this);
                                progressDialog.setTitle("tip");
                                progressDialog.setMessage("Loading...");
                                progressDialog.setCancelable(true);
                                progressDialog.show();
                                IMoBridge.buildVideo(recognizePanel, new IMoBridge.IImoVideoBuildListener() {
                                    @Override
                                    public void onSuccess(String path) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (progressDialog != null) {
                                                    progressDialog.dismiss();
                                                }
                                                StringBuffer stringBuffer = new StringBuffer();
                                                stringBuffer.append("图片地址：" + cacheBitmap);
                                                stringBuffer.append("\n");
                                                stringBuffer.append("视频地址：" + path);
                                                stringBuffer.append("\n");
                                                stringBuffer.append("视频时长：" + recognizePanel.getTime() + " 秒");
                                                stringBuffer.append("\n");
                                                stringBuffer.append("图片特征值：" + Arrays.toString(features));

                                                verifyTvResult.setText(stringBuffer.toString());
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    protected void onFaceRecognized(float score, Bitmap bitmap, String id) {
                    }

                    @Override
                    protected void showRecognitionTimeoutDialog() {
                        AlertDialog alertDialog1 = new AlertDialog.Builder(DemoFaceActionActivity.this)
                                .setTitle("识别失败")//标题
                                .setMessage("无法识别您的脸部或出现异常")//内容
                                .setIcon(R.mipmap.ic_launcher)//图标
                                .setPositiveButton("重新识别", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        recognizePanel.startRandomAction(false);
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
                recognizePanel.startRandomAction(false);
            }

            @Override
            public void onFail(int code) {
                Toast.makeText(DemoFaceActionActivity.this, code == -1 ? "当前设备不支持人脸识别" : "人脸功能初始化失败", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}