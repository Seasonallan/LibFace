package com.library.aimo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.library.aimo.api.IMoRecognitionManager;
import com.library.aimo.api.IMoSDKManager;
import com.library.aimo.api.StaticOpenApi;
import com.library.aimo.config.SettingConfig;
import com.library.aimo.core.FastPermissions;
import com.library.aimo.widget.CameraView;

import java.io.File;
import java.io.FileOutputStream;


/**
 * 简单拍摄证件
 */
public class SimpleCameraActivity extends AppCompatActivity {

    public static void open(AppCompatActivity appCompatActivity, boolean isPositive, boolean autoRecognize, ICameraTakeListener listener) {
        SimpleCameraActivity.listener = listener;
        Intent intent = new Intent(appCompatActivity, SimpleCameraActivity.class);
        intent.putExtra("isPositive", isPositive);
        intent.putExtra("autoRecognize", autoRecognize);
        appCompatActivity.startActivity(intent);
    }


    public static ICameraTakeListener listener;
    public static int headRectExtend = 48;
    public static String buttonDesc = "使用";
    public static String topDesc = "请将证件放入框内，并对其四周边框";

    public static interface ICameraTakeListener {
        void onCameraPictured(boolean isPositive, String fileName, String fileNameHead);
    }

    CameraView cameraView;

    LinearLayout.LayoutParams params;

    boolean autoRecognize = false;
    boolean isPositive = false;

    View cameraLayout;
    LinearLayout cameraContainer;
    LinearLayout cameraIvContainer;
    RelativeLayout cameraRlResult;


    TextView camera_tips;
    View camera_fl_cover;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTransparent();

        setContentView(R.layout.activity_auth_camera);

        camera_fl_cover = findViewById(R.id.camera_fl_cover);
        cameraLayout = findViewById(R.id.camera);
        cameraContainer = findViewById(R.id.camera_container);
        cameraIvContainer = findViewById(R.id.camera_iv_container);
        cameraRlResult = findViewById(R.id.camera_rl_result);
        camera_tips = findViewById(R.id.camera_tips);


        isPositive = getIntent().getBooleanExtra("isPositive", false);
        autoRecognize = getIntent().getBooleanExtra("autoRecognize", false);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        cameraLayout.setPadding(0, 0, 0, getNavigationBarHeight());
        cameraRlResult.setPadding(0, 0, 0, getNavigationBarHeight());

        width = getResources().getDisplayMetrics().widthPixels;
        height = getResources().getDisplayMetrics().heightPixels;
        height += getStatusBarHeight(this);
        height += getNavigationBarHeight();


        rectMargin = (int) (39 * getResources().getDisplayMetrics().density);
        RelativeLayout.LayoutParams rectParams = (RelativeLayout.LayoutParams) camera_fl_cover.getLayoutParams();
        rectParams.leftMargin = rectMargin;
        rectParams.rightMargin = rectMargin;
        rectParams.topMargin = rectMargin + getStatusBarHeight(this);
        rectParams.bottomMargin = rectMargin;
        camera_fl_cover.requestLayout();


