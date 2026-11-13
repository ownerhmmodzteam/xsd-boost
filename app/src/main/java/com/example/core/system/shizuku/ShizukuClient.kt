package com.example.core.system.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class ShizukuClientState(
    val isInstalled: Boolean = false,
    val isBinderAlive: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val shouldShowRationale: Boolean = false,
    val isUserServiceBound: Boolean = false,
    val serverVersion: Int = 0,
    val uid: Int = -1,
    val lastError: String? = null
)

data class ShizukuMetricsSnapshot(
    val cpuUsagePercent: Float? = null,
    val temperatureCelsius: Float? = null
)

/**
 * Utility class handling all communication with Shizuku Manager via Binder IPC and UserService.
 * Safely manages binder lifecycle, runtime Shizuku permission requests, and privileged telemetry
 * reads (CPU usage & thermal sensors) without requiring root.
 */
class ShizukuClient(context: Context) {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(ShizukuClientState())
    val state: StateFlow<ShizukuClientState> = _state.asStateFlow()

    @Volatile
    private var metricsService: IShizukuMetricsService? = null

    @Volatile
    private var isBindingInProgress = false

    @Volatile
    private var listenersRegistered = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshState()
        if (_state.value.isPermissionGranted) {
            bindMetricsService()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        metricsService = null
        isBindingInProgress = false
        _state.value = _state.value.copy(
            isBinderAlive = false,
            isUserServiceBound = false,
            lastError = "Shizuku binder disconnected."
        )
        refreshState()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                refreshState()
                if (granted) {
                    bindMetricsService()
                } else {
                    _state.value = _state.value.copy(
                        lastError = "Izin Shizuku ditolak oleh pengguna."
                    )
                }
            }
        }

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(appContext.packageName, ShizukuMetricsUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("metrics")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isBindingInProgress = false
            metricsService = IShizukuMetricsService.Stub.asInterface(service)
            _state.value = _state.value.copy(
                isUserServiceBound = metricsService != null,
                lastError = null
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBindingInProgress = false
            metricsService = null
            _state.value = _state.value.copy(isUserServiceBound = false)
        }
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val SUI_PACKAGE = "moe.shizuku.redirect"
        const val REQUEST_CODE_SHIZUKU = 9401
    }

    init {
        initialize()
    }

    fun initialize() {
        if (!listenersRegistered) {
            try {
                Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
                Shizuku.addRequestPermissionResultListener(permissionResultListener)
                listenersRegistered = true
            } catch (_: Throwable) {
                // Safe fallback under JVM unit tests or environments without Shizuku IPC
            }
        }
        refreshState()
    }

    fun refreshState(): ShizukuClientState {
        val pm = appContext.packageManager
        val isInstalled = isPackageInstalled(pm, SHIZUKU_PACKAGE) || isPackageInstalled(pm, SUI_PACKAGE)

        val binderAlive = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }

        var permissionGranted = false
        var showRationale = false
        var version = 0
        var serverUid = -1
        var errorMessage: String? = null

        if (binderAlive) {
            try {
                if (Shizuku.isPreV11()) {
                    errorMessage = "Versi Shizuku terlalu lama (membutuhkan Shizuku v11+)."
                } else {
                    version = Shizuku.getVersion()
                    serverUid = Shizuku.getUid()
                    permissionGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                    showRationale = !permissionGranted && Shizuku.shouldShowRequestPermissionRationale()
                }
            } catch (t: Throwable) {
                errorMessage = "Shizuku IPC error: ${t.localizedMessage}"
            }
        }

        val updated = ShizukuClientState(
            isInstalled = isInstalled,
            isBinderAlive = binderAlive,
            isPermissionGranted = permissionGranted,
            shouldShowRationale = showRationale,
            isUserServiceBound = metricsService != null,
            serverVersion = version,
            uid = serverUid,
            lastError = errorMessage
        )
        _state.value = updated

        if (binderAlive && permissionGranted && metricsService == null && !isBindingInProgress) {
            bindMetricsService()
        }

        return updated
    }

    /**
     * Safely requests Shizuku API permission without requiring root.
     * Returns true if permission is already granted or the request dialog was launched.
     */
    fun requestPermission(): Boolean {
        val current = refreshState()
        if (!current.isBinderAlive) {
            _state.value = current.copy(
                lastError = if (current.isInstalled) {
                    "Shizuku terpasang tetapi layanan belum berjalan. Jalankan Shizuku terlebih dahulu."
                } else {
                    "Aplikasi Shizuku tidak ditemukan di perangkat."
                }
            )
            return false
        }

        return try {
            if (Shizuku.isPreV11()) {
                _state.value = _state.value.copy(
                    lastError = "Versi Shizuku tidak mendukung API v11+."
                )
                false
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindMetricsService()
                true
            } else {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
                true
            }
        } catch (t: Throwable) {
            _state.value = _state.value.copy(
                lastError = "Gagal meminta izin Shizuku: ${t.localizedMessage}"
            )
            false
        }
    }

    fun bindMetricsService() {
        if (metricsService != null || isBindingInProgress) return
        val current = _state.value
        if (!current.isBinderAlive || !current.isPermissionGranted) return

        try {
            isBindingInProgress = true
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        } catch (t: Throwable) {
            isBindingInProgress = false
            _state.value = _state.value.copy(
                lastError = "Gagal bind Shizuku UserService: ${t.localizedMessage}"
            )
        }
    }

    fun unbindMetricsService() {
        try {
            if (metricsService != null) {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            }
        } catch (_: Throwable) {
        } finally {
            metricsService = null
            isBindingInProgress = false
            _state.value = _state.value.copy(isUserServiceBound = false)
        }
    }

    /**
     * Reads real CPU usage and device thermal metrics via the bound Shizuku UserService.
     * Returns null fields safely when Shizuku is unavailable, denied, or unbound.
     */
    fun readSystemMetrics(): ShizukuMetricsSnapshot {
        val service = metricsService
        if (service == null) {
            val st = _state.value
            if (st.isBinderAlive && st.isPermissionGranted && !isBindingInProgress) {
                bindMetricsService()
            }
            return ShizukuMetricsSnapshot()
        }

        return try {
            val cpu = service.readCpuUsagePercent()
            val temp = service.readThermalCelsius()
            ShizukuMetricsSnapshot(
                cpuUsagePercent = if (cpu >= 0f && cpu <= 100f) cpu else null,
                temperatureCelsius = if (temp in 10f..115f) temp else null
            )
        } catch (_: Throwable) {
            metricsService = null
            _state.value = _state.value.copy(isUserServiceBound = false)
            ShizukuMetricsSnapshot()
        }
    }

    /**
     * Executes a shell command via Shizuku UserService (ADB/rootless privilege).
     */
    suspend fun executePrivilegedCommand(command: String): String = withContext(Dispatchers.IO) {
        val current = refreshState()
        if (!current.isBinderAlive) {
            return@withContext if (current.isInstalled) {
                "Shizuku belum aktif. Buka Shizuku dan mulai service terlebih dahulu."
            } else {
                "Shizuku belum terpasang. Jalankan dalam mode Standalone."
            }
        }
        if (!current.isPermissionGranted) {
            return@withContext "Izin Shizuku belum diberikan. Ketuk 'Request Shizuku Permission' terlebih dahulu."
        }

        val service = metricsService ?: run {
            bindMetricsService()
            return@withContext "Menghubungkan Shizuku UserService... Coba lagi dalam 1 detik."
        }

        return@withContext try {
            service.executeShellCommand(command)
        } catch (t: Throwable) {
            metricsService = null
            _state.value = _state.value.copy(isUserServiceBound = false)
            "Eksekusi Shizuku gagal: ${t.localizedMessage}"
        }
    }

    fun openShizukuManager(): Boolean {
        return try {
            val launchIntent = appContext.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(launchIntent)
                true
            } else {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://shizuku.rikka.app/guide/setup/")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                appContext.startActivity(webIntent)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun dispose() {
        unbindMetricsService()
        if (listenersRegistered) {
            try {
                Shizuku.removeBinderReceivedListener(binderReceivedListener)
                Shizuku.removeBinderDeadListener(binderDeadListener)
                Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            } catch (_: Throwable) {
            }
            listenersRegistered = false
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
