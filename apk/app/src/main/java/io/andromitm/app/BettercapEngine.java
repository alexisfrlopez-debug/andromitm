package io.andromitm.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pilote du moteur bettercap :
 *  - lancement via su avec LD_LIBRARY_PATH pointant vers les libs embarquées
 *  - REST API locale (login → token) pour envoyer les commandes
 *  - flux /api/events consommé en continu → listeners
 */
public final class BettercapEngine {
    public enum State { IDLE, STARTING, RUNNING, ERROR }

    public interface Listener {
        void onStateChanged(State s, String detail);
        void onEvent(String tag, JSONObject data);
        void onLog(String level, String message);
    }

    private static volatile BettercapEngine instance;
    public static BettercapEngine get(Context c) {
        if (instance == null) instance = new BettercapEngine(c.getApplicationContext());
        return instance;
    }

    private final Context ctx;
    private final Prefs prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newCachedThreadPool();

    private volatile State state = State.IDLE;
    private volatile Listener listener;
    private volatile Process engineProc;
    private volatile String token;
    private volatile String user = "andromitm";
    private volatile String pass = "";
    private String iface = "wlan0";
    private int apiPort = 8081;
    private boolean ipForwardWasOn = false;

    private BettercapEngine(Context ctx) { this.ctx = ctx; this.prefs = new Prefs(ctx); }

    public State state() { return state; }
    public void setListener(Listener l) { listener = l; }
    public String iface() { return iface; }

    private void setState(State s, String detail) {
        state = s;
        Listener l = listener;
        if (l != null) main.post(() -> l.onStateChanged(s, detail));
    }

    private void log(String level, String msg) {
        Listener l = listener;
        if (l != null) main.post(() -> l.onLog(level, msg));
    }

    private void event(String tag, JSONObject data) {
        Listener l = listener;
        if (l != null) main.post(() -> l.onEvent(tag, data));
    }

    /** Chemin du binaire moteur (custom si configuré, sinon lib embarquée). */
    private String enginePath() {
        String custom = prefs.getEnginePath();
        if (!custom.isEmpty()) return custom;
        return ctx.getApplicationInfo().nativeLibraryDir + "/libbettercap.so";
    }

    private String libDir() { return ctx.getApplicationInfo().nativeLibraryDir; }

    // ─────────────────────────────── cycle de vie ───────────────────────────────

    public void start(String iface) {
        if (state == State.RUNNING || state == State.STARTING) return;
        this.iface = iface;
        setState(State.STARTING, ctx.getString(R.string.engine_starting));

        pool.execute(() -> {
            // 0) root
            RootShell.Result root = RootShell.run("id");
            if (!root.out.contains("uid=0")) {
                setState(State.ERROR, ctx.getString(R.string.err_no_root));
                return;
            }
            // 1) binaire présent et exécutable ?
            String bin = enginePath();
            RootShell.Result chk = RootShell.run("test -x " + bin + " && echo ok");
            if (!chk.out.contains("ok")) {
                setState(State.ERROR, ctx.getString(R.string.err_engine_missing));
                return;
            }
            // 2) ip_forward : sauvegarde + activation
            ipForwardWasOn = "1".equals(RootShell.run("cat /proc/sys/net/ipv4/ip_forward").out.trim());
            RootShell.run("echo 1 > /proc/sys/net/ipv4/ip_forward");

            // 3) identifiants API de session
            user = "andromitm";
            pass = randomToken(16);

            // 4) éval bettercap : REST API + découverte passive
            String eval = "set api.rest.address 127.0.0.1:" + apiPort + "; "
                + "set api.rest.username " + user + "; "
                + "set api.rest.password " + pass + "; "
                + "api.rest on; "
                + "net.recon on; "
                + "set net.recon.sort by seen desc";
            String launch = "LD_LIBRARY_PATH=" + libDir() + " " + bin
                + " -iface " + iface + " -no-colors -eval '" + eval + "'";
            try {
                engineProc = RootShell.startLong(launch);
            } catch (Exception e) {
                RootShell.run("echo 0 > /proc/sys/net/ipv4/ip_forward");
                setState(State.ERROR, String.valueOf(e.getMessage()));
                return;
            }
            log("info", "moteur lancé sur " + iface + " (pid " + pidOf(engineProc) + ")");

            // 5) attente de la REST API
            token = null;
            for (int i = 0; i < 20 && token == null; i++) {
                if (engineProc == null) break;
                try { Thread.sleep(1000); } catch (InterruptedException ignored) { }
                token = login(user, pass);
            }
            if (token == null) {
                stop(true);
                setState(State.ERROR, ctx.getString(R.string.err_api_timeout));
                return;
            }
            log("info", "REST API connectée (127.0.0.1:" + apiPort + ")");
            setState(State.RUNNING, iface);
            watchEvents();
            watchProcess();
        });
    }

