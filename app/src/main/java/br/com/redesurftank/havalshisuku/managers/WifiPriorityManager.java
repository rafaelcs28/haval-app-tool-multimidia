package br.com.redesurftank.havalshisuku.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys;
import br.com.redesurftank.havalshisuku.utils.ShizukuUtils;

/**
 * Vigia de prioridade de WiFi: quando uma rede salva de prioridade MAIOR que a atual aparece no
 * alcance, força a troca pra ela (via {@link ServiceManager#switchWifiToNetwork(int)}). Só "SOBE"
 * de prioridade (nunca troca uma rede melhor por uma pior), com histerese temporal (a preferida tem
 * de aparecer estável por STABLE_MS) pra não flapear. Opt-in (pref WIFI_PRIORITY_ENABLED, default
 * OFF). Event-driven (scan/estado de rede) + poll leve de segurança. Roda em thread própria.
 *
 * Fail-safe: se não der pra ler scan/SSID (ex.: localização desligada -> getScanResults vazio), o
 * vigia simplesmente NÃO troca — nunca derruba o WiFi atrás de uma rede que não confirmou no alcance.
 */
public class WifiPriorityManager {
    private static final String TAG = "WifiPriorityManager";
    private static final long POLL_MS = 30000L;
    private static final long STABLE_MS = 20000L; // a candidata tem de persistir isto antes da troca
    private static final long SWITCH_COOLDOWN_MS = 15000L; // quieto após cada troca (a re-associação dispara eventos)

    private static volatile WifiPriorityManager INSTANCE;

    public static WifiPriorityManager getInstance() {
        if (INSTANCE == null) {
            synchronized (WifiPriorityManager.class) {
                if (INSTANCE == null) INSTANCE = new WifiPriorityManager();
            }
        }
        return INSTANCE;
    }

    private final SharedPreferences prefs;
    private final Handler bgHandler;
    private volatile boolean running = false;
    private volatile boolean switching = false;
    private String candidate = null;
    private long candidateSinceMs = 0L;
    private volatile long lastSwitchMs = 0L; // início do cooldown pós-troca
    private BroadcastReceiver receiver;

