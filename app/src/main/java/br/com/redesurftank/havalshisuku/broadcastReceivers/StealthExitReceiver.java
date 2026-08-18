package br.com.redesurftank.havalshisuku.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import br.com.redesurftank.havalshisuku.managers.StealthModeManager;

/**
 * Porta de serviço do Modo Concessionária (rede de segurança).
 *
 * Com o modo ativo o ícone do launcher some, então a saída normal é a sequência do volante
 * (3 toques longos no botão 1). Se o volante falhar, este receiver reverte por telnet/adb:
 *
 *   am broadcast -a br.com.redesurftank.havalshisuku.STEALTH_EXIT
 */
public class StealthExitReceiver extends BroadcastReceiver {
    private static final String ACTION = "br.com.redesurftank.havalshisuku.STEALTH_EXIT";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w("StealthExitReceiver", "Received intent: " + (intent == null ? "null" : intent.getAction()));
        if (intent != null && ACTION.equals(intent.getAction())) {
            StealthModeManager.exit(context, "BROADCAST");
        }
    }
}
