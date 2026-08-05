package br.com.redesurftank.havalshisuku.managers

import br.com.redesurftank.havalshisuku.utils.ShizukuUtils
import java.net.NetworkInterface
import java.util.Collections

private data class IfRow(
        val name: String,
        val up: Boolean,
        val v4: List<String>,
        val usb: Boolean,
        val tipo: String
)

/**
 * Diagnóstico de rede (SÓ LEITURA — não muda nada). Descobre POR ONDE a multimídia tem internet e,
 * principalmente, se com o celular no cabo USB (CarPlay ou Android Auto) aparece uma interface de
 * rede USB (rndis/ncm/usb/eth) COM internet — o que abriria caminho pra "puxar o 4G do celular pelo
 * cabo" (roteando por ela, estilo HotRouter). Faz pings de ~2s: chamar FORA da main thread.
 */
object NetworkDiagnostics {

    // Rede-por-USB do CELULAR. "eth" NAO entra: neste head unit eth0 e ethernet INTERNO (SoC/TBOX).
    private val USB_HINTS = listOf("rndis", "ncm", "usb")
    private val WIFI_HINTS = listOf("wlan", "swlan", "ap0", "p2p")
    private val WWAN_HINTS = listOf("rmnet", "ccmni", "pdp", "wwan")

    fun run(): String {
        val sb = StringBuilder()
        val rows = interfaces()

        // 1) interfaces (API, sem root)
        sb.append("== Interfaces ==\n")
        val visiveis = rows.filter { it.name != "lo" && (it.up || it.v4.isNotEmpty()) }
        if (visiveis.isEmpty()) sb.append("(nenhuma além de loopback)\n")
        for (i in visiveis) {
            sb.append("  ").append(i.name).append("  ").append(if (i.up) "UP" else "down").append("  ")
            sb.append(if (i.v4.isEmpty()) "(sem IPv4)" else i.v4.joinToString(","))
            if (i.tipo.isNotEmpty()) sb.append("  [").append(i.tipo).append("]")
            sb.append("\n")
        }

        // 2) rotas (precisa Shizuku)
        val rotas = sh("ip route")
        val routeGet = sh("ip route get 8.8.8.8")
        val shizukuOff = rotas.isBlank() && routeGet.isBlank()
        sb.append("\n== Rotas ==\n")
        if (shizukuOff) {
            sb.append("(sem saída — Shizuku desligado; só o teste de internet 'geral' vale)\n")
        } else {
            if (rotas.isNotBlank()) sb.append(rotas.trim()).append("\n")
            if (routeGet.isNotBlank()) sb.append("→ ").append(routeGet.trim().replace("\n", " ")).append("\n")
        }
        val getIface = Regex("""\bdev\s+(\S+)""").find(routeGet)?.groupValues?.getOrNull(1)

        // 3) internet agora + DNS
        sb.append("\n== Internet ==\n")
        val netNow = ping(null)
        sb.append("  internet (rota atual): ").append(veredito(netNow)).append("\n")
        val dns = sh("ping -c 1 -W 2 google.com 2>&1").contains("0% packet loss")
        sb.append("  DNS (google.com): ").append(if (dns) "OK ✅" else "falhou ❌").append("\n")

        // 4) USB do CELULAR (nome usb/rndis/ncm). Só carrega internet se tiver IP (sem IP = não roteia).
        //    NÃO uso mais ping por -I aqui: sem root ele cai na rota padrão e dá FALSO-POSITIVO (dizia
        //    "OK" até pra interface SEM IP). O sinal confiável é a ROTA REAL do 8.8.8.8 (dev, abaixo).
        val usbRows = rows.filter { it.usb }
        if (usbRows.isNotEmpty()) {
            sb.append("\n== USB (rede do celular pelo cabo) ==\n")
            for (i in usbRows) {
                sb.append("  ").append(i.name).append(": ")
                sb.append(if (i.v4.isEmpty()) "UP mas SEM IP (não roteia internet)" else "IP " + i.v4.joinToString(","))
                sb.append("\n")
            }
        }

        // 5) veredito — dirigido pela ROTA do 8.8.8.8 (qual 'dev' a internet usa), confiável sem root
        sb.append("\n== 4G do celular pelo cabo? ==\n")
        val usbNames = usbRows.map { it.name }
        val usbComIp = usbRows.filter { it.v4.isNotEmpty() }.map { it.name }
        val rotaEhUsb = getIface != null && USB_HINTS.any { getIface.startsWith(it) }
        sb.append(
                when {
                    rotaEhUsb ->
                            "SIM ✅ a internet está saindo por " + getIface + " (cabo do celular).\n" +
                                    "=> dá pra fixar/rotear a multimídia por ela (opt-in, estilo HotRouter)."
                    usbNames.isEmpty() ->
                            "NÃO apareceu interface USB do celular (rndis/ncm/usb). No cabo, o celular está\n" +
                                    "só em projeção/carga. A internet sai por " + (getIface ?: "?") + "."
                    usbComIp.isEmpty() ->
                            "Interface USB apareceu (" + usbNames.joinToString(",") + ") mas SEM IP → o celular\n" +
                                    "NÃO entregou internet pelo cabo. A internet sai por " + (getIface ?: "?") + " (rede\n" +
                                    "INTERNA do carro), não pelo celular. Ligue o compartilhamento no celular e re-teste."
                    else ->
                            "Há USB COM IP (" + usbComIp.joinToString(",") + ") mas a internet ainda sai por " +
                                    (getIface ?: "?") + ".\nPra confirmar: DESLIGUE o WiFi do carro e re-teste — se a rota do\n" +
                                    "8.8.8.8 passar pra a interface USB, é isso."
                }
        )
        return sb.toString()
    }

