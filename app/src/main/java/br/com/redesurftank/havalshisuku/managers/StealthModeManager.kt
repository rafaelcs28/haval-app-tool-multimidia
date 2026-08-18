package br.com.redesurftank.havalshisuku.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.SplashActivity
import br.com.redesurftank.havalshisuku.ambientlight.AmbientLightService
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys
import br.com.redesurftank.havalshisuku.services.BottomBarService
import br.com.redesurftank.havalshisuku.utils.ShizukuUtils

/**
 * MODO CONCESSIONÁRIA — devolve o carro ao comportamento de fábrica antes de levar à revisão.
 *
 * ARQUITETURA: MASTER SWITCH, não salvar/restaurar preferências.
 * Entrar no modo NÃO altera nenhuma preferência do usuário (a única chave escrita é
 * [SharedPreferencesKeys.STEALTH_MODE_ACTIVE]). Os pontos de ação espalhados pelo app consultam
 * [isActive] e se calam sozinhos. Sair = desligar a flag e reaplicar o estado normal a partir das
 * MESMAS preferências que o boot lê. Assim nenhuma configuração pode se perder numa falha de
 * restauração.
 *
 * VOLTA: o ícone do launcher some, então não há como abrir o app. As portas de volta são:
 *  1. 3 toques LONGOS no botão 1 do volante em até 8s (ServiceManager.dispatchKeyEvent);
 *  2. `am broadcast -a br.com.redesurftank.havalshisuku.STEALTH_EXIT` (StealthExitReceiver),
 *     acessível por telnet/adb como rede de segurança.
 *
 * Toda etapa é isolada em try/catch: uma etapa que falhe NUNCA pode impedir as outras — sobretudo
 * no [exit], que é o único caminho de recuperação.
 */
object StealthModeManager {
    private const val TAG = "StealthModeManager"
    private const val PREFS_NAME = "haval_prefs"
    /** Onde guardamos a lista do que escondemos, para saber o que devolver na volta. */
    private const val HIDDEN_PACKAGES_KEY = "stealthHiddenPackages"

    /**
     * Nunca esconder, mesmo sendo apps de terceiros:
     *  - o Shizuku é a fonte do privilégio que executa o `pm unhide`; escondê-lo tranca a porta
     *    por dentro e a volta passa a exigir telnet;
     *  - o próprio Impulse precisa continuar rodando para detectar a sequência do volante (o
     *    ícone dele já é tratado à parte, desabilitando a activity do launcher).
     */
    private val NEVER_HIDE = setOf(
        "moe.shizuku.privileged.api",
        "br.com.redesurftank.havalshisuku"
    )

    private fun prefs() =
        App.getDeviceProtectedContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Consultado pelos pontos de ação (gate das automações, boot, projetores). Lê a pref direto —
     * device-protected storage, igual aos outros managers — pra funcionar antes do unlock do usuário.
     */
    @JvmStatic
    fun isActive(): Boolean =
        try {
            prefs().getBoolean(SharedPreferencesKeys.STEALTH_MODE_ACTIVE.key, false)
        } catch (t: Throwable) {
            Log.e(TAG, "Falha lendo a flag do Modo Concessionária; assumindo desligado", t)
            false
        }

    /** Grava a flag com commit(): é o estado que define se o app tem volta, não pode ficar em cache. */
    private fun setActive(active: Boolean) {
        prefs().edit().putBoolean(SharedPreferencesKeys.STEALTH_MODE_ACTIVE.key, active).commit()
    }

    // ---------------------------------------------------------------------------------------
    // Entrada / saída
    // ---------------------------------------------------------------------------------------

