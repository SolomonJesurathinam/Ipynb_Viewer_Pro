package com.example.ipynbviewerpro;

public class DataHolder {
    private static final DataHolder ourInstance = new DataHolder();

    public static DataHolder getInstance() {
        return ourInstance;
    }

    private String encodedData;

    private DataHolder() {
    }

    public String getEncodedData() {
        return encodedData;
    }

    public void setEncodedData(String data) {
        this.encodedData = data;
    }
}
