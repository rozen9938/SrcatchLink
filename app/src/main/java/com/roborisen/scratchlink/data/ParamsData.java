package com.roborisen.scratchlink.data;

import java.util.UUID;

public class ParamsData {
    private UUID serviceId;
    private UUID characteristicId;
    private String message;
    private String encoding;
    private boolean startNotifications;

    public boolean isStartNotifications() {
        return startNotifications;
    }

    public void setStartNotifications(boolean startNotifications) {
        this.startNotifications = startNotifications;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getCharacteristicId() {
        return characteristicId;
    }

    public void setCharacteristicId(UUID characteristicId) {
        this.characteristicId = characteristicId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }


}
