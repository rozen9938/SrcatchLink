package com.roborisen.scratchlink.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.roborisen.scratchlink.R;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Intent i = new Intent(SplashActivity.this,MainActivity.class);
                startActivity(i);
                finish();
            }
        },1000);
    }

    @Override
    public void onBackPressed() {

    }
}
