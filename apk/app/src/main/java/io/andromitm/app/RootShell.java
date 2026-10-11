package io.andromitm.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Exécution de commandes via su (root). Toutes les sorties sont journalisables. */
public final class RootShell {
    public static class Result {
        public final int exit;
        public final String out;
        public Result(int exit, String out) { this.exit = exit; this.out = out == null ? "" : out.trim(); }
        public boolean ok() { return exit == 0; }
    }

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();

    public interface Callback { void onResult(Result r); }

    /** true si su répond et uid=0. */
    public static void check(Callback cb) {
        POOL.execute(() -> {
            Result r = run("id");
            cb.onResult(new Result(r.ok() && r.out.contains("uid=0") ? 0 : 1, r.out));
        });
    }

    /** Commande bloquante via su -c. */
    public static Result run(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader e = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            while ((line = e.readLine()) != null) sb.append(line).append('\n');
            int code = p.waitFor();
            return new Result(code, sb.toString());
        } catch (Exception ex) {
            return new Result(-1, String.valueOf(ex.getMessage()));
        }
    }

    /** Commande asynchrone. */
    public static void runAsync(String cmd, Callback cb) {
        POOL.execute(() -> cb.onResult(run(cmd)));
    }

    /** Lance un processus long (moteur) via su ; retourne le Process pour supervision. */
    public static Process startLong(String cmd) throws Exception {
        return Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
    }
}