        new FastPermissions(this).need(Manifest.permission.CAMERA)
                .subscribe(new FastPermissions.Subscribe() {
                    @Override
                    public void onResult(int requestCode, boolean allGranted, String[] permissions) {
                        if (allGranted) {
                            //权限允许后进行的操作
                            initView();
                        } else {
                            Toast.makeText(SimpleCameraActivity.this, "permission request fail!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                }).request(1100);
    }


    /**
     * 是否使用沉浸式
     *
     * @return
     */
    protected boolean transparentBar() {
        //Android 8.0.0,level 26 HuaWei 问题 java.lang.IllegalStateException: Only fullscreen activities can request orientation
        if ("huawei".equalsIgnoreCase(Build.MANUFACTURER) && Build.VERSION.SDK_INT == 26) {
            return false;
        }
        return true;
    }

    /**
     * 获取底部菜单的高度
     *
     * @return
     */
    protected int getNavigationBarHeight() {
        int navigationBarHeight = 0;
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            navigationBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        return navigationBarHeight;
    }

    /**
     * 获取状态栏高度
     *
     * @param context
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }

    /**
     * 设置状态栏全透明
     */
    public void setTransparent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(0xff000000);

            if (transparentBar()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            }
        }
        if (transparentBar()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {//刘海
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
    }

    int rectMargin;
    int width, height;

    protected void initView() {

        Camera camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = CameraView.getPreviewSize(parameters.getSupportedPictureSizes(), parameters.getSupportedPreviewSizes(), height * 1.0f / width);
        cameraView = new CameraView(this);
        cameraView.setCameraSize(size);
        cameraView.bindCamera(camera);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        if (size != null) {
            if (size.height * 1.0f / size.width > width * 1.0f / height) {//以高为基准
                //改变camera大小
                int recordWidth = width;
                params.width = size.height * height / size.width;
                params.height = height;
                params.leftMargin = -(params.width - recordWidth) / 2;
            } else {
                //改变camera大小
                int recordHeight = height;
                params.width = width;
                params.height = size.width * width / size.height;
                params.topMargin = -(params.height - recordHeight) / 2;
            }
        }
        cameraContainer.addView(cameraView, params);
        cameraIvResult = new ImageView(this);
        cameraIvResult.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cameraIvContainer.addView(cameraIvResult, params);


        cameraIvContainer.setVisibility(View.GONE);
        cameraRlResult.setVisibility(View.GONE);

        camera_tips.setText(topDesc);
        ((TextView) findViewById(R.id.camera_iv_use)).setText(buttonDesc);

        camera_tips.post(new Runnable() {
            @Override
            public void run() {
                camera_tips.setPadding(0, 0, 0, camera_tips.getWidth() / 2 + camera_tips.getHeight() * 4);
            }
        });

        findViewById(R.id.camera_iv_take_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCameraIvTakePictureClicked();
            }
        });
        findViewById(R.id.camera_iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.camera_iv_retake).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraRlResult.setVisibility(View.GONE);
                cameraIvContainer.setVisibility(View.GONE);
                cameraView.restart();
            }
        });
        findViewById(R.id.camera_iv_use).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Matrix m = new Matrix();
                    m.setRotate(-90, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
                    Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

                    Camera.Size size = cameraView.getCameraSize();
                    float percent = 1.0f;

                    if (size != null) {
                        if (size.height * 1.0f / size.width > width * 1.0f / height) {//以高为基准
                            percent = height * 1.0f / size.height;
                        } else {
                            percent = width * 1.0f / size.height;
                        }
                    }

                    int left = (int) ((-params.topMargin + rectMargin + getStatusBarHeight(SimpleCameraActivity.this)) / percent);
                    int top = (int) (-params.leftMargin + rectMargin / percent);
                    int width = (int) (camera_fl_cover.getHeight() / percent);
                    int height = (int) (camera_fl_cover.getWidth() / percent);
                    left = Math.max(0, left);
                    top = Math.max(0, top);
                    width = Math.min(result.getWidth() - left, width);
                    height = Math.min(result.getHeight() - top, height);


                    final Bitmap clipBitmap = Bitmap.createBitmap(result, left, top, width, height);
                    filePath = saveBitmapCache(getCacheDir(), clipBitmap, null);
                    if (autoRecognize) {
                        EasyLibUtils.init(getApplication());
                        IMoSDKManager.get().initImoSDK(new IMoSDKManager.FaceSDKInitListener() {
                            @Override
                            public void onInitResult(boolean success, int errorCode) {
                                if (success) {
                                    IMoRecognitionManager.getInstance().init(SettingConfig.getAlgorithmNumThread(), new IMoRecognitionManager.InitListener() {
                                        @Override
                                        public void onSucceed() {
                                            RectF rectF = StaticOpenApi.getBitmapRect(clipBitmap);
                                            if (rectF != null) {
                                                int left = (int) rectF.left;
                                                int top = (int) rectF.top;
                                                int width = (int) rectF.width();
                                                int height = (int) rectF.height();
                                                left -= headRectExtend * getResources().getDisplayMetrics().density;
                                                left = Math.max(0, left);
                                                top -= headRectExtend * getResources().getDisplayMetrics().density;
                                                top = Math.max(0, top);

                                                width += headRectExtend * getResources().getDisplayMetrics().density * 2;
                                                height += headRectExtend * getResources().getDisplayMetrics().density * 2;
                                                width = Math.min(clipBitmap.getWidth() - left, width);
                                                height = Math.min(clipBitmap.getHeight() - top, height);

                                                Bitmap headBitmap = Bitmap.createBitmap(clipBitmap, left, top, width, height);
                                                filePathHead = saveBitmapCache(getCacheDir(), headBitmap, null);
                                            }
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    setResult();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(int code, String msg) {
                                            setResult();
                                        }
                                    });
                                } else {
                                    setResult();
                                }
                            }
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    String filePath = null;
    String filePathHead = null;

    private void setResult() {
        if (SimpleCameraActivity.listener != null) {
            SimpleCameraActivity.listener.onCameraPictured(isPositive, filePath, filePathHead);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoRecognize) {
            IMoSDKManager.get().destroy();
            IMoRecognitionManager.getInstance().release();
        }
        listener = null;
    }

    private ImageView cameraIvResult;

    public void onCameraIvTakePictureClicked() {
        cameraView.takePicture(new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                cameraRlResult.setVisibility(View.VISIBLE);
                cameraIvContainer.setVisibility(View.VISIBLE);
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                Matrix m = new Matrix();
                m.setRotate(90, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                cameraIvResult.setImageBitmap(bitmap);

            }
        });
    }

    private Bitmap bitmap;

    /**
     * 保存图片到缓存文件
     */
    public static String saveBitmapCache(File appDir, Bitmap bitmap, String name) {
        if (bitmap == null) {
            return null;
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        if (!TextUtils.isEmpty(name)) {
            fileName = name + ".jpg";
        }
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file.toString();
    }
}
