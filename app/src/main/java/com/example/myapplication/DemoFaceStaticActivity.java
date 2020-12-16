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

import com.key.Key;
import com.library.aimo.video.record.VideoEncoder;

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
                                    verifyTvStatus.setText(R.string.face_not_rect);
                                    Toast.makeText(DemoFaceStaticActivity.this, getResources().getString(R.string.face_not_recognized), Toast.LENGTH_SHORT).show();
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
                        return null;
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
                            VideoEncoder.clearCaches(recognizePanel.getCacheDir());
                            AlertDialog alertDialog1 = new AlertDialog.Builder(DemoFaceStaticActivity.this)
                                    .setTitle(R.string.verify_error_title)//标题
                                    .setMessage(R.string.verify_error_recognized_fail)//内容
                                    .setIcon(R.mipmap.ic_launcher)//图标
                                    .setPositiveButton(R.string.verify_retry, new DialogInterface.OnClickListener() {//添加"Yes"按钮
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            start();
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
                    }
                };

                verifyLlFace.addView(recognizePanel.onCreate(), 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                recognizePanel.disableClip();
                recognizePanel.onResume();
                VideoEncoder.clearCaches(recognizePanel.getCacheDir());
            }

            @Override
            public void onFail(int code) {
                Toast.makeText(DemoFaceStaticActivity.this, getResources().getString(code == -1 ? R.string.aimo_not_support : R.string.face_sdk_init_fail), Toast.LENGTH_SHORT).show();
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