package com.roborisen.scratchlink.activity;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.roborisen.scratchlink.R;

import com.roborisen.scratchlink.ble.BleDeviceListener;
import com.roborisen.scratchlink.ble.BleManager;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static com.roborisen.scratchlink.ble.BleManager.UART_RX_Characteristic;
import static com.roborisen.scratchlink.ble.BleManager.UART_SERVICE;
import static com.roborisen.scratchlink.ble.BleManager.UART_TX_Characteristic;

public class GroupWizardActivity extends BaseActivity {
    private final String TAG = getClass().getSimpleName();
    private final int STATE_CONNECTED = 131;
    private final int STATE_DISCONNECTED = 132;
    private final int STATE_GATT_133 = 133;
    private final int START_SCAN = 135;
    private final int STOP_SCAN = 136;
    private final int SCAN_TIMEOUT = 137;
    @BindViews({R.id.iv_cube_r,R.id.iv_cube_g,R.id.iv_cube_b,R.id.iv_cube_c,R.id.iv_cube_m,R.id.iv_cube_y,R.id.iv_cube_v,R.id.iv_cube_o}) ImageView [] iv_cubes;
    @BindViews({R.id.rl_cube_r,R.id.rl_cube_g,R.id.rl_cube_b,R.id.rl_cube_c,R.id.rl_cube_m,R.id.rl_cube_y,R.id.rl_cube_v,R.id.rl_cube_o}) RelativeLayout [] rl_cubes;
    @BindViews({R.id.tv_cube_r,R.id.tv_cube_g,R.id.tv_cube_b,R.id.tv_cube_c,R.id.tv_cube_m,R.id.tv_cube_y,R.id.tv_cube_v,R.id.tv_cube_o}) TextView [] tv_cubes;
    @BindViews({R.id.iv_fir_group_id,R.id.iv_sec_group_id}) ImageView [] iv_groups;
    @BindViews({R.id.tv_fir_group_id,R.id.tv_sec_group_id}) TextView [] tv_groups;
    @BindViews({R.id.iv_fir_group_line,R.id.iv_sec_group_line}) ImageView [] iv_group_lines;
    @BindView(R.id.iv_back) ImageView iv_back;
    @BindView(R.id.iv_set) ImageView iv_set;
    @BindView(R.id.iv_clear) ImageView clear;
    @BindView(R.id.tv_title) TextView tv_title;

    private BleManager mBleManager;
    private BluetoothGatt mGatt;
    private BluetoothDevice mAgg;
    private Handler mHandler;