    @JvmStatic
    /**
     * Esconde os ícones dos apps INSTALADOS pelo dono, deixando só os nativos do sistema.
     *
     * `pm list packages -3` é exatamente a distinção pedida: lista apenas o que não veio de
     * fábrica. `pm hide` some com o app do launcher e da gaveta sem desinstalar nem apagar dados —
     * é reversível com `pm unhide`. A lista do que foi escondido é gravada em preferência ANTES de
     * esconder: se o processo morrer no meio, a volta ainda sabe o que devolver.
     */
    private fun hideThirdPartyApps() {
        val raw = ShizukuUtils.runCommandAndGetOutput(arrayOf("pm", "list", "packages", "-3"))
        val packages = raw.lineSequence()
            .map { it.trim().removePrefix("package:").trim() }
            .filter { it.isNotEmpty() && it !in NEVER_HIDE }
            .distinct()
            .toList()
        if (packages.isEmpty()) {
            Log.w(TAG, "Nenhum app de terceiros para esconder")
            return
        }
        // Grava primeiro: um kill no meio do laço não pode deixar apps escondidos sem registro.
        prefs().edit().putString(HIDDEN_PACKAGES_KEY, packages.joinToString(",")).commit()
        var hidden = 0
        for (pkg in packages) {
            try {
                ShizukuUtils.runCommandAndGetOutput(arrayOf("pm", "hide", pkg))
                hidden++
            } catch (t: Throwable) {
                Log.e(TAG, "Falha ao esconder $pkg", t)
            }
        }
        Log.w(TAG, "Apps de terceiros escondidos: $hidden/${packages.size}")
        ClusterPersistentEventLogger.log(
            "stealth_hide_apps",
            mapOf("requested" to packages.size, "hidden" to hidden)
        )
    }

    /** Devolve os apps escondidos por [hideThirdPartyApps]. Best-effort, um a um. */
    private fun restoreThirdPartyApps() {
        val stored = prefs().getString(HIDDEN_PACKAGES_KEY, "").orEmpty()
        if (stored.isBlank()) return
        val packages = stored.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        var restored = 0
        for (pkg in packages) {
            try {
                ShizukuUtils.runCommandAndGetOutput(arrayOf("pm", "unhide", pkg))
                restored++
            } catch (t: Throwable) {
                Log.e(TAG, "Falha ao restaurar $pkg", t)
            }
        }
        // Só limpa o registro depois de tentar todos — se algo falhou, uma nova saída retenta.
        prefs().edit().remove(HIDDEN_PACKAGES_KEY).commit()
        Log.w(TAG, "Apps de terceiros restaurados: $restored/${packages.size}")
        ClusterPersistentEventLogger.log(
            "stealth_restore_apps",
            mapOf("requested" to packages.size, "restored" to restored)
        )
    }

    fun enter(context: Context, reason: String) {
        val appContext = context.applicationContext
        Log.w(TAG, "Entrando no Modo Concessionária (reason=$reason)")

        step("set_flag") { setActive(true) }
        step("hide_launcher_icon") { setLauncherIconEnabled(appContext, false) }
        step("hide_third_party_apps") { hideThirdPartyApps() }
        // Painel volta ao NATIVO: derruba as Presentations do cluster (máscara/tema + HUD).
        // Presentation.dismiss() exige a UI thread.
        onMain { step("stop_projectors") { ProjectorManager.getInstance().stopProjectors() } }
        // A barra inferior e o overlay flutuante de CPU/RAM são desenhados pelo MESMO serviço
        // (BottomBarService); o onDestroy dele já devolve o `wm overscan` para 0,0,0,0.
        step("stop_bottom_bar") {
            appContext.stopService(Intent(appContext, BottomBarService::class.java))
        }
        step("stop_ambient_light") { AmbientLightService.stop(appContext) }
        // Cancela os alarmes de brilho automático (não mexe na pref do usuário).
        step("cancel_auto_brightness") { AutoBrightnessManager.getInstance().setEnabled(false) }

        // Desmontagem dos patches: shell via Shizuku, nunca na thread chamadora (a UI).
        background("unmount_patches") {
            step("unmount_android_auto") { AndroidAutoPatchManager.removeMounts() }
            step("unmount_carplay") { CarPlayPatchManager.removeMounts() }
        }

        step("log") {
            ClusterPersistentEventLogger.log("stealth_mode_enter", mapOf("reason" to reason))
        }
        toast(appContext, "Modo Concessionária ativo — 3 toques no botão 1 e 3 no botão 2 do volante para voltar")
    }

