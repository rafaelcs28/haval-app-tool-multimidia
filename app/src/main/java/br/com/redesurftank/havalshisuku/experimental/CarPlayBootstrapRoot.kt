package br.com.redesurftank.havalshisuku.experimental

import android.content.pm.PackageManager
import android.os.SystemClock
import android.os.ParcelFileDescriptor
import moe.shizuku.server.IShizukuService
import org.apache.commons.net.telnet.TelnetClient
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/** Local transports only. No remote host, credentials, downloads or vehicle commands. */
internal class CarPlayBootstrapRoot {
    fun run(command: String, timeoutMs: Long = 5_000): String {
        require(command.all { it.code < 128 }) { "Non-ASCII root command" }
        val tag = "cpb_" + UUID.randomUUID().toString().replace("-", "")
        val framed = """
            printf '\n${tag}_begin\n'
            ( $command )
            cpb_rc=§?
            printf '\n${tag}_end=%s\n' "§cpb_rc"
        """.trimIndent().replace('§', '$')
        // The console lowercases typed input. Octal framing preserves paths and -R.
        // Keep every terminal input line below canonical TTY limits, including
        // long identity/guard commands. Each chunk only contains octal digits.
        val wire = "{\n" + framed.toByteArray(StandardCharsets.US_ASCII).toList().chunked(512)
            .joinToString("") { chunk ->
                val octal = chunk.joinToString("") {
                    "\\" + (it.toInt() and 255).toString(8).padStart(3, '0')
                }
                "printf '%b' '$octal'\n"
            } + "} | sh\n"
        val client = TelnetClient()
        var executionStarted = false
        try {
            client.connectTimeout = 600
            client.connect("127.0.0.1", 23)
            executionStarted = true
            client.soTimeout = 200
            client.outputStream.write(wire.toByteArray(StandardCharsets.US_ASCII))
            client.outputStream.flush()
            val buffer = ByteArray(8192)
            val result = StringBuilder()
            val deadline = SystemClock.elapsedRealtime() + timeoutMs
            while (SystemClock.elapsedRealtime() < deadline) {
                try {
                    val count = client.inputStream.read(buffer)
                    if (count < 0) break
                    result.append(String(buffer, 0, count, StandardCharsets.UTF_8))
                    if (result.length > 1_048_576) error("Root response too large")
                    if (Regex("(?m)^${tag}_end=[0-9]+\r?\n").containsMatchIn(result)) return parse(result.toString(), tag)
                } catch (_: SocketTimeoutException) { /* bounded read */ }
            }
            error("Local root command timed out")
        } catch (error: Exception) {
            // The same local operation can use Shizuku when Telnet is unavailable.
            // Never retry a command whose execution already began: that could duplicate
            // an injector. Only a failed connection is eligible for the other transport.
            if (executionStarted) throw error
        } finally {
            runCatching { client.disconnect() }
        }
        check(Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            "Local root unavailable"
        }
        val service = checkNotNull(IShizukuService.Stub.asInterface(Shizuku.getBinder()))
        val process = checkNotNull(service.newProcess(arrayOf("sh", "-c", "{\n$framed\n} 2>&1"), null, null))
        var input: ParcelFileDescriptor.AutoCloseInputStream? = null
        try {
            // Close every remote pipe. Leaving copies behind exhausts the root service.
            process.outputStream?.close()
            process.errorStream?.close()
            val stream = ParcelFileDescriptor.AutoCloseInputStream(checkNotNull(process.inputStream))
            input = stream
            val bytes = ByteArrayOutputStream()
            val readFailure = AtomicReference<Throwable?>()
            val reader = Thread({
                try {
                    val chunk = ByteArray(8192)
                    while (true) {
                        val count = stream.read(chunk)
                        if (count < 0) break
                        check(bytes.size() + count <= 1_048_576) { "Root response too large" }
                        bytes.write(chunk, 0, count)
                    }
                } catch (error: Throwable) { readFailure.set(error) }
            }, "carplay-root-response").apply { isDaemon = true; start() }
            reader.join(timeoutMs)
            check(!reader.isAlive && readFailure.get() == null) { "Root response unavailable or timed out" }
            return parse(bytes.toString(StandardCharsets.UTF_8.name()), tag)
        } finally {
            runCatching { process.destroy() } // short shell only; detached owners remain alive
            runCatching { input?.close() }
        }
    }

    private fun parse(raw: String, tag: String): String {
        val text = raw.replace("\r", "")
        val start = text.indexOf("\n${tag}_begin\n")
        val end = Regex("(?m)^${tag}_end=([0-9]+)$").find(text)
        check(start >= 0 && end != null && end.range.first > start) { "Root response framing missing" }
        check(end.groupValues[1] == "0") { "Local root command failed" }
        return text.substring(start + tag.length + 8, end.range.first).trimEnd('\n')
    }

    companion object {
        fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
    }
}
