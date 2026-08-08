package android.net.wifi;

/**
 * Stub PARCIAL do IWifiManager do framework (Android 9 / API 28).
 *
 * Cada método é pinado pelo código de transação EXATO do AOSP android-9.0.0 (mesmo truque do
 * IConnectivityManager.aidl daqui, que pina startTethering=23/stopTethering=24). Assim a gente
 * NÃO precisa replicar a interface inteira em ordem — e, principalmente, NÃO declaramos os
 * vizinhos perigosos (removeNetwork=15, disableNetwork=17) pra não haver risco de chamar por engano.
 *
 * Índices (0-based) conferidos no IWifiManager.aidl do android-9.0.0:
 *   15 = removeNetwork(int, String)              <- NÃO declarar (apaga a rede salva)
 *   16 = enableNetwork(int, boolean, String)     <- ação: seleciona/força a rede
 *   17 = disableNetwork(int, String)             <- NÃO declarar
 *   25 = getWifiEnabledState()                   <- leitura de calibração (esperado 3 = ENABLED)
 *
 * {@hide}
 */
interface IWifiManager {
    boolean enableNetwork(int netId, boolean disableOthers, String packageName) = 16;
    int getWifiEnabledState() = 25;
}