    @JvmStatic
    fun exit(context: Context, reason: String) {
        val appContext = context.applicationContext
        Log.w(TAG, "Saindo do Modo Concessionária (reason=$reason)")

        // A flag cai PRIMEIRO: tudo que é reaplicado abaixo se auto-gateia por isActive().
        step("clear_flag") { setActive(false) }
        step("show_launcher_icon") { setLauncherIconEnabled(appContext, true) }
        step("restore_third_party_apps") { restoreThirdPartyApps() }
        onMain { step("restart_projectors") { ProjectorManager.getInstance().refresh() } }
        step("restart_bottom_bar") { restartBottomBarLikeBoot(appContext) }
        step("restart_ambient_light") { AmbientLightService.startIfEnabled(appContext) }
        // updateSchedule() já retorna cedo se a pref do usuário estiver desligada.
        step("restore_auto_brightness") { AutoBrightnessManager.getInstance().updateSchedule() }

        background("remount_patches") {
            // Mesma rotina e mesmo gate por pref que o ForegroundService usa no boot.
            step("remount_android_auto") {
                if (prefs().getBoolean(SharedPreferencesKeys.AA_PATCH_AUTO_MOUNT.key, false)) {
                    AndroidAutoPatchManager.ensureMounted()
                }
            }
            step("remount_carplay") {
                if (prefs().getBoolean(SharedPreferencesKeys.CARPLAY_PATCH_AUTO_MOUNT.key, true)) {
                    CarPlayPatchManager.ensureMounted()
                }
            }
        }

        step("log") {
            ClusterPersistentEventLogger.log("stealth_mode_exit", mapOf("reason" to reason))
        }
        toast(appContext, "Impulse reativado")
    }

    // ---------------------------------------------------------------------------------------
    // Etapas
    // ---------------------------------------------------------------------------------------

    /**
     * Some/volta com o ícone do launcher. SplashActivity é quem carrega o intent-filter LAUNCHER
     * (ver AndroidManifest.xml); DONT_KILL_APP mantém o processo vivo — é ele que ainda escuta o
     * volante pra poder desfazer isso.
     */
    private fun setLauncherIconEnabled(context: Context, enabled: Boolean) {
        val component = ComponentName(context, SplashActivity::class.java)
        val state =
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        context.packageManager.setComponentEnabledSetting(
            component,
            state,
            PackageManager.DONT_KILL_APP
        )
        Log.w(TAG, "Ícone do launcher " + (if (enabled) "reativado" else "escondido"))
    }

    /** Mesmo caminho do BottomBarBootReceiver: sobe o serviço e reaplica o overscan salvo. */
    private fun restartBottomBarLikeBoot(context: Context) {
        val p = prefs()
        if (!p.getBoolean(SharedPreferencesKeys.PERSISTENT_BOTTOM_BAR.key, false)) {
            Log.d(TAG, "Barra inferior desligada nas preferências; nada a religar")
            return
        }
        if (!android.provider.Settings.canDrawOverlays(context)) {
            Log.e(TAG, "Sem permissão de overlay; não dá pra religar a barra inferior")
            return
        }
        context.startService(Intent(context, BottomBarService::class.java))
        val overscan = p.getInt(SharedPreferencesKeys.PERSISTENT_BOTTOM_BAR_OVERSCAN.key, 20)
        background("reapply_overscan") {
            ShizukuUtils.runCommandAndGetOutput(arrayOf("wm", "overscan", "0,0,0,$overscan"))
        }
    }

    // ---------------------------------------------------------------------------------------
    // Utilitários — nenhuma etapa pode derrubar as seguintes
    // ---------------------------------------------------------------------------------------

    private inline fun step(name: String, body: () -> Unit) {
        try {
            body()
        } catch (t: Throwable) {
            Log.e(TAG, "Etapa '$name' do Modo Concessionária falhou (seguindo adiante)", t)
            try {
                ClusterPersistentEventLogger.log(
                    "stealth_mode_step_failed",
                    mapOf("step" to name, "error" to t.toString())
                )
            } catch (ignored: Throwable) {
            }
        }
    }

    private fun onMain(body: () -> Unit) {
        try {
            Handler(Looper.getMainLooper()).post(body)
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao postar etapa na UI thread", t)
        }
    }

    private fun background(name: String, body: () -> Unit) {
        try {
            Thread({ step(name, body) }, "StealthMode-$name").start()
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao iniciar a thread da etapa '$name'", t)
        }
    }

    private fun toast(context: Context, message: String) {
        onMain {
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (t: Throwable) {
                Log.e(TAG, "Falha ao exibir o toast do Modo Concessionária", t)
            }
        }
    }
}
