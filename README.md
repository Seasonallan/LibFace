# 第一步
复制libs下的所有aar文件到项目的libs目录
# 第二步
在app项目的build.gradle下的dependencies中添加依赖
    implementation(name: 'core-release', ext: 'aar')
    implementation(name: 'libFaceLiveness-release', ext: 'aar')
    implementation(name: 'libFaceDetector-release', ext: 'aar')
    implementation(name: 'libFaceActionDetector-release', ext: 'aar')
    implementation(name: 'libFaceExtractorV3_1-release', ext: 'aar')
    implementation(name: 'libFaceTrackerDetector-release', ext: 'aar')
# 第三步
在app项目的build.gradle下的android中添加配置
 repositories {
        flatDir {
            dirs 'libs'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    packagingOptions {
        pickFirst 'lib/armeabi-v7a/libopencv_java3.so'
    }

在defaultConfig中添加
        ndk {
            abiFilters "x86", 'armeabi-v7a', 'armeabi'
        }
        
# 第四步
在AndroidManifest.xml中添加摄像头权限
<uses-permission android:name="android.permission.CAMERA" />
# 第五步
使用IMoBridge调用IMO库API
示例1 交互式活体检测：
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
                                verifyTvStatus.setText("请向右转头</");
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
                        final String cacheBitmap = Bitmap2FileUtils.saveBitmapCache(getApplication().getCacheDir(), bitmap, "face");
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
        
示例2 人脸登录：
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
                         final String cacheBitmap = Bitmap2FileUtils.saveBitmapCache(getApplication().getCacheDir(), bitmap, "face");
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
         
示例3 静态活体检测：
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

