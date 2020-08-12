package com.roborisen.scratchlink.ble;

import android.bluetooth.BluetoothDevice;


import java.io.Serializable;

import no.nordicsemi.android.support.v18.scanner.ScanRecord;


public class BleDevice {
    private BluetoothDevice device;
    private int rssi;
    private ScanRecord scanRecord;
    private int versionNum;

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public ScanRecord getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(ScanRecord scanRecord) {
        this.scanRecord = scanRecord;
    }

    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @Override
    public String toString() {
        String deviceInfo = "device : { name : "+device.getName()
                +", mac_addr:"
                +device.getAddress()
                +", rssi:"+getRssi()
                +", bonded State:"+device.getBondState();
        return deviceInfo;
    }
}
