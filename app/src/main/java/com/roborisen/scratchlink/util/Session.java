package com.roborisen.scratchlink.util;

import android.bluetooth.BluetoothDevice;
import android.util.Base64;
import android.util.Log;

import com.roborisen.scratchlink.data.DiscoverScanFilter;
import com.roborisen.scratchlink.data.ParamsData;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class Session {

    private final String TAG = getClass().getSimpleName();

    public DiscoverScanFilter receiveObject(String data){
        DiscoverScanFilter discoverScanFilter = new DiscoverScanFilter();
        try {
            JSONObject jsonObject = new JSONObject(data);
            //GetConnectType
            getConnectType(jsonObject.getString("params"),discoverScanFilter);
            discoverScanFilter.setId(jsonObject.getString("id"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return discoverScanFilter;
    }

    public JSONObject sendResult(int result, int id){
        JSONObject datas = new JSONObject();
        try{
            datas.put("jsonrpc","2.0");
            datas.put("id",id);
            datas.put("result",result);
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return datas;
    }

    public JSONObject readResult(String serviceId,String characteristicId, byte[] message,String encoding){
        JSONObject datas = new JSONObject();
        try {
            datas.put("serviceId",serviceId);
            datas.put("characteristicId",characteristicId);
            datas.put("message", Base64.encodeToString(message,Base64.DEFAULT).replace("\n",""));
            datas.put("encoding",encoding);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return datas;
    }

    public JSONObject sendObject(String method,JSONObject data){
        JSONObject datas = new JSONObject();
        try {
            datas.put("jsonrpc","2.0");
            datas.put("method",method);
            datas.put("params",data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
       return datas;
    }

    public ParamsData setParamsData(String serviceId, String characteristicId, String message, String encoding){
        ParamsData paramsData = new ParamsData();

        UUID serviceuId = UUID.fromString(serviceId);
        UUID characteristicuId = UUID.fromString(characteristicId);
        paramsData.setServiceId(serviceuId);
        paramsData.setCharacteristicId(characteristicuId);
        paramsData.setMessage(message);
        paramsData.setEncoding(encoding);
        return paramsData;
    }

    public ParamsData setParamsData(String serviceId, String characteristicId, boolean startNotification){
        ParamsData paramsData = new ParamsData();
        UUID serviceuId = UUID.fromString(serviceId);
        UUID characteristicuId = UUID.fromString(characteristicId);
        paramsData.setServiceId(serviceuId);
        paramsData.setCharacteristicId(characteristicuId);
        paramsData.setStartNotifications(startNotification);
        return paramsData;
    }

    public BluetoothDevice sendDeviceInfo(ScanResult scanResult, WebSocket conn){
        BluetoothDevice device = null;
        try {
            JSONObject data = new JSONObject();
            data.put("name",scanResult.getDevice().getName());
            data.put("rssi",scanResult.getRssi());
            data.put("peripheralId",scanResult.getDevice().getAddress().hashCode());
            JSONObject datas = sendObject("didDiscoverPeripheral",data);
            device = scanResult.getDevice();
            Log.e(TAG,"sendData:"+datas.toString());
            conn.send(datas.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return device;
    }

    public int getConnectType(String params, DiscoverScanFilter discoverScanFilter){
        int connectType=0;
        try {
            //get Filters Type
            JSONObject jsonParams = new JSONObject(params);
            JSONArray filtersObject = new JSONArray(jsonParams.getString("filters"));

            Iterator<String> keys =  filtersObject.getJSONObject(0).keys();

            //0:services , 1: name , 2: namePrefix
            int filterType=-1;
            String filterData = null;

            while (keys.hasNext()){
                String type = keys.next();
                if(type.equalsIgnoreCase("services")){
                    filterType = 0;
                    JSONArray services = new JSONArray(filtersObject.getJSONObject(0).getString("services"));
                    filterData = services.get(0).toString();
                    break;
                }

                if(type.equalsIgnoreCase("name")){
                    filterType = 1;
                    filterData = filtersObject.getJSONObject(0).getString("name");
                    break;
                }

                if(type.equalsIgnoreCase("namePrefix")){
                    filterType = 2;
                    filterData = filtersObject.getJSONObject(0).getString("namePrefix");
                    break;
                }
            }

            discoverScanFilter.setFiltersType(filterData);
            discoverScanFilter.setConnectType(filterType);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return connectType;
    }

}
