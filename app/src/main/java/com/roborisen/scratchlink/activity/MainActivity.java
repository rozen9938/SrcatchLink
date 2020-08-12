package com.roborisen.scratchlink.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.roborisen.scratchlink.R;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends BaseActivity {
    private int buttonIdx = 0;
    private final String TAG = getClass().getSimpleName();

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= 23) {
            int isPermissiongGranted = 0;
            for(int i=0;i<permissions.length;i++){
                if(grantResults[i]!= PackageManager.PERMISSION_GRANTED && grantResults[i]!=PackageManager.PERMISSION_GRANTED){
                    //resume tasks needing this permission
                    //return;
                    isPermissiongGranted ++;
                }
            }
            if(isPermissiongGranted==0){
                sendActivity(buttonIdx);
            }else{
                checkPermission();
            }
        }
    }

    //Use to SDK version 23 Higher
    private int checkPermission() {
        int isPermissiongGranted = 0;
        if (Build.VERSION.SDK_INT >= 23) {
            String[] permission = new String[]{ Manifest.permission.ACCESS_FINE_LOCATION};
            for (int i = 0; i < permission.length; i++) {
                if (checkSelfPermission(permission[i]) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, permission[i]);
                    isPermissiongGranted++;
                } else {

                }
            }
            if (isPermissiongGranted > 0) {
                requestPermissions(permission, 1002);
            }else{
                sendActivity(buttonIdx);
            }
        }
        return isPermissiongGranted;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.ib_groupwizard,R.id.ib_connect,R.id.ib_guide,R.id.ib_fwupdate})
    void clickIB(View v){
        Log.e(TAG,"sendActivity0");
        switch (v.getId()){
            case R.id.ib_groupwizard:
                buttonIdx=0;
                checkPermission();
                break;
            case R.id.ib_connect:
                buttonIdx = 1;
                checkPermission();

                break;
            case R.id.ib_guide:
                //sendActivity(2);
                break;
            case R.id.ib_fwupdate:
                buttonIdx = 3;
                //checkPermission();
                break;
        }
    }

    private void sendActivity(int num){
        Intent i = null;
        switch (num){
            case 0:
                i = new Intent(MainActivity.this,GroupWizardActivity.class);
                break;
            case 1:
                i = new Intent(MainActivity.this,ConnectActivity.class);
                break;
            case 2:
                i = new Intent(MainActivity.this,GuideActivity.class);
                break;
            case 3:
                //i = new Intent(MainActivity.this,FWUpdateActivity.class);
                return;
        }
        if(i != null){
            Log.e(TAG,"sendActivity");
            startActivity(i);
        }
    }

}