    private fun interfaces(): List<IfRow> {
        val list =
                try {
                    Collections.list(NetworkInterface.getNetworkInterfaces())
                } catch (t: Throwable) {
                    emptyList<NetworkInterface>()
                }
        return list.map { ni ->
                    val name = ni.name ?: "?"
                    val up = try { ni.isUp } catch (t: Throwable) { false }
                    val v4 =
                            try {
                                Collections.list(ni.inetAddresses)
                                        .filter { !it.isLoopbackAddress && (it.address?.size == 4) }
                                        .mapNotNull { it.hostAddress }
                            } catch (t: Throwable) {
                                emptyList()
                            }
                    val usb = name != "lo" && USB_HINTS.any { name.startsWith(it) }
                    val tipo =
                            when {
                                usb -> "USB?"
                                name.startsWith("eth") -> "eth-interno"
                                WIFI_HINTS.any { name.startsWith(it) } -> "wifi"
                                WWAN_HINTS.any { name.startsWith(it) } -> "4G-carro"
                                name.startsWith("vlan") || name.startsWith("vt") -> "interno"
                                name.startsWith("dummy") -> "dummy"
                                name == "lo" -> "loop"
                                else -> ""
                            }
                    IfRow(name, up, v4, usb, tipo)
                }
                .sortedBy { it.name }
    }

    private fun veredito(ok: Boolean) = if (ok) "OK ✅" else "sem internet ❌"

    private fun sh(cmd: String): String =
            try {
                ShizukuUtils.runCommandAndGetOutput(arrayOf("sh", "-c", cmd)) ?: ""
            } catch (t: Throwable) {
                ""
            }

    /** 1 pacote por [iface] (SO_BINDTODEVICE) ou pela rota atual se null, pra 8.8.8.8. true se respondeu. */
    private fun ping(iface: String?): Boolean {
        val bind = if (iface != null) "-I $iface " else ""
        val out = sh("ping ${bind}-c 1 -W 2 8.8.8.8 2>&1")
        return out.contains("0% packet loss") || out.contains(" 1 received")
    }
}
