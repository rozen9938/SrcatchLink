package com.roborisen.scratchlink.service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.roborisen.scratchlink.R;
import com.roborisen.scratchlink.ble.BleDevice;
import com.roborisen.scratchlink.ble.BleDeviceListener;
import com.roborisen.scratchlink.ble.BleManager;
import com.roborisen.scratchlink.data.DiscoverScanFilter;
import com.roborisen.scratchlink.data.ParamsData;
import com.roborisen.scratchlink.socketserver.BleWebSocketServer;
import com.roborisen.scratchlink.socketserver.SocketMessageListener;
import com.roborisen.scratchlink.util.ByteUtil;
import com.roborisen.scratchlink.util.GetSSLContext;
import com.roborisen.scratchlink.util.PrefUtil;
import com.roborisen.scratchlink.util.Session;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.net.ssl.SSLContext;


import no.nordicsemi.android.support.v18.scanner.ScanResult;

import static android.util.Base64.DEFAULT;

public class BleWebSocketService extends Service {
    private final String TAG = getClass().getSimpleName();
    private final String mPrefKey ="isGroup";
    private BleWebSocketServer mWebSocketServer;
    private GetSSLContext mGetSSLContext;
    private Session mSession;
    private BleManager mBleManager;
    private WebSocket mWebSocket = null;
    private BluetoothDevice mBluetoothDevice;
    private List<ScanResult> mDeviceList;
    private BluetoothGatt mGatt;
    private int mMessageId = 0;
    private ParamsData mParamsData;
    private boolean isRead;
    private int mSaveDatalength;
    private int mDatalength;
    private Notification mNotification;
    private boolean mReconnect;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new BleWebSocketServiceBinder();
    }

    public class BleWebSocketServiceBinder extends Binder {
        public BleWebSocketService getService() {
            return BleWebSocketService.this;
        }
    }

    private void webSocketOpen(){
        mSession = new Session();
        String ipAddress = "0.0.0.0";
        InetSocketAddress inetSockAddress = new InetSocketAddress(ipAddress,20110);
        mWebSocketServer = new BleWebSocketServer(inetSockAddress);
        setSocketMessageListener(mWebSocketServer);
        SSLContext sslContext = mGetSSLContext.doGetSSLContext();
        if(sslContext != null) {
            //Certificate CA
            mWebSocketServer.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
        }
        //SocketServer Start
        mWebSocketServer.setReuseAddr(true);
        mWebSocketServer.start();
    }

    private void setSocketMessageListener(BleWebSocketServer webSocketServer){
        webSocketServer.setmSocketMessageListener(new SocketMessageListener() {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                Log.e(TAG,"onOpen");
                if(mWebSocket != null){
                    mWebSocket.close();
                    mBleManager.disconnectDevices(mGatt);
                    mReconnect = true;
                }

                mWebSocket = conn;
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                if(!mReconnect){
                    mBleManager.disconnectDevices(mGatt);
                    mBleManager.stopScan();
                    mWebSocket = null;
                }
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                try {
                    Log.e(TAG,"message:"+message);
                    JSONObject jsonObject = new JSONObject(message);
                    JSONObject paramsObject = null;
                    String method = jsonObject.getString("method");

                    mMessageId = jsonObject.getInt("id");
                    if(jsonObject.getString("params") != null){
                        paramsObject = new JSONObject(jsonObject.getString("params"));
                    }

                    if (!conn.getResourceDescriptor().equalsIgnoreCase("/scratch/bt")) {
                        // /scratch/ble or else
                        switch (method) {
                            case "discover" :
                                if(mReconnect){
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            mReconnect = false;
                                            mBleManager.disconnectDevices(mGatt);
                                            DiscoverScanFilter data = mSession.receiveObject(message);
                                            mBleManager.setScanFilter(data.getConnectType(),data.getFiltersType());
                                            mBleManager.startScan();
                                        }
                                    },500);
                                }else {
                                    mReconnect = false;
                                    DiscoverScanFilter data = mSession.receiveObject(message);
                                    mBleManager.setScanFilter(data.getConnectType(),data.getFiltersType());
                                    mBleManager.startScan();
                                }

                                break;
                            case "connect":
                                mBleManager.stopScan();
                                if(mBluetoothDevice != null){
                                    mBleManager.deviceConnect(mBluetoothDevice);
                                }
                                break;
                            case "startNotifications":
                                mBleManager.setCharacteristicNotification(UUID.fromString(paramsObject.getString("serviceId")),mGatt,UUID.fromString(paramsObject.getString("characteristicId")));
                                break;
                            case "write":
                                mParamsData = mSession.setParamsData(mBleManager.makeUUID(paramsObject.getString("serviceId")).toString(),mBleManager.makeUUID(paramsObject.getString("characteristicId")).toString()
                                        ,paramsObject.getString("message"),paramsObject.getString("encoding"));
                                if(isRead){
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            //isRead = false;
                                            if(mParamsData.getEncoding().equalsIgnoreCase("base64")){
                                                byte[] decodeData = Base64.decode(mParamsData.getMessage(),DEFAULT);
                                                //count length;
                                                mSaveDatalength = decodeData.length;
                                                mDatalength = decodeData.length;
                                                mBleManager.writeCharacteristic(mParamsData.getServiceId(),mGatt,mParamsData.getCharacteristicId(),decodeData);
                                            }
                                            isRead = false;
                                        }
                                    },200);
                                }else{
                                    if(mParamsData.getEncoding().equalsIgnoreCase("base64")){
                                        byte[] decodeData = Base64.decode(mParamsData.getMessage(),DEFAULT);//count length;
                                        mSaveDatalength = decodeData.length;
                                        mDatalength = decodeData.length;
                                        String dataString[] = new ByteUtil().byteArrayToHex(decodeData).split(" ");
                                        if(dataString.length > 6 && dataString[6].equalsIgnoreCase("ad")){
                                            if(new PrefUtil(BleWebSocketService.this).getValue(mPrefKey,false)){
                                                decodeData[9] = 0x1A;
                                            }
                                        }

                                        mBleManager.writeCharacteristic(mParamsData.getServiceId(),mGatt,mParamsData.getCharacteristicId(),decodeData);
                                    }
                                }
                                break;
                            case "read":
                                mParamsData = mSession.setParamsData(mBleManager.makeUUID(paramsObject.getString("serviceId")).toString()
                                        ,mBleManager.makeUUID(paramsObject.getString("characteristicId")).toString(),paramsObject.getBoolean("startNotifications"));
                                if(mParamsData.isStartNotifications()){
                                    isRead = true;
                                    mBleManager.setCharacteristicNotification(mParamsData.getServiceId(),mGatt,mParamsData.getCharacteristicId());
                                }
                                mBleManager.readCharacteristic(mParamsData.getServiceId(),mGatt,mParamsData.getCharacteristicId());
                                break;
                        }
                    }else{
                        ///scratch/bt
                    }
                }catch(Exception ex) {
                    Log.e(TAG, "Exception:" + ex.toString());
                }
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                Log.e(TAG,"onError:"+ex.toString());
            }

            @Override
            public void onStart() {
                Log.e(TAG,"onStart");
            }
        });
    }

    private void setBleListener(){
        mBleManager.setmBleDeviceListener(null);
        mBleManager.setmBleDeviceListener(new BleDeviceListener() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.e(TAG,"onConnectionStateChange:"+newState);
                if(newState == BluetoothGatt.STATE_CONNECTED){
                    if(gatt.discoverServices()){
                        Log.e(TAG,"discover true");
                    }else{
                        Log.e(TAG, "discover false");
                        gatt.disconnect();
                        gatt.close();
                        startScan();
                    }
                }
                if(newState == BluetoothGatt.STATE_DISCONNECTED){
                    gatt.disconnect();
                    gatt.close();
                    mGatt = null;
                    mWebSocket.close();
                    if(status == 133){
                        Log.e(TAG,"133 Error");
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Log.e(TAG,"133 start scan");
                                mBleManager.startScan();
                            }
                        },400);

                    }
                }
            }

            @Override
            public void onServiceDiscoverd(BluetoothGatt gatt, int status) {
                Log.e(TAG,"status:"+status);
                if(status == BluetoothGatt.GATT_SUCCESS){
                    mGatt = gatt;
                    mWebSocket.send(mSession.sendResult(0,mMessageId).toString());
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, byte[] datas, int status) {
                Log.e(TAG,"onCharacteristicRead");
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, byte[] datas, int status) {
                if(mDatalength != 0){
                    mDatalength = mDatalength - datas.length;
                }
                if(mDatalength == 0){
                    mWebSocket.send(mSession.sendResult(mSaveDatalength,mMessageId).toString());
                    mSaveDatalength = 0;
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                JSONObject result = mSession.readResult(characteristic.getService().getUuid().toString(),characteristic.getUuid().toString(),characteristic.getValue(),"base64");
                String datas = mSession.sendObject("characteristicDidChange",result).toString();
                Log.e(TAG,"datas:"+datas);
                mWebSocket.send(datas);
            }

            @Override
            public void getScanAllResult(List<ScanResult> results) {
                mDeviceList = results;

                for(int i=0;i<results.size();i++){
                    Log.e(TAG,"result:"+results.get(i).getScanRecord().getDeviceName());
                    //find and connect
                    if(mWebSocket != null){
                        if(mBleManager.getmFilterData().equals(getString(R.string.device_name))){
                            if(isPingPong(results.get(i)))
                                break;
                        }else{
                            //OTHER DEVICE
                            mBluetoothDevice = mSession.sendDeviceInfo(results.get(i),mWebSocket);
                        }
                    }
                }
            }

            @Override
            public void startScan() { }

            @Override
            public void stopScan() { }
        });
    }

    //PINGPONG DEVICE NAME && GROUP ID && AIR ALL OK, SEND DEVICE INFO
    private boolean isPingPong(ScanResult results){
        PrefUtil prefUtil = new PrefUtil(this);
        boolean isGrouping = prefUtil.getValue(mPrefKey,false);
        String groupId = prefUtil.getValue("GroupId","");
        if(isGrouping){
            try {
                if(mBleManager.getmFilterData().contains(getString(R.string.device_name))
                        && results.getScanRecord().getDeviceName().contains(getString(R.string.air)) && groupId.equals(results.getDevice().getName().substring(9,11))){
                    if(groupId.equals(results.getDevice().getName().substring(9,11))) {
                        mBluetoothDevice = mSession.sendDeviceInfo(results,mWebSocket);
                        return true;
                    }
                }
            }catch (Exception ex){Log.e(TAG,"error:"+ex.toString());}
        }else{
            if(mBleManager.getmFilterData().contains(getString(R.string.device_name))
                    && results.getScanRecord().getDeviceName().contains(getString(R.string.air))){
                mBluetoothDevice = mSession.sendDeviceInfo(results,mWebSocket);
            }
        }

       return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            mNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();
            startForeground(1,mNotification);
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGetSSLContext = new GetSSLContext(this);
        webSocketOpen();
        //BleManager Initialize
        mBleManager = new BleManager(this);
        setBleListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG,"onDestroy");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            stopForeground(true);
        }
        try {
            mWebSocketServer.stop();
            mBleManager.disconnectDevices(mGatt);
            Log.e(TAG,"onDestroy");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            stopForeground(true);
        }
        try {
            mWebSocketServer.stop();
            mBleManager.disconnectDevices(mGatt);
            Log.e(TAG,"taskRemoved");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopSelf();
    }
}
