package com.project.guardianalertcapstone;

public class DeviceManager {
    private static DeviceManager instance;
    private boolean isConnected = false;

    private DeviceManager() {}

    public static DeviceManager getInstance() {
        if (instance == null) {
            instance = new DeviceManager();
        }
        return instance;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public void resetConnection() {
        isConnected = false;
    }
}
