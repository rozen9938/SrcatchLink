package com.roborisen.scratchlink.data;

public class DiscoverScanFilter {
    /**
     * connectType
     * 0: services, 1:name , 2: NamePrefix
     */
    private int connectType;
    private String filtersType;
    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public int getConnectType() {
        return connectType;
    }

    public void setConnectType(int connectType) {
        this.connectType = connectType;
    }

    public String getFiltersType() {
        return filtersType;
    }

    public void setFiltersType(String filtersType) {
        this.filtersType = filtersType;
    }
}