    private WifiPriorityManager() {
        prefs = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE);
        HandlerThread t = new HandlerThread("WifiPriorityThread");
        t.start();
        bgHandler = new Handler(t.getLooper());
    }

    private boolean isEnabled() {
        return prefs.getBoolean(SharedPreferencesKeys.WIFI_PRIORITY_ENABLED.getKey(), false);
    }

    /** Chamado no boot / quando o Shizuku fica pronto. */
    public void onServicesReady() {
        if (isEnabled()) start();
    }

    /** Chamado pelo toggle das Configurações (a UI persiste a pref antes). */
    public void setEnabled(boolean enabled) {
        if (enabled) start();
        else stop();
    }

    /** Liga/desliga o recurso E persiste a pref E avisa o EcoTrip. Ponto único usado pela UI e pelo
     *  comando remoto (ConnectivityStatusProvider.call), pra os dois lados ficarem em sincronia e o
     *  EcoTrip não ficar forçando o valor velho. */
    public void setFeatureEnabled(boolean enabled) {
        prefs.edit().putBoolean(SharedPreferencesKeys.WIFI_PRIORITY_ENABLED.getKey(), enabled).apply();
        setEnabled(enabled);
        try {
            ConnectivityStatusManager.INSTANCE.notifyChanged(App.getContext());
        } catch (Throwable ignored) {
        }
    }

    private void start() {
        bgHandler.post(() -> {
            if (running) return;
            running = true;
            ensureLocationOn();
            registerReceiver();
            bgHandler.removeCallbacks(tick);
            bgHandler.post(tick);
            Log.w(TAG, "WiFi priority enforcer started");
        });
    }

    // getScanResults() exige a LOCALIZAÇÃO ligada (Android 9); sem ela o scan volta vazio e o vigia
    // nunca troca. O shell do Shizuku (uid shell) tem WRITE_SECURE_SETTINGS -> liga em high-accuracy.
    private void ensureLocationOn() {
        try {
            ShizukuUtils.runCommandAndGetOutput(
                    new String[]{"settings", "put", "secure", "location_mode", "3"});
        } catch (Throwable t) {
            Log.e(TAG, "ensureLocationOn failed", t);
        }
    }

    private void stop() {
        bgHandler.post(() -> {
            running = false;
            unregisterReceiver();
            bgHandler.removeCallbacks(tick);
            candidate = null;
            candidateSinceMs = 0L;
            // desligar o recurso restaura o pool (desfaz o disableOthers do último switch), senão as
            // redes ficariam presas desabilitadas mesmo com o vigia off.
            try {
                ServiceManager.getInstance().enableAllSavedWifi();
            } catch (Throwable ignored) {
            }
            Log.w(TAG, "WiFi priority enforcer stopped");
        });
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running || !isEnabled()) {
                running = false;
                return;
            }
            try {
                enforceOnce();
            } catch (Throwable t) {
                Log.e(TAG, "enforce error", t);
            }
            bgHandler.postDelayed(this, POLL_MS);
        }
    };

    private void registerReceiver() {
        if (receiver != null) return;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                bgHandler.post(() -> {
                    if (running && isEnabled()) {
                        try {
                            enforceOnce();
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        try {
            App.getContext().registerReceiver(receiver, f);
        } catch (Throwable t) {
            Log.e(TAG, "registerReceiver failed", t);
        }
    }

    private void unregisterReceiver() {
        if (receiver != null) {
            try {
                App.getContext().unregisterReceiver(receiver);
            } catch (Throwable ignored) {
            }
            receiver = null;
        }
    }

    private List<String> priorityList() {
        List<String> out = new ArrayList<>();
        try {
            String json = prefs.getString(SharedPreferencesKeys.WIFI_PRIORITY_LIST.getKey(), "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                String s = arr.optString(i, "");
                if (!s.isEmpty()) out.add(s);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private void enforceOnce() {
        if (switching) return;
        // COOLDOWN pós-troca: depois de trocar, a re-associação do WiFi dispara NETWORK_STATE_CHANGED
        // (onLost/onAvailable), que re-chamava o enforceOnce NO MEIO da transição -> ele reagia ao
        // estado transitório e re-trocava = FLAPPING (confirmado no carro: vigia ON = flapa; vigia OFF
        // = o Android fica estável na preferida). Fica quieto ~15s após cada troca pra o WiFi assentar;
        // o Android é "sticky" (uma vez na preferida, ele fica), então só o nosso churn tirava de lá.
        if (SystemClock.elapsedRealtime() - lastSwitchMs < SWITCH_COOLDOWN_MS) return;
        List<String> prio = priorityList();
        if (prio.size() < 2) return; // nada a decidir com menos de 2 redes priorizadas
        WifiManager wm = (WifiManager) App.getContext().getSystemService(Context.WIFI_SERVICE);
        if (wm == null || !wm.isWifiEnabled()) return;
        try {
            wm.startScan();
        } catch (Throwable ignored) {
        }
        Set<String> visible = visibleSsids(wm);
        if (visible.isEmpty()) {
            // Scan vazio quase sempre = localização desligada. Liga (self-heal); o próximo ciclo já vê.
            ensureLocationOn();
            return; // sem confirmar redes no alcance, não arrisca trocar (fail-safe)
        }
        String current = currentSsid(wm);
        boolean disconnected = current == null;
        int curIdx = current != null ? prio.indexOf(current) : -1;
        if (curIdx < 0) curIdx = Integer.MAX_VALUE; // atual fora da lista/desconectado -> qualquer prioridade é upgrade
        // maior prioridade VISÍVEL com índice menor que o da rede atual
        String target = null;
        for (int i = 0; i < prio.size() && i < curIdx; i++) {
            if (visible.contains(prio.get(i))) {
                target = prio.get(i);
                break;
            }
        }
        if (target == null || target.equals(current)) {
            candidate = null;
            candidateSinceMs = 0L;
            return;
        }
        // histerese temporal SÓ quando já conectado (evita flapping entre redes). DESCONECTADO reconecta
        // na hora: como a troca anterior desabilitou o pool, esperar 20s deixaria o carro offline à toa.
        long now = SystemClock.elapsedRealtime();
        if (!disconnected) {
            if (!target.equals(candidate)) {
                candidate = target;
                candidateSinceMs = now;
                return;
            }
            if (now - candidateSinceMs < STABLE_MS) return;
        }
        int netId = netIdForSsid(target);
        if (netId < 0) return;
        candidate = null;
        candidateSinceMs = 0L;
        switching = true;
        try {
            Log.w(TAG, "Switching to higher-priority WiFi '" + target + "' (netId " + netId + ")");
            ServiceManager.getInstance().switchWifiToNetwork(netId);
        } finally {
            switching = false;
            lastSwitchMs = SystemClock.elapsedRealtime(); // inicia o cooldown (ignora os eventos da transição)
        }
    }

    /**
     * Diagnóstico + verificação IMEDIATA (sem histerese): reporta o que o vigia vê e decide, e troca
     * na hora se houver uma rede de prioridade MAIOR visível. Pro botão "Verificar agora". BLOCKING
     * (pode levar ~10s se trocar) -> chamar FORA da main thread.
     */
    public String forceCheckNow() {
      try {
        StringBuilder sb = new StringBuilder();
        Context ctx = App.getContext();
        if (ctx == null) return "Contexto indisponível.";
        WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        if (wm == null) return "WifiManager indisponível.";
        if (!wm.isWifiEnabled()) return "WiFi está desligado.";
        try {
            wm.startScan();
        } catch (Throwable ignored) {
        }
        List<String> prio = priorityList();
        Set<String> visible = visibleSsids(wm);
        String current = currentSsid(wm);
        sb.append("Conectado: ").append(current == null ? "(nenhuma)" : current).append("\n");
        sb.append("Prioridade: ").append(prio.isEmpty() ? "(vazia)" : prio.toString()).append("\n");
        sb.append("Visíveis: ")
                .append(visible.isEmpty() ? "(nenhuma)" : visible.toString())
                .append("\n");
        if (prio.size() < 2) {
            sb.append("=> precisa de 2+ redes na lista de prioridade.");
            return sb.toString();
        }
        if (visible.isEmpty()) {
            ensureLocationOn();
            sb.append("=> SCAN VAZIO (localização estava desligada?). Acabei de LIGAR a localização; espere ~10s e toque em \"Verificar agora\" de novo.");
            return sb.toString();
        }
        int curIdx = current != null ? prio.indexOf(current) : -1;
        if (curIdx < 0) curIdx = Integer.MAX_VALUE;
        String target = null;
        for (int i = 0; i < prio.size() && i < curIdx; i++) {
            if (visible.contains(prio.get(i))) {
                target = prio.get(i);
                break;
            }
        }
        if (target == null) {
            sb.append("=> nenhuma rede de prioridade MAIOR que a atual está visível. Nada a trocar.");
            return sb.toString();
        }
        if (target.equals(current)) {
            sb.append("=> já está na de maior prioridade visível.");
            return sb.toString();
        }
        int netId = netIdForSsid(target);
        if (netId < 0) {
            sb.append("=> '").append(target).append("' visível, mas sem netId salvo (rede salva?).");
            return sb.toString();
        }
        sb.append("=> trocando para '").append(target).append("' (netId ").append(netId).append(") — o WiFi pisca ~10s...\n");
        switching = true;
        boolean ok;
        try {
            ok = ServiceManager.getInstance().switchWifiToNetwork(netId);
        } finally {
            switching = false;
            lastSwitchMs = SystemClock.elapsedRealtime(); // inicia o cooldown do vigia
        }
        sb.append(ok ? "enableNetwork OK." : "enableNetwork FALHOU.");
        return sb.toString();
      } catch (Throwable t) {
        Log.e(TAG, "forceCheckNow crashed", t);
        try {
            br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger
                    .logText("wifi_diag_crash", Log.getStackTraceString(t));
        } catch (Throwable ignored) {
        }
        return "Erro no diagnostico: " + t + "\n(o trace foi pro log; me mande esta mensagem)";
      }
    }

    private Set<String> visibleSsids(WifiManager wm) {
        Set<String> out = new HashSet<>();
        try {
            List<ScanResult> results = wm.getScanResults();
            if (results != null) {
                for (ScanResult r : results) {
                    if (r.SSID != null && !r.SSID.isEmpty()) out.add(r.SSID);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private String currentSsid(WifiManager wm) {
        try {
            WifiInfo wi = wm.getConnectionInfo();
            if (wi != null) {
                String s = wi.getSSID();
                if (s != null) {
                    if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                        s = s.substring(1, s.length() - 1);
                    }
                    if (!s.isEmpty() && !s.equals("<unknown ssid>")) return s;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private int netIdForSsid(String ssid) {
        for (String e : ServiceManager.getInstance().listSavedWifi()) {
            int bar = e.indexOf('|');
            if (bar > 0 && e.substring(bar + 1).equals(ssid)) {
                try {
                    return Integer.parseInt(e.substring(0, bar));
                } catch (Throwable ignored) {
                }
            }
        }
        return -1;
    }
}
