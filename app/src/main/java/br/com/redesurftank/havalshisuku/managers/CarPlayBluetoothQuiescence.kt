package br.com.redesurftank.havalshisuku.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.SystemClock
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/** Physical OEM Bluetooth observations only; never registers SDK callbacks or changes radio state. */
internal object CarPlayBluetoothQuiescence {
    private const val PACKAGE = "com.ts.car.bluetooth.service"
    private const val SERVICE = "com.ts.car.bluetooth.service.BluetoothService"
    private const val DESCRIPTOR = "ts.car.bluetooth.sdk.IBluetoothManager"
    private const val APK_SIZE = 2_871_765L
    private const val APK_SHA256 = "c99e49b47704e610c044ab01aa530ca451784e9e78885b11cad1c1f45518eac0"
    private const val MAX_DEVICES = 16
    private const val SETTLE_MS = 5_000L
    private const val MAX_READ_MS = 1_500L
    private data class Bound(val binder: IBinder, val epoch: Long, val at: Long)
    private data class Firmware(val epoch: Long, val path: String, val size: Long,
                                val modified: Long, val approved: Boolean)
    private val stateLock = Any()
    private val readLock = Any()
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var connection: ServiceConnection? = null
    private var bound: Bound? = null
    private var epoch = 0L
    private var lastBindAt = -SETTLE_MS
    private var firmware: Firmware? = null

    fun read(context: Context, retainedAddresses: Set<String>): Boolean? {
        if (Looper.myLooper() == Looper.getMainLooper()) return null
        if (retainedAddresses.isEmpty() || retainedAddresses.size > MAX_DEVICES ||
            retainedAddresses.any { RetainedBluetoothDevicePolicy.normalizeAddress(it) != it }) return null
        return synchronized(readLock) {
            requestBinding(context.applicationContext)
            val current = synchronized(stateLock) { bound } ?: return@synchronized null
            val started = SystemClock.elapsedRealtime()
            if (started - current.at < SETTLE_MS || !verifyFirmware(context, current.epoch)) return@synchronized null
            val observed = readDisconnected(current.binder, retainedAddresses)
            val ended = SystemClock.elapsedRealtime()
            synchronized(stateLock) {
                if (bound !== current || !current.binder.isBinderAlive || ended < started ||
                    ended - started > MAX_READ_MS) null else observed
            }
        }
    }

    private fun readDisconnected(binder: IBinder, retained: Set<String>): Boolean? {
        val state = adapterState(binder) ?: return null
        if (state == 10) return if (adapterState(binder) == 10) true else null
        if (state != 12) return null
        val before = devices(binder, 12) ?: return null
        val addresses = before + retained
        if (addresses.isEmpty() || addresses.size > MAX_DEVICES) return null
        // -1 aggregates all connection managers. Empty is supplementary, never readiness proof.
        val connected = devices(binder, 13, -1) ?: return null
        if (connected.isNotEmpty()) return false
        for (address in addresses) {
            // Exact backend has five real proxies; PAN index 5 is unsupported by TX14.
            for (profile in 0..4) {
                val stateAtProfile = call(binder, 14, 1, {
                    it.writeString(address); it.writeInt(profile)
                }) { it.readInt() } ?: return null
                when (RetainedBluetoothDevicePolicy.profileAbsence(stateAtProfile)) {
                    true -> Unit
                    false -> return false
                    null -> return null
                }
            }
        }
        val after = devices(binder, 12) ?: return null
        if (before != after) return null
        if (devices(binder, 13, -1)?.isEmpty() != true) return null
        return if (adapterState(binder) == 12) true else null
    }

    private fun adapterState(binder: IBinder): Int? =
        // Profiled server deliberately leaves a fresh CallResult at INVALID_CODE=0 here only.
        call(binder, 4, 0, {}) { it.readInt() }

    private fun devices(binder: IBinder, code: Int, profile: Int? = null): Set<String>? =
        call(binder, code, 1, { parcel -> profile?.let { parcel.writeInt(it) } }) { reply ->
            val count = reply.readInt()
            if (count !in 0..MAX_DEVICES) return@call null
            val addresses = LinkedHashSet<String>()
            repeat(count) {
                if (reply.readInt() != 1) return@call null
                val address = RetainedBluetoothDevicePolicy.normalizeAddress(reply.readString()) ?: return@call null
                reply.readString() // Device name discarded; no identifiers are logged or published.
                repeat(3) { reply.readInt() } // deviceClass, features, enabledFeatures
                if (!addresses.add(address)) return@call null
            }
            addresses
        }