    public void stop() { stop(false); }

    private void stop(boolean internal) {
        if (!internal && state == State.IDLE) return;
        // tentatives d'arrêt propre des modules
        String t = token;
        if (t != null) {
            command("arp.spoof off");
            command("dns.spoof off");
            command("net.sniff off");
            command("net.recon off");
        }
        RootShell.runAsync("pkill -f bettercap", r -> { });
        if (engineProc != null) { engineProc.destroy(); engineProc = null; }
        token = null;
        // restauration ip_forward
        RootShell.runAsync("echo " + (ipForwardWasOn ? 1 : 0) + " > /proc/sys/net/ipv4/ip_forward",
            r -> log("info", "ip_forward restauré (" + (ipForwardWasOn ? "1" : "0") + ")"));
        setState(State.IDLE, null);
    }

    private void watchProcess() {
        final Process p = engineProc;
        pool.execute(() -> {
            try {
                int code = p.waitFor();
                if (state == State.RUNNING || state == State.STARTING) {
                    log("warn", "moteur terminé (code " + code + ")");
                    stop(true);
                }
            } catch (InterruptedException ignored) { }
        });
    }

    // ─────────────────────────────── REST API ───────────────────────────────

    private String login(String u, String p) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + apiPort + "/api/login").openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setConnectTimeout(1500);
            c.setReadTimeout(1500);
            c.setDoOutput(true);
            try (OutputStream os = c.getOutputStream()) {
                os.write(new JSONObject().put("username", u).put("password", p).toString().getBytes(StandardCharsets.UTF_8));
            }
            if (c.getResponseCode() != 200) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return new JSONObject(sb.toString()).optString("token", null);
        } catch (Exception e) {
            return null;
        }
    }

    /** Envoie une commande à la session bettercap (ex : "arp.spoof on"). */
    public void command(String cmd) {
        final String t = token;
        if (t == null) { log("warn", "moteur arrêté — commande ignorée : " + cmd); return; }
        pool.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + apiPort + "/api/session").openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("Authorization", "Bearer " + t);
                c.setConnectTimeout(2000);
                c.setReadTimeout(4000);
                c.setDoOutput(true);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(new JSONObject().put("cmd", cmd).toString().getBytes(StandardCharsets.UTF_8));
                }
                int code = c.getResponseCode();
                if (code == 200) log("info", "✓ " + cmd);
                else log("error", "✗ " + cmd + " (http " + code + ")");
            } catch (Exception e) {
                log("error", "✗ " + cmd + " : " + e.getMessage());
            }
        });
    }

    /** Flux d'événements /api/events (JSON ligne par ligne, stream non terminé). */
    private void watchEvents() {
        pool.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + apiPort + "/api/events").openConnection();
                c.setRequestProperty("Authorization", "Bearer " + token);
                c.setConnectTimeout(3000);
                c.setReadTimeout(0); // stream infini
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while (state == State.RUNNING && (line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    try {
                        JSONObject ev = new JSONObject(line);
                        String tag = ev.optString("tag", "");
                        JSONObject data = ev.optJSONObject("data");
                        if (data == null) data = new JSONObject();
                        if (!tag.isEmpty()) event(tag, data);
                    } catch (Exception ignored) { }
                }
            } catch (Exception e) {
                if (state == State.RUNNING) log("warn", "flux événements interrompu : " + e.getMessage());
            }
        });
    }

    private static String randomToken(int n) {
        String alphabet = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        return sb.toString();
    }

    private static long pidOf(Process p) {
        try {
            return ((Long) p.getClass()
                .getMethod("pid").invoke(p));
        } catch (Exception e) {
            return -1;
        }
    }
}
