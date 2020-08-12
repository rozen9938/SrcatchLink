package com.roborisen.scratchlink.util;

public class ByteUtil {
    public int[] byteToInt(byte[] sps,byte[] step){
        int [] data = new int[2];
        short val=(short)(((sps[0] & 0xFF) << 8) | (sps[1] & 0xFF));
        short val2=(short)(((step[0] & 0xFF) << 8) | (step[1] & 0xFF));
        data[0] = (int)val;
        data[1] = (int)val2;
        return data;
    }
    public int byteToInt(byte[] sps){
        int data =0;
        short val=(short)(((sps[0] & 0xFF) << 8) | (sps[1] & 0xFF));
        data = (int)val;
        return data;
    }
    public byte[] intToByte(int step){
        byte[] data = new byte[2];
        data [0] = (byte)(step>>8);
        data [1] = (byte)(step&0x000000FF);
        return data;
    }

    public byte[] setActivityCode(int assignCubeId,int modelId){
        int activityId  = (assignCubeId*256*16) + (modelId);
        return intToByte(activityId);
    }

    public byte[] intToByte(int steps[]){
        int a=0;
        byte[] data = new byte[(steps.length*2)];
        a=0;
        for (int i=0;i<steps.length;i++){
            data[a] = (byte)(steps[i]>>8);
            a++;
            data [a] = (byte)(steps[i]&0x000000FF);
            a++;
        }
        return data;
    }

    //steps and sps sperate
    public byte[] intToByte(int sps [],int steps[]){
        int totalSteps[] = new int[(steps.length*2)];
        int a=0;
        for(int i =0;i<steps.length;i++){
            totalSteps[a] = sps[i];
            a++;
            totalSteps[a] = steps[i];
            a++;
        }

        byte[] data = new byte[(totalSteps.length*2)];
        a=0;
        for (int i=0;i<totalSteps.length;i++){
            data[a] = (byte)(totalSteps[i]>>8);
            a++;
            data [a] = (byte)(totalSteps[i]&0x000000FF);
            a++;
        }
        return data;
    }

    public String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for(final byte b: a)
            sb.append(String.format("%02x ", b&0xff));
        return sb.toString();
    }

    public byte[] stringToByte(String dataString){
        String[] dataStringSplit = dataString.split(" ");
        byte [] data = new byte[dataStringSplit.length];
        for(int i=0;i<dataStringSplit.length;i++){
            data[i] = (byte) Integer.parseInt(dataStringSplit[i],16);
        }
        return data;
    }
}
