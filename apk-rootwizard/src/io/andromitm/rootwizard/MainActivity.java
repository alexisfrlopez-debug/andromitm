package io.andromitm.rootwizard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * AndroMITM RootWizard — assistant de root guidé (Blackview Xplore1 / DokeOS 4.2 / Android 15).
 * UI 100% programmatique, sans dépendance externe : build aapt2 + javac + d8.
 */
public class MainActivity extends Activity {

    static final int C_BG     = 0xFF0D1117;
    static final int C_CARD   = 0xFF161B22;
    static final int C_LINE   = 0xFF30363D;
    static final int C_ACCENT = 0xFF00E676;
    static final int C_TXT    = 0xFFE6EDF3;
    static final int C_MUT    = 0xFF8B949E;
    static final int C_DANGER = 0xFFF85149;
    static final int C_WARN   = 0xFFD29922;
    static final int C_OK     = 0xFF3FB950;

    Handler ui;
    TextView tvModel, tvAndroid, tvRom, tvSoc, socNote;
    TextView stBoot, stOem, stAdb, stMagisk, stRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = new Handler(Looper.getMainLooper());
        Window w = getWindow();
        w.setStatusBarColor(C_BG);
        w.setNavigationBarColor(C_BG);
        setContentView(buildUi());
        refresh();
    }

    // ═════════════════════════════ UI ═════════════════════════════

    View buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(C_BG);
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(32));
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(title(L10n.s("app_title"), 26, C_ACCENT, true));
        root.addView(title(L10n.s("app_sub"), 13, C_MUT, false));
        root.addView(gap(14));

        LinearLayout warn = card(C_DANGER);
        warn.addView(title(L10n.s("warn_title"), 17, C_DANGER, true));
        warn.addView(gap(6));
        warn.addView(body(L10n.s("warn_body"), C_TXT));
        root.addView(warn);
        root.addView(gap(12));

        LinearLayout hon = card(C_WARN);
        hon.addView(title(L10n.s("honest_title"), 16, C_WARN, true));
        hon.addView(gap(6));
        hon.addView(body(L10n.s("honest_body"), C_TXT));
        root.addView(hon);
        root.addView(gap(12));

        LinearLayout dev = card(C_LINE);
        dev.addView(title(L10n.s("device_title"), 17, C_ACCENT, true));
        dev.addView(gap(6));
        tvModel = body("", C_TXT); dev.addView(tvModel);
        tvAndroid = body("", C_TXT); dev.addView(tvAndroid);
        tvRom = body("", C_MUT); dev.addView(tvRom);
        tvSoc = body("", C_TXT); dev.addView(tvSoc);
        socNote = body("", C_WARN); dev.addView(socNote);
        root.addView(dev);
        root.addView(gap(12));

        LinearLayout st = card(C_LINE);
        st.addView(title(L10n.s("status_title"), 17, C_ACCENT, true));
        stBoot = addStatusRow(st, L10n.s("st_boot"));
        stOem = addStatusRow(st, L10n.s("st_oem"));
        stAdb = addStatusRow(st, L10n.s("st_adb"));
        stMagisk = addStatusRow(st, L10n.s("st_magisk"));
        stRoot = addStatusRow(st, L10n.s("st_root"));
        st.addView(gap(8));
        st.addView(button(L10n.s("btn_refresh"), C_ACCENT, C_BG, new View.OnClickListener() {
            public void onClick(View v) { refresh(); }
        }));
        root.addView(st);
        root.addView(gap(14));

        root.addView(stepCard("0", L10n.s("s0_t"), L10n.s("s0_b"), null, null));
        root.addView(stepCard("1", L10n.s("s1_t"), L10n.s("s1_b"), null, null));
        root.addView(stepCard("2", L10n.s("s2_t"), L10n.s("s2_b"),
                new String[]{L10n.s("btn_open_dev")},
                new View.OnClickListener[]{new View.OnClickListener() {
                    public void onClick(View v) { openDevSettings(); }
                }}));
        root.addView(stepCard("3", L10n.s("s3_t"), L10n.s("s3_b"),
                new String[]{L10n.s("btn_copy")},
                new View.OnClickListener[]{new View.OnClickListener() {
                    public void onClick(View v) { copy("andromitm-unlock", L10n.s("cmds_3")); }
                }}));
        root.addView(stepCard("4", L10n.s("s4_t"), L10n.s("s4_b"),
                new String[]{L10n.s("btn_blackview")},
                new View.OnClickListener[]{new View.OnClickListener() {
                    public void onClick(View v) { openUrl("https://www.blackview.hk/"); }
                }}));
        root.addView(stepCard("5", L10n.s("s5_t"), L10n.s("s5_b"),
                new String[]{L10n.s("btn_magisk"), L10n.s("btn_open_magisk")},
                new View.OnClickListener[]{new View.OnClickListener() {
                    public void onClick(View v) { openUrl("https://github.com/topjohnwu/Magisk/releases"); }
                }, new View.OnClickListener() {
                    public void onClick(View v) { openMagisk(); }
                }}));
        root.addView(stepCard("6", L10n.s("s6_t"), L10n.s("s6_b"),
                new String[]{L10n.s("btn_copy")},
                new View.OnClickListener[]{new View.OnClickListener() {
                    public void onClick(View v) { copy("andromitm-flash", L10n.s("cmds_6")); }
                }}));
        root.addView(stepCard("7", L10n.s("s7_t"), L10n.s("s7_b"),
                new String[]{L10n.s("btn_test_root")},
                new View.OnClickListener[]{new View.OnClickListener() {
                    public void onClick(View v) { testRoot(); }
                }}));

        root.addView(gap(14));
        LinearLayout foot = card(C_LINE);
        foot.addView(body(L10n.s("footer"), C_MUT));
        root.addView(foot);
        return sv;
    }

    View stepCard(String num, String t, String b, String[] labels, View.OnClickListener[] handlers) {
        LinearLayout c = card(C_LINE);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = new TextView(this);
        badge.setText(num);
        badge.setTextColor(C_BG);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setTextSize(16);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(C_ACCENT);
        g.setShape(GradientDrawable.OVAL);
        badge.setBackground(g);
        head.addView(badge, new LinearLayout.LayoutParams(dp(30), dp(30)));

        TextView tv = title(t, 15, C_TXT, true);
        tv.setPadding(dp(10), 0, 0, 0);
        head.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        c.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        c.addView(gap(6));
        c.addView(body(b, C_MUT));

        if (labels != null) {
            c.addView(gap(8));
            for (int i = 0; i < labels.length; i++) {
                c.addView(button(labels[i], C_ACCENT, C_BG, handlers[i]));
                c.addView(gap(6));
            }
        }
        rootSpacing(c);
        return c;
    }

    // ════════════════════════ détections ════════════════════════

    void refresh() {
        tvModel.setText(L10n.s("device_model") + " : " + Build.MANUFACTURER + " " + Build.MODEL);
        tvAndroid.setText("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        String disp = Build.DISPLAY;
        tvRom.setText(L10n.s("device_rom") + " : " + (disp == null || disp.isEmpty() ? "?" : disp));

        String soc = socModel();
        tvSoc.setText(L10n.s("device_soc") + " : " + soc);

        String low = soc.toLowerCase();
        String familyKey;
        if (low.contains("mediatek") || low.matches(".*(mt[0-9]{4}).*")) familyKey = "soc_mtk";
        else if (low.contains("unisoc") || low.contains("ums9") || low.contains("sp98") || low.contains("sc98"))
            familyKey = "soc_unisoc";
        else if (low.contains("qcom") || low.contains("msm") || low.matches(".*(sm[0-9]{4}).*")
                || low.contains("sdm") || low.contains("apq")) familyKey = "soc_qcom";
        else familyKey = "soc_other";
        socNote.setText(L10n.s(familyKey));

        String vbs = prop("ro.boot.verifiedbootstate");
        String fl = prop("ro.boot.flash.locked");
        String bootVal; int bootCol;
        if ("0".equals(fl) || "orange".equals(vbs)) { bootVal = L10n.s("val_unlocked"); bootCol = C_DANGER; }
        else if ("yellow".equals(vbs)) { bootVal = L10n.s("val_yellow"); bootCol = C_WARN; }
        else if ("red".equals(vbs)) { bootVal = L10n.s("val_red"); bootCol = C_DANGER; }
        else if ("1".equals(fl) || "green".equals(vbs)) { bootVal = L10n.s("val_locked"); bootCol = C_OK; }
        else { bootVal = L10n.s("val_unknown"); bootCol = C_MUT; }
        setSt(stBoot, bootVal, bootCol);

        int oem = gl("oem_unlock_allowed");
        setSt(stOem, oem == 1 ? L10n.s("val_on") : L10n.s("val_off"), oem == 1 ? C_OK : C_WARN);
        int adb = gl("adb_enabled");
        setSt(stAdb, adb == 1 ? L10n.s("val_on") : L10n.s("val_off"), adb == 1 ? C_OK : C_WARN);

        try {
            getPackageManager().getPackageInfo("com.topjohnwu.magisk", 0);
            setSt(stMagisk, L10n.s("val_installed"), C_OK);
        } catch (Exception e) {
            setSt(stMagisk, L10n.s("val_missing"), C_WARN);
        }

        setSt(stRoot, L10n.s("val_checking"), C_MUT);
        testRoot();
    }

    String socModel() {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                String mo = Build.SOC_MODEL;
                if (mo != null && !mo.isEmpty() && !"unknown".equalsIgnoreCase(mo)) {
                    String m = Build.SOC_MANUFACTURER;
                    return (m != null && !m.isEmpty() ? m + " " : "") + mo;
                }
            } catch (Throwable ignored) { }
        }
        String b = (prop("ro.board.platform") + " " + prop("ro.soc.model") + " " + Build.HARDWARE).trim();
        return b.isEmpty() ? "?" : b;
    }

    String prop(String key) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            return (String) c.getMethod("get", String.class).invoke(null, key);
        } catch (Throwable t) {
            return "";
        }
    }

    int gl(String key) {
        try {
            return Settings.Global.getInt(getContentResolver(), key);
        } catch (Exception e) {
            return 0;
        }
    }

    void setSt(TextView tv, String val, int color) {
        tv.setText(val);
        tv.setTextColor(color);
    }

    // ════════════════════════ actions ════════════════════════

    void testRoot() {
        new Thread(new Runnable() {
            public void run() {
                int rc = -1;
                String out = "";
                try {
                    final Process pr = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(6000);
                                if (isAlive(pr)) pr.destroy();
                            } catch (InterruptedException ignored) { }
                        }
                    }).start();
                    BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String l;
                    while ((l = r.readLine()) != null) sb.append(l);
                    rc = pr.waitFor();
                    out = sb.toString();
                } catch (Exception e) {
                    rc = -1;
                }
                final boolean ok = rc == 0 && out.contains("uid=0");
                final String outF = out;
                ui.post(new Runnable() {
                    public void run() {
                        String shown = outF == null || outF.isEmpty()
                                ? "uid=0" : outF.substring(0, Math.min(70, outF.length()));
                        setSt(stRoot, ok ? L10n.s("val_yes") + " — " + shown : L10n.s("val_off"),
                                ok ? C_OK : C_WARN);
                        if (ok) Toast.makeText(MainActivity.this, L10n.s("toast_root_ok"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    static boolean isAlive(Process p) {
        try {
            p.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    void copy(String label, String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label, text));
            Toast.makeText(this, L10n.s("toast_copied"), Toast.LENGTH_SHORT).show();
        }
    }

    void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, L10n.s("toast_no_app"), Toast.LENGTH_SHORT).show();
        }
    }

    void openDevSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception e2) {
                Toast.makeText(this, L10n.s("toast_no_app"), Toast.LENGTH_SHORT).show();
            }
        }
    }

    void openMagisk() {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage("com.topjohnwu.magisk");
            if (i != null) startActivity(i);
            else openUrl("https://github.com/topjohnwu/Magisk/releases");
        } catch (Exception e) {
            Toast.makeText(this, L10n.s("toast_no_app"), Toast.LENGTH_SHORT).show();
        }
    }

    // ════════════════════════ widgets ════════════════════════

    int dp(float v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    LinearLayout card(int borderColor) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(C_CARD);
        g.setCornerRadius(dp(14));
        g.setStroke(dp(1), borderColor);
        c.setBackground(g);
        int p = dp(16);
        c.setPadding(p, p, p, p);
        return c;
    }

    /** marge verticale entre les cartes (le padding parent ne suffit pas) */
    void rootSpacing(LinearLayout card) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        card.setLayoutParams(lp);
    }

    TextView title(String t, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        return v;
    }

    TextView body(String t, int color) {
        TextView v = title(t, 14, color, false);
        v.setLineSpacing(dp(2), 1f);
        return v;
    }

    View gap(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    TextView addStatusRow(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView l = title(label, 14, C_TXT, false);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView val = title("…", 14, C_MUT, true);
        row.addView(val, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return val;
    }

    Button button(String label, int bgColor, int fgColor, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(fgColor);
        b.setTextSize(15);
        b.setTypeface(null, Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(12));
        b.setBackground(g);
        b.setPadding(dp(16), dp(12), dp(16), dp(12));
        b.setOnClickListener(listener);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return b;
    }
}
