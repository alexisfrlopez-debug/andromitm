package io.andromitm.app;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** AndroMITM — cockpit principal : pilotage, cibles, journal live. */
public class MainActivity extends AppCompatActivity implements BettercapEngine.Listener {

    private Prefs prefs;
    private BettercapEngine engine;
    private final EventsAdapter events = new EventsAdapter();
    private HostsAdapter hostsAdapter;
    private final Map<String, Host> hosts = new LinkedHashMap<>();
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    // vues
    private TextView tvRoot, tvEngine, tvIfaceInfo, tvEngineDetail;
    private Spinner spIface;
    private MaterialButton btnEngine;
    private SwitchMaterial swArp, swDns, swSniff;
    private View pageDashboard, pageTargets, pageLogs;
    private TextView tvTargetsCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        engine = BettercapEngine.get(this);
        setContentView(R.layout.activity_main);

        // vues
        tvRoot = findViewById(R.id.tv_root);
        tvEngine = findViewById(R.id.tv_engine);
        tvEngineDetail = findViewById(R.id.tv_engine_detail);
        tvIfaceInfo = findViewById(R.id.tv_iface_info);
        spIface = findViewById(R.id.sp_iface);
        btnEngine = findViewById(R.id.btn_engine);
        swArp = findViewById(R.id.sw_arp);
        swDns = findViewById(R.id.sw_dns);
        swSniff = findViewById(R.id.sw_sniff);
        tvTargetsCount = findViewById(R.id.tv_targets_count);

        RecyclerView rvHosts = findViewById(R.id.rv_hosts);
        hostsAdapter = new HostsAdapter(new HashSet<>(prefs.getTargets()));
        rvHosts.setLayoutManager(new LinearLayoutManager(this));
        rvHosts.setAdapter(hostsAdapter);

