package com.example.myapplication;


import android.Manifest;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.baidu.idl.face.example.R;
import com.key.Key;
import com.library.aimo.SimpleCameraActivity;
import com.library.aimo.api.IMoSDKManager;
import com.library.aimo.core.FastPermissions;


public class DemoListActivity extends AppCompatActivity {

    public static String uid = "20201216";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        setTitle("功能选择");

        findViewById(R.id.list_btn_action).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DemoListActivity.this, DemoFaceActionActivity.class));
            }
        });
        findViewById(R.id.list_btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IMoBridge.initEasyLibUtils(getApplication());
                if (!IMoBridge.existLocalFace(uid)){
                    Toast.makeText(DemoListActivity.this, "本地没有特征值，请先进行交互式活体采样", Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(new Intent(DemoListActivity.this, DemoFaceLoginActivity.class));
            }
        });
        findViewById(R.id.list_btn_static).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DemoListActivity.this, DemoFaceStaticActivity.class));
            }
        });
        findViewById(R.id.list_btn_ocr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IMoSDKManager.KEY = Key.key;//对应包名的key
                SimpleCameraActivity.headRectExtend = 48; //脸部矩阵需要扩大的范围
                SimpleCameraActivity.buttonDesc = "使用"; //按钮多语言
                SimpleCameraActivity.topDesc = "请将证件放入框内，并对其四周边框";//顶部提示多语言
                SimpleCameraActivity.open(DemoListActivity.this,
                        true, true, new SimpleCameraActivity.ICameraTakeListener() {
                            @Override
                            public void onCameraPictured(boolean isPositive, String fileName, String fileNameHead) {
                                ImageView imageView = findViewById(R.id.result);
                                imageView.setImageBitmap(BitmapFactory.decodeFile(fileName));

                                ImageView imageViewHead = findViewById(R.id.resultHead);
                                imageViewHead.setImageBitmap(BitmapFactory.decodeFile(fileNameHead));
                            }
                        });
            }
        });

        new FastPermissions(this).need(Manifest.permission.READ_PHONE_STATE).request(10086);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}