package com.example.myapplication;


import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        new FastPermissions(this).need(Manifest.permission.READ_PHONE_STATE).request(10086);
    }


}