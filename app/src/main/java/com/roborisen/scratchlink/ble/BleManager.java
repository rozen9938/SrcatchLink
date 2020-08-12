package com.roborisen.scratchlink.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;

import com.roborisen.scratchlink.R;
import com.roborisen.scratchlink.util.ByteUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;


/***
 *
 * Created by rozen on 2018-08-02.
 */

public class BleManager extends BluetoothGattCallback {
    private final String TAG = getClass().getSimpleName();
    //Ble UUID
    private final String BASE_UUID = "xxxxxxxx-0000-1000-8000-00805F9B34FB";
    //Ble UUID
    public static final UUID UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UART_TX_Characteristic = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");//Properties : Notify
    public static final UUID UART_RX_Characteristic = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); //Properties : Write, Write No Response

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BleDeviceListener mBleDeviceListener;
    private int mDataLength;
    private ArrayList<String> mResponseData;
    public boolean isScanning = false;
    private Timer timer;
    private boolean isTimer = false;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;
    private BluetoothGatt mGatt;
    private volatile boolean isWriting;
    private Queue<byte[]> sendQueue; //To be inited with sendQueue = new ConcurrentLinkedQueue<String>();
    private int lowRssi = -90;
    private BluetoothLeScannerCompat mBluetoothScanner;
    private int mConnetType = 0;
    private List<ScanFilter> mFilters;
    private String mFilterData;
    private int dataLength;
    private boolean mIsDisconnect;
    public BleManager(Context context){
        this.mContext = context;
        initialize();
    }

    public boolean isWriting() {
        return isWriting;
    }

    public void setWriting(boolean writing) {
        isWriting = writing;
    }

    public boolean initialize() {
        Log.e(TAG,"initialize");
        // For API level 18 and above, get a reference to BluetoothAdapter through
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        mBluetoothScanner = BluetoothLeScannerCompat.getScanner();
        sendQueue = new LinkedList<>();
        return true;
    }

    /**
     * Set BleDeviceListener for GATTCallback/ScanCallback
     * */
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        mBleDeviceListener.onServiceDiscoverd(gatt,status);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        try{
            if(mBleDeviceListener != null){
                mBleDeviceListener.onConnectionStateChange(gatt,status,newState);
            }
        }catch (Exception ex){
            Log.e(TAG,"onConnectionStateChange:"+ex.toString());
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        mBleDeviceListener.onCharacteristicRead(gatt,characteristic.getValue(),status);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        isWriting = false;
        try {
            if(mBleDeviceListener != null){
                if(characteristic.getValue() != null){
                    mBleDeviceListener.onCharacteristicWrite(gatt,characteristic.getValue(),status);
                    _send(characteristic);
                }
            }

        }catch (Exception ex){
            Log.e(TAG,"onCharacteristicWrite ex:"+ex.toString());
        }
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if(mBleDeviceListener != null) {
            String data = new ByteUtil().byteArrayToHex(characteristic.getValue());
            Log.e(TAG, "data:" + data);
            String[] responseDatas = data.split(" ");
            if (gatt.getDevice().getName().contains(mContext.getString(R.string.device_name))) {
                if (!isTimer) {
                    isTimer = true;
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            isTimer = false;
                            mDataLength = 0;
                            mResponseData.clear();
                        }
                    }, 2000);
                }

                //PingPong Devices Read Data
                if (responseDatas.length > 8 && mDataLength == 0) {
                    mDataLength = Integer.parseInt(responseDatas[7] + responseDatas[8], 16);
                    mResponseData = new ArrayList<>();
                }

                for (int i = 0; i < responseDatas.length; i++) {
                    mResponseData.add(responseDatas[i]);
                }

                if (mResponseData.size() == mDataLength) {
                    timer.cancel();
                    isTimer = false;
                    if (mBleDeviceListener != null) {
                        byte[] datas = new byte[mResponseData.size()];
                        for (int i = 0; i < mResponseData.size(); i++) {
                            datas[i] = (byte) Integer.parseInt(mResponseData.get(i), 16);
                        }
                        characteristic.setValue(datas);
                        mBleDeviceListener.onCharacteristicChanged(gatt, characteristic);
                    }
                    mDataLength = 0;
                    mResponseData.clear();
                }
            } else {
                //make handle
                mBleDeviceListener.onCharacteristicChanged(gatt, characteristic);
            }
        }
    }

    public void setmBleDeviceListener(BleDeviceListener mBleDeviceListener) {
        this.mBleDeviceListener = mBleDeviceListener;
    }

    public boolean isBleEnable(){
        return mBluetoothAdapter.enable();
    }

    public void turnOffBluetooth(){
        if(mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
        }
    }
   public void turnOnBluetooth(){
        if(!mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }
   }

   public void startScan(){
       if(mBluetoothAdapter.isEnabled()){
           if(!isScanning){
               Log.e(TAG,"startScan in BleManager");
               isScanning = true;
               ScanSettings settings = new ScanSettings.Builder()
                       .setLegacy(false)
                       .setUseHardwareBatchingIfSupported(false)
                       .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                       .setReportDelay(500)
                       .build();
               mBluetoothScanner.startScan(mFilters,settings,mScanCallback);
               mBleDeviceListener.startScan();
           }
       }
   }

   public void stopScan(){
       if(mBluetoothAdapter.isEnabled()){
           if(isScanning){
               isScanning = false;
               Log.e(TAG,"Stop Scan in BleManager");
               mBluetoothScanner = BluetoothLeScannerCompat.getScanner();
               mBluetoothScanner.stopScan(mScanCallback);
               mBleDeviceListener.stopScan();
           }
       }
   }

    public void disconnectDevices(final BluetoothGatt gatt){
        isWriting = false;
        if(!mIsDisconnect){
            mIsDisconnect = true;
            if(gatt!=null){
                if(gatt.getDevice().getName().contains(mContext.getString(R.string.device_name))){
                    writeCharacteristic(UART_SERVICE,gatt,UART_RX_Characteristic,rebootMultiroleAggregator());
                }
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mIsDisconnect = false;
                        gatt.disconnect();
                        gatt.close();
                    }
                },200);

            }
        }
    }

    public void setCharacteristicNotification(UUID serviceId, final BluetoothGatt gatt, UUID characteristic){
        BluetoothGattService mBluetoothGattService = gatt.getService(serviceId);
        BluetoothGattCharacteristic mBluetoothGattCharacteristic = mBluetoothGattService.getCharacteristic(characteristic);
        gatt.setCharacteristicNotification(mBluetoothGattCharacteristic,true);
        BluetoothGattDescriptor descriptor = mBluetoothGattCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    public void readCharacteristic(UUID serviceId, BluetoothGatt gatt, UUID characteristic){
        BluetoothGattService mBluetoothGattService = gatt.getService(serviceId);
        BluetoothGattCharacteristic mBluetoothGattCharacteristic = mBluetoothGattService.getCharacteristic(characteristic);
        gatt.readCharacteristic(mBluetoothGattCharacteristic);
    }

    public void writeCharacteristic(UUID serviceId, final BluetoothGatt gatt, UUID characteristic, final byte[]data){
            BluetoothGattService mBluetoothGattService = gatt.getService(serviceId);
            mBluetoothGattCharacteristic = mBluetoothGattService.getCharacteristic(characteristic);
            mGatt = gatt;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    send(data, mBluetoothGattCharacteristic);
                }
            }).start();
    }

    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            ArrayList<ScanResult> list = new ArrayList<>();
            if(mBleDeviceListener != null){
                for(int i=0;i<results.size();i++){
                    if(mConnetType == 2){
                        if(results.get(i).getScanRecord().getDeviceName() != null && results.get(i).getScanRecord().getDeviceName().contains(mFilterData)){
                            if(results.get(i).getRssi() > lowRssi){
                                BleDevice nDevice = new BleDevice();
                                nDevice.setDevice(results.get(i).getDevice());
                                nDevice.setRssi(results.get(i).getRssi());
                                nDevice.setScanRecord(results.get(i).getScanRecord());
                            }
                            list.add(results.get(i));
                        }
                    }else{
                        if(results.get(i).getRssi() > lowRssi){
                            BleDevice nDevice = new BleDevice();
                            nDevice.setDevice(results.get(i).getDevice());
                            nDevice.setRssi(results.get(i).getRssi());
                            nDevice.setScanRecord(results.get(i).getScanRecord());
                            //mBleDeviceListener.getScanResult(nDevice);
                        }
                        list.add(results.get(i));
                    }
               }
               mBleDeviceListener.getScanAllResult(list);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG,"onScanFailed:");
        }
    };


    public int send(byte[] data, BluetoothGattCharacteristic characteristic) {
        dataLength = data.length;
        final int shareLength = data.length/20;
        final int rest = data.length%20;
        int count = 0;
        for(int i=0;i<=shareLength;i++) {
            final int position = i;

            byte datas[] = null;

            if (position == shareLength) {
                if (rest > 0) {
                    datas = new byte[rest];
                    for (int a = 0; a < rest; a++) {
                        datas[a] = data[count];
                        count++;
                    }
                }
            } else {
                datas = new byte[20];
                for (int a = 0; a < 20; a++) {
                    datas[a] = data[count];
                    count++;
                }
            }
            try {
                if (datas != null) {
                    Log.e(TAG,"characteristic:"+new ByteUtil().byteArrayToHex(datas));
                    sendQueue.add(datas);
                }
            } catch (Exception ex) {
                Log.e(TAG,"ex:"+ex.toString());
            }
        }

        if (!isWriting) _send(characteristic);

        return 0; //0
    }

    private boolean _send(BluetoothGattCharacteristic characteristic) {
        String rebootProtocol = "ff ff ff ff 00 00 a8 00 0a 01 ";
        if (sendQueue.isEmpty()) {
            Log.e(TAG,"sendQueue.isEmpty():"+dataLength);
            return false;
        }
        characteristic.setValue(sendQueue.poll());

        if(rebootProtocol.contains(new ByteUtil().byteArrayToHex(characteristic.getValue()))){
            Log.e(TAG,"isWriting = false");
            isWriting = false; // Set the write in progress flag
        }else{
            Log.e(TAG,"isWriting = true");
            isWriting = true; // Set the write in progress flag
        }

        SystemClock.sleep(10);
        mGatt.writeCharacteristic(characteristic);
        return true;
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }


    public boolean deviceConnect(final BluetoothDevice device) {
        if (mBluetoothAdapter == null || device.getAddress() == null) {
            Log.w(TAG,"BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        /*// Previously connected device. Try to reconnect.
        if (mGatt != null) {
            Log.d(TAG,"Trying to use an existing mBluetoothGatt for connection.");
            if (mGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }*/
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mGatt = device.connectGatt(mContext, false, this);
        Log.e(TAG,"dis:"+refreshDeviceCache(mGatt));
        return true;
    }

    public void noDeviceDisconnect(){
        if(mGatt != null){
            mGatt.disconnect();
            mGatt.close();
        }
    }

    public void setScanFilter(int connectType,String filterData){
        mConnetType = connectType;
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter scan_filter;
        if(connectType==0){
            ParcelUuid serviceUUID = null;
            //services
            serviceUUID = new ParcelUuid(makeUUID(filterData));
            Log.e(TAG,"serviceUUID:"+serviceUUID.toString());
            scan_filter = new ScanFilter.Builder().setServiceUuid(serviceUUID).build();
            filters.add(scan_filter);
        }
        if(connectType==1){
            //name
            scan_filter = new ScanFilter.Builder().setDeviceName(filterData).build();
            filters.add(scan_filter);
        }
        if(connectType==2){
            //namePrefix
            scan_filter = new ScanFilter.Builder().build();
            filters.add(scan_filter);
        }
        mFilterData = filterData;
        mFilters = filters;
    }

    public int getmConnetType() {
        return mConnetType;
    }

    public String getmFilterData() {
        return mFilterData;
    }

    public UUID makeUUID(String value){
        UUID uuid;
        String hexId ="";
        if(value.length() < 8) {
            hexId = Integer.toHexString(Integer.parseInt(value));
        }
        if(hexId.length() == 4){
            uuid = UUID.fromString(BASE_UUID.replace("xxxxxxxx",("0000"+hexId)));
        }else if(hexId.length() == 8){
            uuid = UUID.fromString(BASE_UUID.replace("xxxxxxxx",(hexId)));
        }else{
            uuid = UUID.fromString(value);
        }
        return uuid;
    }

    public byte[] rebootMultiroleAggregator(){
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
