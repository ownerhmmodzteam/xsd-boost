package com.example.core.system.shizuku

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShizukuStatus(
    val isInstalled: Boolean = false,
    val isRunning: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val binderAvailable: Boolean = false,
    val isUserServiceBound: Boolean = false,
    val serverVersion: Int = 0,
    val lastExecutedCommand: String? = null,
    val lastCommandOutput: String? = null,
    val lastError: String? = null,
    val activeTuningProfiles: List<String> = emptyList()
)

class ShizukuSystemBridge(
    context: Context,
    val client: ShizukuClient = ShizukuClient(context)
) {

    private val bridgeScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _status = MutableStateFlow(ShizukuStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    init {
        bridgeScope.launch {
            client.state.collect { clientState ->
                _status.value = _status.value.copy(
                    isInstalled = clientState.isInstalled,
                    isRunning = clientState.isBinderAlive,
                    isPermissionGranted = clientState.isPermissionGranted,
                    binderAvailable = clientState.isBinderAlive,
                    isUserServiceBound = clientState.isUserServiceBound,
                    serverVersion = clientState.serverVersion,
                    lastError = clientState.lastError,
                    activeTuningProfiles = if (clientState.isBinderAlive && clientState.isPermissionGranted) {
                        listOf("Governor Performance", "Touch Resampling Boost", "Privileged Metrics Access")
                    } else {
                        emptyList()
                    }
                )
            }
        }
        checkShizukuState()
    }

    fun checkShizukuState(): ShizukuStatus {
        client.refreshState()
        return _status.value
    }

    fun requestShizukuPermission(): Boolean {
        return client.requestPermission()
    }

    fun openShizukuApp(): Boolean {
        return client.openShizukuManager()
    }

    suspend fun executePrivilegedCommand(cmd: String): String {
        val result = client.executePrivilegedCommand(cmd)
        _status.value = _status.value.copy(
            lastExecutedCommand = cmd,
            lastCommandOutput = result
        )
        return result
    }

    suspend fun applyGamingGovernorProfile(): String {
        return executePrivilegedCommand("cmd power set-fixed-performance-mode-enabled true")
    }

    suspend fun clearAllGameBackgroundCaches(): String {
        return executePrivilegedCommand("pm trim-caches 4096M")
    }

    suspend fun optimizeTouchSamplingRate(): String {
        return executePrivilegedCommand("cmd display set-user-preferred-display-mode")
    }
}
