package io.andromitm.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Préférences persistantes (autorisation, cibles, réglages moteur). */
public class Prefs {
    private static final String FILE = "andromitm";
    private final SharedPreferences sp;

    public Prefs(Context c) { sp = c.getSharedPreferences(FILE, Context.MODE_PRIVATE); }

    public boolean isAuthorized() { return sp.getBoolean("authorized", false); }
    public void setAuthorized(boolean v) { sp.edit().putBoolean("authorized", v).apply(); }

    public String getIface() { return sp.getString("iface", "wlan0"); }
    public void setIface(String v) { sp.edit().putString("iface", v).apply(); }

    /** Chemin d'un binaire bettercap alternatif (appareils 64-bits-only, ex. via Termux). */
    public String getEnginePath() { return sp.getString("engine_path", ""); }
    public void setEnginePath(String v) { sp.edit().putString("engine_path", v == null ? "" : v).apply(); }

    /** Cibles ARP sélectionnées (IP). */
    public java.util.Set<String> getTargets() { return sp.getStringSet("targets", new java.util.HashSet<>()); }
    public void setTargets(java.util.Set<String> v) { sp.edit().putStringSet("targets", v).apply(); }
}