    private int mGroupId[] = new int[]{-1,-1};
    private int mGroupIdx = 0;
    private final int mScanDelay = 25000;
    private boolean mIsGattSuccess = false;
    private boolean mIsSetCubeColors = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groupwizard);
        ButterKnife.bind(this);
        mBleManager = new BleManager(this);
        mBleManager.setScanFilter(2,getString(R.string.device_name));
        setBleListener();
        setmHandler();
    }

    private void setmHandler(){
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case STATE_CONNECTED:
                        tv_title.setText(R.string.disconnect);
                        mIsGattSuccess = true;
                        break;
                    case STATE_DISCONNECTED:
                        tv_title.setText(getString(R.string.search_cube));
                        break;
                    case START_SCAN:
                        tv_title.setText(getString(R.string.searching));
                        break;
                    case STOP_SCAN:
                        tv_title.setText(getString(R.string.search_cube));
                        break;
                }
            }
        };
    }

    private void setBleListener(){
        mBleManager.setmBleDeviceListener(new BleDeviceListener() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.e(TAG,"status:"+status+"/ newState:"+newState);
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    mGatt = gatt;
                    gatt.discoverServices();
                }
                if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    mGatt.disconnect();
                    mGatt = null;
                    mAgg = null;
                    mIsGattSuccess = false;
                    mHandler.sendMessage(makeMessage(STATE_DISCONNECTED,null));
                }

                if(status == STATE_GATT_133) {
                    gatt.disconnect();
                    gatt.close();
                    mAgg = null;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBleManager.startScan();
                }
            }

            @Override
            public void onServiceDiscoverd(BluetoothGatt gatt, int status) {
                if(status== GATT_SUCCESS){
                    mBleManager.setCharacteristicNotification(UART_SERVICE, gatt, UART_TX_Characteristic);
                    mHandler.sendMessage(makeMessage(STATE_CONNECTED,null));
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, byte[] datas, int status) {

            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, byte[] datas, int status) {

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            }

            @Override
            public void getScanAllResult(List<ScanResult> results) {
                Log.e(TAG,"scanResult:"+results.size());
                for(int i=0;i<results.size();i++){
                    if(results.get(i).getDevice().getName()!=null &&
                            results.get(i).getScanRecord().getDeviceName().contains(getString(R.string.air))){
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mBleManager.stopScan();
                            }
                        },200);
                        mAgg = results.get(i).getDevice();
                    }
                }
            }

            @Override
            public void startScan() {
                mHandler.sendMessage(makeMessage(START_SCAN,null));
                mHandler.sendMessageDelayed(makeMessage(SCAN_TIMEOUT,null),mScanDelay);
            }

            @Override
            public void stopScan() {
                mHandler.sendMessage(makeMessage(STOP_SCAN,null));
                mHandler.removeMessages(SCAN_TIMEOUT);
                if(mAgg != null){
                    mBleManager.deviceConnect(mAgg);
                }
            }
        });
    }

    private Message makeMessage(int what,Bundle bundle){
        Message msg = Message.obtain(mHandler,what);
        if(bundle != null){
            msg.setData(bundle);
        }
        return msg;
    }

    private void playSound(int colorIndex){
        if(mGatt != null){
            mBleManager.writeCharacteristic(UART_SERVICE,mGatt,UART_RX_Characteristic,generateBuzzerTone(colorIndex));
        }
    }

    @OnClick(R.id.tv_title)
    void onFindCube(){
        if(mIsGattSuccess){
            allDisconnect();
        }else{
            if(!mBleManager.isScanning){
                mBleManager.startScan();
            }else{
                mBleManager.stopScan();
            }
        }
    }

    @OnClick({R.id.iv_set,R.id.iv_clear})
    void onClickSetGroupId(View v){
        switch (v.getId()){
            case R.id.iv_set:
                if(mGatt != null){
                    if(mGroupId[0] > -1 && mGroupId[1] > -1){
                        mIsSetCubeColors = true;
                        String groupNum = mGroupId[0]+""+mGroupId[1];
                        int groupId = Integer.parseInt(groupNum,16);
                        mBleManager.writeCharacteristic(UART_SERVICE,mGatt,UART_RX_Characteristic,setGroupName(groupId));
                    }
                }
                break;
            case R.id.iv_clear:
                if(mGatt != null){
                    mIsSetCubeColors = false;
                    if(mGroupId[0] > -1){
                        iv_cubes[mGroupId[0]].setVisibility(View.VISIBLE);
                        tv_cubes[mGroupId[0]].setVisibility(View.VISIBLE);
                    }
                    if(mGroupId[1] > -1){
                        iv_cubes[mGroupId[1]].setVisibility(View.VISIBLE);
                        tv_cubes[mGroupId[1]].setVisibility(View.VISIBLE);
                    }
                    for(int i =0;i<iv_groups.length;i++){
                        iv_groups[i].setBackgroundColor(0x00000000);
                        tv_groups[i].setText("");
                        mGroupId[i] = 0;
                    }
                    iv_group_lines [0].setBackgroundResource(R.drawable.btn_cube_select);
                    iv_group_lines [1].setBackgroundResource(R.drawable.btn_cube_unselect);
                    mGroupIdx = 0;
                    String groupNum = 0+""+0;
                    int groupId = Integer.parseInt(groupNum,16);
                    mBleManager.writeCharacteristic(UART_SERVICE,mGatt,UART_RX_Characteristic,setGroupName(groupId));
                }
                break;
        }
    }

    @OnClick({R.id.rl_cube_r,R.id.rl_cube_g,R.id.rl_cube_b,R.id.rl_cube_c,R.id.rl_cube_m,R.id.rl_cube_y,R.id.rl_cube_v,R.id.rl_cube_o})
    void onClickCubes(View v){
        int index =0;
        switch (v.getId()){
            case R.id.rl_cube_r:
                setGroupColor(0);
                index = 0;
                break;
            case R.id.rl_cube_g:
                setGroupColor(1);
                index = 1;
                break;
            case R.id.rl_cube_b:
                setGroupColor(2);
                index = 2;
                break;
            case R.id.rl_cube_c:
                setGroupColor(3);
                index = 3;
                break;
            case R.id.rl_cube_m:
                setGroupColor(4);
                index = 4;
                break;
            case R.id.rl_cube_y:
                setGroupColor(5);
                index = 5;
                break;
            case R.id.rl_cube_v:
                setGroupColor(6);
                index = 6;
                break;
            case R.id.rl_cube_o:
                setGroupColor(7);
                index = 7;
                break;
        }
        playSound(index);
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

    @OnClick({R.id.iv_back})
    void onBackClick(){
        onBackPressed();
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
    }

    private void allDisconnect(){
        mBleManager.stopScan();
        if(mGatt != null){
            tv_title.setText(getString(R.string.search_cube));
            int groupId = (mGroupId[0]*16)+mGroupId[1];
            if(mIsSetCubeColors){
                mBleManager.writeCharacteristic(UART_SERVICE,mGatt,UART_RX_Characteristic,flashDiscoveryGroupId(groupId));
            }else{
                if(mGroupId[0] == 0 && mGroupId[1]==0){
                    mBleManager.writeCharacteristic(UART_SERVICE,mGatt,UART_RX_Characteristic,flashDiscoveryGroupId(groupId));
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mBleManager.writeCharacteristic(UART_SERVICE,mGatt,UART_RX_Characteristic,rebootMultiroleAggregator());

                            mGatt.disconnect();
                            mGatt.close();
                        }
                    },200);
                }else{
                    mBleManager.writeCharacteristic(UART_SERVICE,mGatt,UART_RX_Characteristic,rebootMultiroleAggregator());
                    mGatt.disconnect();
                    mGatt.close();
                }
            }
            mGatt = null;
            mAgg = null;
            mIsGattSuccess = false;
        }
    }

    @Override
    public void onBackPressed() {
        allDisconnect();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private byte[] generateBuzzerTone(int soundCode){
        byte[] data = new byte[13];
        data[0] = (byte) 0xff;
        data[1] = (byte) 0xff;
        data[2] = (byte) 0xff;
        data[3] = (byte) 0xff;
        data[4] = (byte) 0;
        data[5] = (byte) 0;
        data[6] = (byte) 0xEf;
        data[7] = (byte) 0x00;
        data[8] = (byte) 0x0d;
        data[9] = (byte) 0x0c;
        data[10] = (byte)soundCode;
        data[11] = (byte) 0x01;
        data[12] = (byte)10;
        return data;
    }

    public byte[] setGroupName(int groupId){
        byte [] data = new byte [11];
        data [0] = (byte)0xff;
        data [1] = (byte)0xff;
        data [2] = (byte)0xff;
        data [3] = (byte)0xff;
        data [4] = (byte)0x80;
        data [5] = (byte)0xbd;
        data [6] = (byte)0xad;
        data [7] = (byte)0x00;
        data [8] = (byte)0x0b;
        data [9] = (byte)0x0a;
        data [10] = (byte)groupId;

        return data;
    }

    private byte[] flashDiscoveryGroupId(int groupId){
        byte[] data = new byte[11];
        data [0] = (byte)0xff;
        data [1] = (byte)0xff;
        data [2] = (byte)0xff;
        data [3] = (byte)0xaa;
        data [4] = (byte)0x0;
        data [5] = (byte)0x0;
        data [6] = (byte)0xf9;
        data [7] = (byte)0x00;
        data [8] = (byte)0x0b;
        data [9] = (byte)0xfe;
        data [10] = (byte)groupId;

        return data;
    }

    private byte[] rebootMultiroleAggregator(){
        byte [] data = new byte [10];
        data [0] = (byte)0xff;
        data [1] = (byte)0xff;
        data [2] = (byte)0xff;
        data [3] = (byte)0xff;
        data [4] = (byte)0x00;
        data [5] = (byte)0x00;
        data [6] = (byte)0xA8;
        data [7] = (byte)0x00;
        data [8] = (byte)0x0A;
        data [9] = (byte)1;
        return data;
    }

}
