package br.com.redesurftank.havalshisuku.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import br.com.redesurftank.havalshisuku.services.ForegroundService;

public class RestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, ForegroundService.class);
        // Encaminha a action (ex.: ACTION_SHIZUKU_RESTART do toque na notificação) pro serviço, pra
        // ela ser tratada ANTES do guard isServiceRunning. Sem action = restart normal (AlarmManager).
        if (intent != null && intent.getAction() != null) {
            serviceIntent.setAction(intent.getAction());
        }
        // startForegroundService garante a promoção correta a foreground mesmo com o processo morto
        // (evita o crash de "did not call startForeground" que getService direto poderia causar).
        context.startForegroundService(serviceIntent);
    }
}