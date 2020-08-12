package com.roborisen.scratchlink.activity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.roborisen.scratchlink.R;
import com.roborisen.scratchlink.service.BleWebSocketService;
import com.roborisen.scratchlink.util.PrefUtil;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ConnectActivity extends BaseActivity {
    private final String TAG = getClass().getSimpleName();
    @BindViews({R.id.iv_cube_r,R.id.iv_cube_g,R.id.iv_cube_b,R.id.iv_cube_c,R.id.iv_cube_m,R.id.iv_cube_y,R.id.iv_cube_v,R.id.iv_cube_o}) ImageView[] iv_cubes;
    @BindViews({R.id.rl_cube_r,R.id.rl_cube_g,R.id.rl_cube_b,R.id.rl_cube_c,R.id.rl_cube_m,R.id.rl_cube_y,R.id.rl_cube_v,R.id.rl_cube_o}) RelativeLayout[] rl_cubes;
    @BindViews({R.id.tv_cube_r,R.id.tv_cube_g,R.id.tv_cube_b,R.id.tv_cube_c,R.id.tv_cube_m,R.id.tv_cube_y,R.id.tv_cube_v,R.id.tv_cube_o}) TextView[] tv_cubes;
    @BindViews({R.id.iv_fir_group_id,R.id.iv_sec_group_id}) ImageView [] iv_groups;
    @BindViews({R.id.tv_fir_group_id,R.id.tv_sec_group_id}) TextView [] tv_groups;
    @BindViews({R.id.iv_fir_group_line,R.id.iv_sec_group_line}) ImageView [] iv_group_lines;
    @BindViews({R.id.iv_on,R.id.iv_off}) ImageView [] iv_status;

    private final String mPrefKey ="isGroup";
    private final String mPrefGroupKey = "GroupId";
    private PrefUtil mPrefUtil;
    private BleWebSocketService mBleWebSocketService;
    private String mGroupIdString ="";
    private int [] mGroupId = {-1,-1};
    private int mGroupIdx;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.e(TAG,"BIndService");
            mBleWebSocketService = ((BleWebSocketService.BleWebSocketServiceBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleWebSocketService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        ButterKnife.bind(this);
        //Service On
        if(isServiceRunning()){
            Log.e(TAG,"isServiceRunning");
        }else{
            Intent service = new Intent(this, BleWebSocketService.class);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                Log.e(TAG,"startService1");
                startForegroundService(service);
            }else{
                Log.e(TAG,"startService");
                startService(service);
            }
            bindService(service,mServiceConnection,BIND_AUTO_CREATE);
        }

        mPrefUtil = new PrefUtil(this);
        mGroupIdString = mPrefUtil.getValue(mPrefGroupKey,"");

        if(mPrefUtil.getValue(mPrefKey,false)){
            iv_status[0].setBackgroundResource(R.drawable.btn_on_select);
            mGroupIdx=0;
            setGroupColor(Integer.parseInt(mGroupIdString.substring(0,1)));
            mGroupIdx=1;
            setGroupColor(Integer.parseInt(mGroupIdString.substring(1,2)));
            mGroupIdx=0;
        }else{
            iv_status[1].setBackgroundResource(R.drawable.btn_off_select);
            mGroupIdString = "01";
            mPrefUtil.putString(mPrefGroupKey,"01");
            mGroupIdx=0;
            setGroupColor(0);
            mGroupIdx=1;
            setGroupColor(1);
            mGroupIdx=0;
        }
    }

    public boolean isServiceRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if (BleWebSocketService.class.getName().equals(service.service.getClassName()))
                return true;
        }
        return false;
    }

    @OnClick(R.id.tv_title)
    void onGoScratchClick(){
        String url ="https://roborisen.com/scratch";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);

    }

    @OnClick({R.id.iv_on,R.id.iv_off})
    void onClickSetGroupId(View v){
        switch (v.getId()){
            case R.id.iv_on:
                mPrefUtil.putBoolen(mPrefKey,true);
                String groupId = mGroupId[0]+""+mGroupId[1];
                mPrefUtil.putString(mPrefGroupKey,groupId);
                iv_status[0].setBackgroundResource(R.drawable.btn_on_select);
                iv_status[1].setBackgroundResource(R.drawable.btn_off_unselect);
                mGroupIdString = groupId;
                break;
            case R.id.iv_off:
                mPrefUtil.putBoolen(mPrefKey,false);
                iv_status[0].setBackgroundResource(R.drawable.btn_on_unselect);
                iv_status[1].setBackgroundResource(R.drawable.btn_off_select);
                mGroupIdString = "01";
                break;
        }
    }

    @OnClick({R.id.rl_cube_r,R.id.rl_cube_g,R.id.rl_cube_b,R.id.rl_cube_c,R.id.rl_cube_m,R.id.rl_cube_y,R.id.rl_cube_v,R.id.rl_cube_o})
    void onClickCubes(View v){
        switch (v.getId()){
            case R.id.rl_cube_r:
                setGroupColor(0);
                break;
            case R.id.rl_cube_g:
                setGroupColor(1);
                break;
            case R.id.rl_cube_b:
                setGroupColor(2);
                break;
            case R.id.rl_cube_c:
                setGroupColor(3);
                break;
            case R.id.rl_cube_m:
                setGroupColor(4);
                break;
            case R.id.rl_cube_y:
                setGroupColor(5);
                break;
            case R.id.rl_cube_v:
                setGroupColor(6);
                break;
            case R.id.rl_cube_o:
                setGroupColor(7);
                break;
        }
    }

    @OnClick({R.id.rl_fir_group_id,R.id.rl_sec_group_id})
    void onClickGruops(View v){
        switch (v.getId()){
            case R.id.rl_fir_group_id:
                mGroupIdx = 0;
                iv_group_lines [0].setBackgroundResource(R.drawable.btn_cube_select);
                iv_group_lines [1].setBackgroundResource(R.drawable.btn_cube_unselect);
                break;
            case R.id.rl_sec_group_id:
                mGroupIdx = 1;
                iv_group_lines [1].setBackgroundResource(R.drawable.btn_cube_select);
                iv_group_lines [0].setBackgroundResource(R.drawable.btn_cube_unselect);
                break;
        }
    }

    private void setGroupColor(int colorIdx){
        int colorId [] = new int[]{R.drawable.btn_cube_r,R.drawable.btn_cube_g,
                R.drawable.btn_cube_b,R.drawable.btn_cube_c,R.drawable.btn_cube_m,R.drawable.btn_cube_y,R.drawable.btn_cube_v,R.drawable.btn_cube_o};
        int lastColorIdx = mGroupId[mGroupIdx];

        if(colorIdx == mGroupId[0] || colorIdx == mGroupId[1]){

        }else{
            if(lastColorIdx > -1){
                iv_cubes[lastColorIdx].setVisibility(View.VISIBLE);
                tv_cubes[lastColorIdx].setVisibility(View.VISIBLE);
            }
            iv_groups[mGroupIdx].setBackgroundResource(colorId[colorIdx]);
            tv_groups[mGroupIdx].setText(tv_cubes[colorIdx].getText().toString());
            mGroupId[mGroupIdx] = colorIdx;
            tv_cubes[colorIdx].setVisibility(View.INVISIBLE);
            iv_cubes[colorIdx].setVisibility(View.INVISIBLE);
        }
        if(mPrefUtil.getValue(mPrefKey,false)){
            String groupId = mGroupId[0]+""+mGroupId[1];
            mPrefUtil.putString(mPrefGroupKey,groupId);
        }
    }

    @OnClick(R.id.iv_back)
    void onBackButtonClick(){
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mBleWebSocketService.stopForeground(true);
        unbindService(mServiceConnection);
    }
}
