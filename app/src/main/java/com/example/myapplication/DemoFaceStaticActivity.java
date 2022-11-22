package com.example.myapplication;


import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.baidu.idl.face.example.R;
import com.key.Key;

public class DemoFaceStaticActivity extends AppCompatActivity {

    TextView verifyTvStatus;
    TextView verifyTvStart;
    FrameLayout verifyLlFace;

    ImageView imageView1, imageView2, imageView3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_static);

        setTitle("静态人脸识别");

        verifyTvStatus = findViewById(R.id.camera_tv_desc);
        verifyLlFace = findViewById(R.id.verify_ll_face);

        imageView1 = findViewById(R.id.auth_iv_example1);
        imageView2 = findViewById(R.id.auth_iv_example2);
        imageView3 = findViewById(R.id.auth_iv_example3);

        verifyTvStart = findViewById(R.id.camera_iv_start);
        verifyTvStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
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
        if (timer != null) {
            timer.cancel();
        }
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
                recognizePanel = new IMoBridge.RecognizePanel(DemoFaceStaticActivity.this) {

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
                                    Toast.makeText(DemoFaceStaticActivity.this, "未检测到人脸", Toast.LENGTH_SHORT).show();
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
                        return null;//notice：这里必须为null
                    }


                    @Override
                    protected void onFaceRecorded(String id, Bitmap bitmap) {
                    }

                    @Override
                    protected void onFaceRecognized(float score, Bitmap bitmap, String id) {
                        if (score < 0.9) {
                            return;
                        }
                        currentStatus = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentTime >= 10000 * 2 / 3) {
                                    imageView1.setImageBitmap(bitmap);
                                } else if (currentTime >= 10000 * 1 / 3) {
                                    if (imageView1.getDrawable() == null) {
                                        imageView1.setImageBitmap(bitmap);
                                    } else {
                                        imageView2.setImageBitmap(bitmap);
                                    }
                                } else {
                                    if (imageView1.getDrawable() == null) {
                                        imageView1.setImageBitmap(bitmap);
                                    } else if (imageView2.getDrawable() == null) {
                                        imageView2.setImageBitmap(bitmap);
                                    } else {
                                        imageView3.setImageBitmap(bitmap);
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    protected void showRecognitionTimeoutDialog() {
                        if (imageView3.getDrawable() == null) {
                            AlertDialog alertDialog1 = new AlertDialog.Builder(DemoFaceStaticActivity.this)
                                    .setTitle("识别失败")//标题
                                    .setMessage("无法识别您的脸部或出现异常")//内容
                                    .setIcon(R.mipmap.ic_launcher)//图标
                                    .setPositiveButton("重新识别", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            start();
                                        }
                                    })
                                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            finish();
                                        }
                                    })

                                    .create();
                            alertDialog1.show();
                        }
                    }
                };

                verifyLlFace.addView(recognizePanel.onCreate(), 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                recognizePanel.disableClip();
                recognizePanel.onResume();
            }

            @Override
            public void onFail(int code) {
                Toast.makeText(DemoFaceStaticActivity.this, code == -1 ? "当前设备不支持人脸识别" : "人脸功能初始化失败", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void start() {
        if ("完成".equals(verifyTvStart.getText().toString())) {
            finish();
            return;
        }
        if (recognizePanel != null)
            recognizePanel.startFaceCheck();
        countDown(10 * 1000);
    }

    CountDownTimer timer;
    long currentTime;

    private void countDown(long lsTime) {
        verifyTvStart.setText("" + lsTime / 1000);
        timer = new CountDownTimer(lsTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentTime = millisUntilFinished;
                verifyTvStart.setText("" + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                if (imageView3.getDrawable() != null) {
                    verifyTvStart.setText("完成");
                } else {
                }
                timer = null;
            }
        }.start();
    }


}