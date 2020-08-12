package com.roborisen.scratchlink.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;


import java.util.List;

import no.nordicsemi.android.support.v18.scanner.ScanResult;


public interface BleDeviceListener {
    void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);
    void onServiceDiscoverd(BluetoothGatt gatt, int status);
    void onCharacteristicRead(BluetoothGatt gatt, byte[] datas, int status);
    void onCharacteristicWrite(BluetoothGatt gatt, byte[] datas, int status);
    void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    void getScanAllResult(List<ScanResult> results);
    void startScan();
    void stopScan();
}