    private fun <T> call(binder: IBinder, code: Int, expectedResult: Int,
                         encode: (Parcel) -> Unit, decode: (Parcel) -> T?): T? {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESCRIPTOR)
            encode(data)
            if (!binder.transact(code, data, reply, 0)) return null
            reply.readException()
            val value = decode(reply) ?: return null
            if (reply.readInt() != 1) return null
            val result = reply.readInt()
            val message = reply.readString() // Never log it; it may contain device details.
            if (expectedResult == 0 && message != null) return null
            if (result == expectedResult && reply.dataAvail() == 0) value else null
        } catch (_: Exception) {
            null
        } finally {
            reply.recycle(); data.recycle()
        }
    }

    private fun requestBinding(context: Context) {
        val candidate: ServiceConnection
        synchronized(stateLock) {
            val now = SystemClock.elapsedRealtime()
            if (connection != null || now - lastBindAt < SETTLE_MS) return
            lastBindAt = now
            candidate = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    synchronized(stateLock) {
                        if (connection !== this) return
                        bound = binder?.takeIf { it.isBinderAlive && name != null &&
                            name.packageName == PACKAGE && name.className == SERVICE }?.let {
                            Bound(it, ++epoch, SystemClock.elapsedRealtime())
                        }
                    }
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    synchronized(stateLock) { if (connection === this) bound = null }
                }
                override fun onBindingDied(name: ComponentName?) = releaseBinding(context, this)
                override fun onNullBinding(name: ComponentName?) = releaseBinding(context, this)
            }
            connection = candidate
        }
        if (!handler.post {
                if (synchronized(stateLock) { connection === candidate }) {
                    val success = runCatching {
                        context.bindService(Intent(DESCRIPTOR).setComponent(ComponentName(PACKAGE, SERVICE)), candidate, 0)
                    }.getOrDefault(false)
                    if (!success) releaseBinding(context, candidate)
                }
            }) {
            synchronized(stateLock) { if (connection === candidate) { connection = null; bound = null } }
        }
    }

    private fun releaseBinding(context: Context, candidate: ServiceConnection) {
        synchronized(stateLock) {
            if (connection !== candidate) return
            connection = null; bound = null
        }
        runCatching { context.unbindService(candidate) }
    }

    private fun verifyFirmware(context: Context, bindingEpoch: Long): Boolean = try {
        val info = context.packageManager.getServiceInfo(ComponentName(PACKAGE, SERVICE), 0)
        check(info.packageName == PACKAGE && info.name == SERVICE)
        val file = File(info.applicationInfo.sourceDir)
        check(file.isFile && file.length() == APK_SIZE)
        val path = file.canonicalPath
        val size = file.length()
        val modified = file.lastModified()
        val previous = firmware
        if (previous != null && previous.epoch == bindingEpoch && previous.path == path &&
            previous.size == size && previous.modified == modified) previous.approved
        else {
            val hash = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val bytes = ByteArray(16_384)
                while (true) { val count = stream.read(bytes); if (count < 0) break; hash.update(bytes, 0, count) }
            }
            val approved = hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) } == APK_SHA256 &&
                file.length() == size && file.lastModified() == modified
            firmware = Firmware(bindingEpoch, path, size, modified, approved)
            approved
        }
    } catch (_: Exception) {
        firmware = null
        false
    }
}

/** Pure admission signature. A candidate still requires the physical Bluetooth proof above. */
internal object RetainedBluetoothDevicePolicy {
    data class Facts(val type: Int, val accepted: Int, val state: Int,
                     val connections: Map<Int, Int>, val available: Int, val active: Int,
                     val activeStates: Map<Int, Int>, val address: String?, val usbDeviceMode: Boolean,
                     val transportNodes: List<String?>)

    fun normalizeAddress(value: String?): String? = value?.uppercase(Locale.ROOT)?.takeIf {
        it.matches(Regex("[0-9A-F]{2}(:[0-9A-F]{2}){5}")) && it != "00:00:00:00:00:00" && it != "FF:FF:FF:FF:FF:FF"
    }

    fun addressIfCandidate(facts: Facts): String? {
        // Profiled DCM promotes paired BT READY to global ONLINE (1) and can
        // retain it after disconnect. Neither 0 nor 1 proves physical absence.
        if (facts.type !in setOf(1, 2) || facts.accepted != 1 || facts.state !in 0..1 ||
            facts.connections != mapOf(2 to 3) || facts.available != 0 || facts.active != 0 ||
            facts.activeStates.isNotEmpty() || facts.usbDeviceMode ||
            facts.transportNodes.size != 3 || facts.transportNodes.any { !it.isNullOrEmpty() }) return null
        return normalizeAddress(facts.address)
    }

    fun profileAbsence(state: Int): Boolean? = when (state) {
        0 -> true
        1, 2, 3 -> false
        else -> null
    }
}
