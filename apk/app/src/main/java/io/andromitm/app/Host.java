package io.andromitm.app;

/** Hôte découvert sur le LAN. */
public class Host {
    public final String ip;
    public final String mac;
    public final String vendor;
    public final String hostname;
    public final boolean android;
    public long seen;

    public Host(String ip, String mac, String vendor, String hostname) {
        this.ip = ip == null ? "?" : ip;
        this.mac = mac == null ? "" : mac;
        String v = vendor;
        if (v == null || v.isEmpty() || "unknown".equalsIgnoreCase(v)) v = VendorDb.androidVendor(this.mac);
        this.vendor = v == null || v.isEmpty() ? null : v;
        this.hostname = hostname == null ? "" : hostname;
        this.android = VendorDb.androidVendor(this.mac) != null;
        this.seen = System.currentTimeMillis();
    }

    public String label() {
        if (hostname != null && !hostname.isEmpty()) return hostname;
        if (vendor != null) return vendor;
        return mac.isEmpty() ? ip : mac;
    }
}
