package br.com.redesurftank.havalshisuku.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.SystemClock
import ts.car.dcm.common.data.ConnDevice

/**
 * Read-only absence evidence for the automatic CarPlay bootstrap, independent of AA recovery.
 *
 * true means sustained fresh empty DCM replies; false means a device/projection was observed;
 * null means unavailable, malformed, changing, or still settling. Even a Bluetooth-only phone
 * blocks this conservative gate: preparation can wait until phones are fully disconnected.
 * This is a settling policy, not a claim that the OEM exposes an initialization-ready flag.
 */
object CarPlayBridgeQuiescence {
    private const val DCM_PACKAGE = "com.ts.car.dcm.dccontroller"
    private const val DCM_ACTION = "com.ts.car.dcm.dccontroller.connectionmanagerservice.CmService"
    private const val DCM_INTERFACE = "ts.car.dcm.common.aidl.IConnectionManagementService"
    private const val GET_ONLINE_DEVICES = 14
    private const val GET_ACTIVE_PROJECTIONS = 45
    private const val BIND_RETRY_MS = 5_000L

    private data class BoundService(val binder: IBinder, val epoch: Long, val boundAtMs: Long)

    private val stateLock = Any()
    private val readLock = Any()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val window = CarPlayBridgeQuiescenceWindow()
    private var connection: ServiceConnection? = null
    private var boundService: BoundService? = null
    private var nextEpoch = 0L
    private var lastBindAttemptMs = -BIND_RETRY_MS

    /** Call only from the background publisher; binding is posted without waiting for it. */
    fun read(context: Context): Boolean? {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            synchronized(stateLock) { window.reset() }
            return null
        }
        return synchronized(readLock) {
            requestBinding(context.applicationContext)
            val service = synchronized(stateLock) { boundService } ?: return@synchronized null
            val startedAt = SystemClock.elapsedRealtime()
            val onlineEmpty = readOnlineDevicesEmpty(service.binder)
            val activeEmpty = readActiveProjectionsEmpty(service.binder)
            val completedAt = SystemClock.elapsedRealtime()
            val observed = when {
                onlineEmpty == false || activeEmpty == false -> false
                onlineEmpty == true && activeEmpty == true -> true
                else -> null
            }
            synchronized(stateLock) {
                if (boundService !== service || !service.binder.isBinderAlive) {
                    window.reset()
                    null
                } else {
                    // Long/blocking transactions cannot contribute stale absence evidence.
                    val fresh = completedAt >= startedAt &&
                        completedAt - startedAt <= CarPlayBridgeQuiescenceWindow.MAX_SAMPLE_GAP_MS
                    window.observe(service.epoch, service.boundAtMs, completedAt,
                        if (fresh || observed == false) observed else null)
                }
            }
        }
    }

    private fun requestBinding(context: Context) {
        val candidate: ServiceConnection
        synchronized(stateLock) {
            val now = SystemClock.elapsedRealtime()
            if (connection != null || now - lastBindAttemptMs < BIND_RETRY_MS) return
            lastBindAttemptMs = now
            candidate = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    synchronized(stateLock) {
                        if (connection !== this) return
                        window.reset()
                        boundService = binder?.takeIf { it.isBinderAlive }?.let {
                            BoundService(it, ++nextEpoch, SystemClock.elapsedRealtime())
                        }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    synchronized(stateLock) {
                        if (connection !== this) return
                        boundService = null
                        window.reset()
                    }
                    // Keep the binding: Android reconnects this ServiceConnection.
                }

                override fun onBindingDied(name: ComponentName?) = releaseBinding(context, this)
                override fun onNullBinding(name: ComponentName?) = releaseBinding(context, this)
            }
            connection = candidate
        }
        if (!mainHandler.post {
                val stillWanted = synchronized(stateLock) { connection === candidate }
                if (stillWanted) {
                    val didBind = try {
                        context.bindService(Intent(DCM_ACTION).setPackage(DCM_PACKAGE), candidate,
                            Context.BIND_AUTO_CREATE)
                    } catch (_: Exception) {
                        false
                    }
                    if (!didBind) releaseBinding(context, candidate)
                }
            }) {
            synchronized(stateLock) {
                if (connection === candidate) {
                    connection = null
                    boundService = null
                    window.reset()
                }
            }
        }
    }

    private fun releaseBinding(context: Context, candidate: ServiceConnection) {
        synchronized(stateLock) {
            if (connection !== candidate) return
            connection = null
            boundService = null
            window.reset()
        }
        runCatching { context.unbindService(candidate) }
    }

    private fun readOnlineDevicesEmpty(binder: IBinder): Boolean? =
        transactRead(binder, GET_ONLINE_DEVICES) { reply ->
            // AIDL's out-Bundle presence marker must be present, not a null/default reply.
            if (reply.readInt() != 1) return@transactRead null
            val devices = Bundle.CREATOR.createFromParcel(reply)
            devices.classLoader = ConnDevice::class.java.classLoader
            val keys = devices.keySet()
            // Force complete unparcelling, including every value, before trusting emptiness.
            if (keys.any { devices.get(it) !is ConnDevice }) return@transactRead null
            keys.isEmpty()
        }

    private fun readActiveProjectionsEmpty(binder: IBinder): Boolean? =
        transactRead(binder, GET_ACTIVE_PROJECTIONS) { reply ->
            // Captured firmware Proxy: readException(), createTypedArrayList(ConnDevice.CREATOR).
            val devices = reply.createTypedArrayList(ConnDevice.CREATOR)
                ?: return@transactRead null
            if (devices.any { it == null }) return@transactRead null
            devices.isEmpty()
        }

    private fun transactRead(binder: IBinder, code: Int, decode: (Parcel) -> Boolean?): Boolean? {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DCM_INTERFACE)
            if (!binder.transact(code, data, reply, 0)) return null
            reply.readException()
            val result = decode(reply)
            if (reply.dataAvail() == 0) result else null
        } catch (_: Exception) {
            null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }
}

/** Pure monotonic policy; only the reader's stateLock may mutate its instance. */
internal class CarPlayBridgeQuiescenceWindow {
    companion object {
        const val BIND_SETTLE_MS = 5_000L
        const val EMPTY_SPAN_MS = 2_000L
        const val MAX_SAMPLE_GAP_MS = 1_500L
        const val MIN_EMPTY_SAMPLES = 3
    }

    private var epoch: Long? = null
    private var firstEmptyAtMs: Long? = null
    private var previousAtMs: Long? = null
    private var emptySamples = 0

    fun reset() {
        epoch = null
        firstEmptyAtMs = null
        previousAtMs = null
        emptySamples = 0
    }

    fun observe(epoch: Long, boundAtMs: Long, nowMs: Long, empty: Boolean?): Boolean? {
        val previous = previousAtMs
        if (this.epoch != epoch || previous != null &&
            (nowMs <= previous || nowMs - previous > MAX_SAMPLE_GAP_MS)) reset()
        this.epoch = epoch
        previousAtMs = nowMs
        if (empty != true || boundAtMs < 0 || nowMs < boundAtMs ||
            nowMs - boundAtMs < BIND_SETTLE_MS) {
            firstEmptyAtMs = null
            emptySamples = 0
            return if (empty == false) false else null
        }
        if (firstEmptyAtMs == null) firstEmptyAtMs = nowMs
        emptySamples++
        return if (emptySamples >= MIN_EMPTY_SAMPLES &&
            nowMs - firstEmptyAtMs!! >= EMPTY_SPAN_MS) true else null
    }
}
