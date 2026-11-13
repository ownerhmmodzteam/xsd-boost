package com.example.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameProfileRepository(
    private val dao: GameProfileDao,
    private val presetDao: SensitivityPresetDao? = null
) {

    val allProfiles: Flow<List<GameProfileEntity>> = dao.observeAllProfiles()
    val enabledProfiles: Flow<List<GameProfileEntity>> = dao.observeEnabledProfiles()
    val sensitivityPresets: Flow<List<SensitivityPresetEntity>>? = presetDao?.observeAllPresets()

    val enabledPackageSet: Flow<Set<String>> = enabledProfiles.map { list ->
        list.map { it.packageName }.toSet()
    }

    fun observeProfile(packageName: String): Flow<GameProfileEntity?> {
        return dao.observeProfile(packageName)
    }

    suspend fun getOrCreateProfile(
        packageName: String,
        appName: String,
        defaultOverlaySide: String = "RIGHT",
        defaultHandleSizeDp: Float = 76f,
        defaultHandleOpacity: Float = 0.90f,
        defaultLongPressMs: Long = 420L
    ): GameProfileEntity {
        val existing = dao.getProfile(packageName)
        if (existing != null) return existing
        val created = GameProfileEntity(
            packageName = packageName,
            appName = appName,
            overlaySide = if (defaultOverlaySide.equals("LEFT", ignoreCase = true)) "LEFT" else "RIGHT",
            handleSizeDp = defaultHandleSizeDp.coerceIn(56f, 120f),
            handleOpacity = defaultHandleOpacity.coerceIn(0.25f, 1.0f),
            longPressDurationMs = defaultLongPressMs.coerceIn(250L, 800L)
        )
        dao.upsert(created)
        return created
    }

    suspend fun setBoosterEnabled(
        packageName: String,
        appName: String,
        enabled: Boolean,
        defaultOverlaySide: String = "RIGHT",
        defaultHandleSizeDp: Float = 76f,
        defaultHandleOpacity: Float = 0.90f,
        defaultLongPressMs: Long = 420L
    ): GameProfileEntity {
        val base = getOrCreateProfile(
            packageName = packageName,
            appName = appName,
            defaultOverlaySide = defaultOverlaySide,
            defaultHandleSizeDp = defaultHandleSizeDp,
            defaultHandleOpacity = defaultHandleOpacity,
            defaultLongPressMs = defaultLongPressMs
        )
        val updated = base.copy(
            appName = appName,
            boosterEnabled = enabled,
            overlayEnabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsert(updated)
        return updated
    }

    suspend fun updateProfile(
        packageName: String,
        appName: String = packageName,
        transform: (GameProfileEntity) -> GameProfileEntity
    ): GameProfileEntity {
        val current = getOrCreateProfile(packageName, appName)
        val updated = transform(current).copy(updatedAt = System.currentTimeMillis())
        dao.upsert(updated)
        return updated
    }

    suspend fun resetProfile(packageName: String): GameProfileEntity? {
        val current = dao.getProfile(packageName) ?: return null
        val reset = GameProfileEntity(
            packageName = current.packageName,
            appName = current.appName,
            boosterEnabled = current.boosterEnabled,
            overlayEnabled = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsert(reset)
        return reset
    }

    suspend fun saveSensitivityPreset(name: String, x: Int, y: Int, gamePackageName: String = ""): Long? {
        val daoRef = presetDao ?: return null
        val cleanName = name.trim().ifEmpty { "Preset ${x}x${y}" }
        return daoRef.insert(
            SensitivityPresetEntity(
                name = cleanName,
                gamePackageName = gamePackageName,
                sensitivityX = x.coerceIn(1, 26),
                sensitivityY = y.coerceIn(1, 26)
            )
        )
    }

    suspend fun deleteSensitivityPreset(id: Long) {
        presetDao?.deleteById(id)
    }
}

fun GameProfileEntity.toCrosshairConfig(): com.example.core.system.crosshair.CrosshairConfig {
    val style = try {
        com.example.core.system.crosshair.CrosshairStyle.valueOf(crosshairStyle)
    } catch (_: Exception) {
        com.example.core.system.crosshair.CrosshairStyle.CLASSIC_CROSS
    }
    return com.example.core.system.crosshair.CrosshairConfig(
        style = style,
        colorArgb = crosshairColorArgb,
        sizeDp = crosshairSizeDp,
        opacity = crosshairOpacity
    )
}