        RecyclerView rvEvents = findViewById(R.id.rv_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(events);

        pageDashboard = findViewById(R.id.page_dashboard);
        pageTargets = findViewById(R.id.page_targets);
        pageLogs = findViewById(R.id.page_logs);

        // navigation
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            pageDashboard.setVisibility(id == R.id.nav_dash ? View.VISIBLE : View.GONE);
            pageTargets.setVisibility(id == R.id.nav_targets ? View.VISIBLE : View.GONE);
            pageLogs.setVisibility(id == R.id.nav_logs ? View.VISIBLE : View.GONE);
            if (id == R.id.nav_targets) loadHostsFromArpTable();
            return true;
        });

        btnEngine.setOnClickListener(v -> {
            if (engine.state() == BettercapEngine.State.RUNNING) engine.stop();
            else engine.start(String.valueOf(spIface.getSelectedItem()));
            persistTargets();
        });

        swArp.setOnCheckedChangeListener((b, checked) -> {
            Set<String> targets = prefs.getTargets();
            if (checked) {
                if (targets.isEmpty()) {
                    toast(getString(R.string.err_no_targets));
                    swArp.setChecked(false);
                    return;
                }
                engine.command("set arp.spoof.targets " + String.join(",", targets));
                engine.command("arp.spoof on");
            } else engine.command("arp.spoof off");
        });

        swDns.setOnCheckedChangeListener((b, checked) -> {
            if (checked) showDnsDialog();
            else engine.command("dns.spoof off");
        });

        swSniff.setOnCheckedChangeListener((b, checked) ->
            engine.command(checked ? "net.sniff on" : "net.sniff off"));

        findViewById(R.id.btn_refresh_hosts).setOnClickListener(v -> loadHostsFromArpTable());
        findViewById(R.id.btn_clear_logs).setOnClickListener(v -> events.clear());
        findViewById(R.id.btn_engine_path).setOnClickListener(v -> showEnginePathDialog());
        findViewById(R.id.btn_ca_guide).setOnClickListener(v -> showCaGuide());

        loadInterfaces();
        engine.setListener(this);
        updateUiForState(engine.state(), null);
        checkRoot();
    }

    // ─────────────────────────── moteur : callbacks ───────────────────────────

    @Override
    public void onStateChanged(BettercapEngine.State s, String detail) {
        runOnUiThread(() -> updateUiForState(s, detail));
    }

    @Override
    public void onEvent(String tag, JSONObject data) {
        runOnUiThread(() -> {
            switch (tag) {
                case "endpoint.new" -> {
                    Host h = new Host(
                        data.optString("ipv4", data.optString("address", "")),
                        data.optString("mac", ""),
                        data.optString("vendor", null),
                        data.optString("hostname", ""));
                    if (!h.ip.isEmpty() && !"?".equals(h.ip)) {
                        hosts.put(h.ip, h);
                        hostsAdapter.replace(new ArrayList<>(hosts.values()));
                        tvTargetsCount.setText(getString(R.string.targets_count, hosts.size()));
                    }
                    events.add("info", "⊕ hôte : " + h.ip + " (" + (h.vendor == null ? "?" : h.vendor) + ")");
                }
                case "net.sniff.http.request" -> events.add("info",
                    "HTTP " + data.optString("method", "?") + " " + data.optString("hostname", "")
                        + data.optString("path", "/"));
                case "net.sniff.http.response" -> events.add("info",
                    "HTTP ← " + data.optString("status_code", "?") + " " + data.optString("hostname", ""));
                case "dns.spoofer" -> events.add("warn", "DNS : " + data);
                case "arp.spoof.on" -> events.add("warn", "ARP spoof ACTIF");
                case "arp.spoof.off" -> events.add("info", "ARP spoof arrêté");
                case "sys.log" -> events.add("info", data.optString("message", String.valueOf(data)));
                case "session.started" -> events.add("info", "session bettercap démarrée");
                default -> events.add("info", tag + (data.length() > 0 ? " : " + compact(data) : ""));
            }
        });
    }

    @Override
    public void onLog(String level, String message) {
        runOnUiThread(() -> events.add(level, message));
    }

    private static String compact(JSONObject o) {
        String s = String.valueOf(o);
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }

    // ─────────────────────────── interface ───────────────────────────

    private void updateUiForState(BettercapEngine.State s, String detail) {
        boolean running = s == BettercapEngine.State.RUNNING;
        boolean starting = s == BettercapEngine.State.STARTING;
        boolean error = s == BettercapEngine.State.ERROR;

        btnEngine.setText(running ? R.string.engine_stop : R.string.engine_start);
        btnEngine.setEnabled(!starting);
        int color = running ? getColor(R.color.state_running)
            : error ? getColor(R.color.state_error)
            : starting ? getColor(R.color.state_starting)
            : getColor(R.color.state_idle);
        tvEngine.setText(running ? R.string.engine_running
            : starting ? R.string.engine_starting
            : error ? R.string.engine_error : R.string.engine_idle);
        tvEngine.setTextColor(color);
        tvEngineDetail.setText(detail == null || detail.isEmpty() ? "—" : detail);

        swArp.setEnabled(running);
        swDns.setEnabled(running);
        swSniff.setEnabled(running);
        if (!running) { swArp.setChecked(false); swDns.setChecked(false); swSniff.setChecked(false); }
    }

    private void checkRoot() {
        RootShell.check(r -> runOnUiThread(() -> {
            boolean ok = r.exit == 0;
            tvRoot.setText(ok ? R.string.root_ok : R.string.root_missing);
            tvRoot.setTextColor(getColor(ok ? R.color.state_running : R.color.state_error));
            if (!ok) toast(getString(R.string.err_no_root));
        }));
    }

    /** Interfaces disponibles depuis /sys/class/net. */
    private void loadInterfaces() {
        pool.execute(() -> {
            RootShell.Result r = RootShell.run("ls /sys/class/net");
            List<String> ifaces = new ArrayList<>();
            Collections.addAll(ifaces, r.out.split("\\s+"));
            ifaces.removeIf(i -> i.isEmpty() || i.equals("lo") || i.equals("sit0"));
            if (ifaces.isEmpty()) ifaces.add("wlan0");
            runOnUiThread(() -> {
                ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ifaces);
                spIface.setAdapter(ad);
                int sel = ifaces.indexOf(prefs.getIface());
                if (sel >= 0) spIface.setSelection(sel);
                tvIfaceInfo.setText(getString(R.string.iface_count, ifaces.size()));
            });
        });
    }

    /** Repli sans moteur : table ARP du noyau (fonctionne même sans root). */
    private void loadHostsFromArpTable() {
        pool.execute(() -> {
            List<Host> list = new ArrayList<>();
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"cat", "/proc/net/arp"});
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (first) { first = false; continue; }
                    String[] c = line.trim().split("\\s+");
                    if (c.length >= 6 && !"00:00:00:00:00:00".equals(c[3])) {
                        list.add(new Host(c[0], c[3], null, ""));
                    }
                }
            } catch (Exception ignored) { }
            runOnUiThread(() -> {
                if (engine.state() == BettercapEngine.State.RUNNING) return; // net.recon fait mieux
                hosts.clear();
                for (Host h : list) hosts.put(h.ip, h);
                hostsAdapter.replace(list);
                tvTargetsCount.setText(getString(R.string.targets_count, list.size()));
            });
        });
    }

    private void persistTargets() { hostsAdapter.persistTo(prefs); }

    private void showDnsDialog() {
        LinearLayout box = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_dns, null);
        EditText domains = box.findViewById(R.id.dns_domains);
        EditText ip = box.findViewById(R.id.dns_ip);
        ip.setText(localIp());
        new AlertDialog.Builder(this)
            .setTitle(R.string.dns_dialog_title)
            .setView(box)
            .setPositiveButton(R.string.common_enable, (DialogInterface d, int w) -> {
                String dom = domains.getText().toString().trim();
                String addr = ip.getText().toString().trim();
                if (dom.isEmpty() || addr.isEmpty()) { swDns.setChecked(false); return; }
                engine.command("set dns.spoof.domains " + dom.replace(" ", ","));
                engine.command("set dns.spoof.address " + addr);
                engine.command("dns.spoof on");
            })
            .setNegativeButton(R.string.common_cancel, (DialogInterface d, int w) -> swDns.setChecked(false))
            .show();
    }

    private void showEnginePathDialog() {
        EditText in = new EditText(this);
        in.setSingleLine(true);
        in.setHint("/data/data/com.termux/files/…/bettercap");
        in.setText(prefs.getEnginePath());
        new AlertDialog.Builder(this)
            .setTitle(R.string.engine_path_title)
            .setMessage(R.string.engine_path_hint)
            .setView(in)
            .setPositiveButton(R.string.common_save, (d, w) -> {
                prefs.setEnginePath(in.getText().toString().trim());
                toast(getString(R.string.common_saved));
            })
            .setNegativeButton(R.string.common_cancel, null)
            .show();
    }

    private void showCaGuide() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.ca_title)
            .setMessage(R.string.ca_message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private String localIp() {
        try {
            for (java.net.NetworkInterface ni : Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (java.net.InetAddress ia : Collections.list(ni.getInetAddresses())) {
                    if (ia instanceof java.net.Inet4Address && ia.isSiteLocalAddress()) return ia.getHostAddress();
                }
            }
        } catch (Exception ignored) { }
        return "192.168.1.66";
    }

    private void toast(String m) {
        android.widget.Toast.makeText(this, m, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (engine.state() == BettercapEngine.State.RUNNING) engine.stop();
    }
}
