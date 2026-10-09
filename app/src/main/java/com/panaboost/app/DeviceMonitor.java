package com.panaboost.app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import java.util.Locale;

public final class DeviceMonitor {
    private DeviceMonitor() {}

    public static final class Sample {
        public final long freeRam, totalRam, freeStorage, totalStorage;
        public final int batteryLevel, batteryTempTenths, batteryStatus, thermalStatus;
        public final boolean batterySaver;
        public final String network;
        public final long timestamp;

        Sample(long fr, long tr, long fs, long ts, int bl, int bt, int bs, int thermal, boolean saver, String net) {
            freeRam = fr; totalRam = tr; freeStorage = fs; totalStorage = ts;
            batteryLevel = bl; batteryTempTenths = bt; batteryStatus = bs;
            thermalStatus = thermal; batterySaver = saver; network = net;
            timestamp = System.currentTimeMillis();
        }
        public double ramUsedPercent() { return totalRam > 0 ? 100d * (totalRam - freeRam) / totalRam : 0; }
        public double storageUsedPercent() { return totalStorage > 0 ? 100d * (totalStorage - freeStorage) / totalStorage : 0; }
    }

    public static Sample read(Context c) {
        long freeRam = 0, totalRam = 0, freeStorage = 0, totalStorage = 0;
        try {
            ActivityManager.MemoryInfo m = new ActivityManager.MemoryInfo();
            ((ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(m);
            freeRam = m.availMem; totalRam = m.totalMem;
        } catch (Exception ignored) { }
        try {
            StatFs s = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            freeStorage = s.getAvailableBytes(); totalStorage = s.getTotalBytes();
        } catch (Exception ignored) { }
        int pct = -1, temp = -1, status = -1;
        try {
            Intent b = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (b != null) {
                int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                pct = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
                temp = b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            }
        } catch (Exception ignored) { }
        int thermal = -1; boolean saver = false;
        try {
            PowerManager pm = (PowerManager)c.getSystemService(Context.POWER_SERVICE);
            saver = pm.isPowerSaveMode();
            if (Build.VERSION.SDK_INT >= 29) thermal = pm.getCurrentThermalStatus();
        } catch (Exception ignored) { }
        return new Sample(freeRam, totalRam, freeStorage, totalStorage, pct, temp, status, thermal, saver, network(c));
    }

    public static String size(long bytes) {
        if (bytes < 0) return "—";
        return String.format(Locale.getDefault(), "%.1f GB", bytes / 1073741824.0);
    }
    public static String temp(int tenths) {
        return tenths >= 0 ? String.format(Locale.getDefault(), "%.1f °C", tenths / 10f) : "No disponible";
    }
    public static String thermal(int status) {
        switch (status) {
            case PowerManager.THERMAL_STATUS_NONE: return "Normal";
            case PowerManager.THERMAL_STATUS_LIGHT: return "Ligero";
            case PowerManager.THERMAL_STATUS_MODERATE: return "Moderado";
            case PowerManager.THERMAL_STATUS_SEVERE: return "Alto";
            case PowerManager.THERMAL_STATUS_CRITICAL: return "Crítico";
            case PowerManager.THERMAL_STATUS_EMERGENCY: return "Emergencia";
            case PowerManager.THERMAL_STATUS_SHUTDOWN: return "Apagado térmico";
            default: return "No disponible";
        }
    }
    public static String network(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities cap = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (cap == null) return "Sin conexión";
            if (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
            if (cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Datos móviles";
            if (cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
            if (cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return "VPN";
            return "Otra conexión";
        } catch (Exception e) { return "Desconocida"; }
    }
}
