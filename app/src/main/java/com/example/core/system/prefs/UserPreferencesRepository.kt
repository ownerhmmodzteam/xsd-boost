package com.example.core.system.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.system.crosshair.CrosshairConfig
import com.example.core.system.crosshair.CrosshairStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xsd_boost_settings")

data class EdgeSwipeConfig(
    val enabled: Boolean = true,
    val sensitivity: Float = 0.55f, // 0.2f (low sensitivity / long swipe) to 1.0f (high sensitivity / short swipe)
    val triggerYFraction: Float = 0.35f, // 0.1f to 0.85f from top
    val triggerWidthDp: Float = 24f, // 16dp to 36dp
    val triggerHeightDp: Float = 130f // 60dp to 220dp
)

data class BubblePosition(
    val x: Int = 36,
    val y: Int = 220
)

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val SENSITIVITY_X = intPreferencesKey("sensitivity_x")
        val SENSITIVITY_Y = intPreferencesKey("sensitivity_y")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val CROSSHAIR_ENABLED = booleanPreferencesKey("crosshair_enabled")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val OVERLAY_COMPACT = booleanPreferencesKey("overlay_compact")
        val MONITOR_INTERVAL_MS = longPreferencesKey("monitor_interval_ms")
        val FAVORITE_GAMES = stringSetPreferencesKey("favorite_games")
        val SELECTED_GRAPHICS_PROFILE = stringPreferencesKey("selected_graphics_profile")

        // Left Edge Swipe & Mini Bubble persistence
        val EDGE_SWIPE_ENABLED = booleanPreferencesKey("edge_swipe_enabled")
        val EDGE_SWIPE_SENSITIVITY = floatPreferencesKey("edge_swipe_sensitivity")
        val EDGE_TRIGGER_Y_FRACTION = floatPreferencesKey("edge_trigger_y_fraction")
        val EDGE_TRIGGER_WIDTH_DP = floatPreferencesKey("edge_trigger_width_dp")
        val EDGE_TRIGGER_HEIGHT_DP = floatPreferencesKey("edge_trigger_height_dp")
        val BUBBLE_X = intPreferencesKey("bubble_x")
        val BUBBLE_Y = intPreferencesKey("bubble_y")

        // Crosshair configs
        val CROSSHAIR_STYLE = stringPreferencesKey("crosshair_style")
        val CROSSHAIR_COLOR = longPreferencesKey("crosshair_color")
        val CROSSHAIR_SIZE = floatPreferencesKey("crosshair_size")
        val CROSSHAIR_THICKNESS = floatPreferencesKey("crosshair_thickness")
        val CROSSHAIR_OPACITY = floatPreferencesKey("crosshair_opacity")
        val CROSSHAIR_CENTER_DOT = booleanPreferencesKey("crosshair_center_dot")
        val CROSSHAIR_GAP = floatPreferencesKey("crosshair_gap")
        val CROSSHAIR_OUTLINE = booleanPreferencesKey("crosshair_outline")
        val CROSSHAIR_ROTATION = floatPreferencesKey("crosshair_rotation")
        val CROSSHAIR_POSITION_X = floatPreferencesKey("crosshair_position_x")
        val CROSSHAIR_POSITION_Y = floatPreferencesKey("crosshair_position_y")

        // Smart Game Detection & Flexible Edge Handle preferences
        val SMART_BOOSTER_WATCHER_ENABLED = booleanPreferencesKey("smart_booster_watcher_enabled")
        val DEFAULT_HANDLE_HIDDEN = booleanPreferencesKey("default_handle_hidden")
        val DEFAULT_OVERLAY_SIDE = stringPreferencesKey("default_overlay_side")
        val DEFAULT_HANDLE_SIZE_DP = floatPreferencesKey("default_handle_size_dp")
        val DEFAULT_HANDLE_OPACITY = floatPreferencesKey("default_handle_opacity")
        val DEFAULT_LONG_PRESS_MS = longPreferencesKey("default_long_press_ms")
        val SENSITIVITY_BASELINE_X = intPreferencesKey("sensitivity_baseline_x")
        val SENSITIVITY_BASELINE_Y = intPreferencesKey("sensitivity_baseline_y")
    }

    val sensitivityX: Flow<Int> = context.dataStore.data.map { it[SENSITIVITY_X] ?: 1 }
    val sensitivityY: Flow<Int> = context.dataStore.data.map { it[SENSITIVITY_Y] ?: 1 }
    val sensitivityBaselineX: Flow<Int> = context.dataStore.data.map { it[SENSITIVITY_BASELINE_X] ?: 1 }
    val sensitivityBaselineY: Flow<Int> = context.dataStore.data.map { it[SENSITIVITY_BASELINE_Y] ?: 1 }
    val isOverlayEnabled: Flow<Boolean> = context.dataStore.data.map { it[OVERLAY_ENABLED] ?: false }
    val isCrosshairEnabled: Flow<Boolean> = context.dataStore.data.map { it[CROSSHAIR_ENABLED] ?: false }
    val overlayOpacity: Flow<Float> = context.dataStore.data.map { it[OVERLAY_OPACITY] ?: 0.90f }
    val overlayCompact: Flow<Boolean> = context.dataStore.data.map { it[OVERLAY_COMPACT] ?: false }
    val monitorIntervalMs: Flow<Long> = context.dataStore.data.map { it[MONITOR_INTERVAL_MS] ?: 1000L }
    val favoriteGames: Flow<Set<String>> = context.dataStore.data.map { it[FAVORITE_GAMES] ?: emptySet() }
    val selectedGraphicsProfile: Flow<String> = context.dataStore.data.map { it[SELECTED_GRAPHICS_PROFILE] ?: "ultra_smooth" }
    val isSmartWatcherEnabled: Flow<Boolean> = context.dataStore.data.map { it[SMART_BOOSTER_WATCHER_ENABLED] ?: true }
    val isDefaultHandleHidden: Flow<Boolean> = context.dataStore.data.map { it[DEFAULT_HANDLE_HIDDEN] ?: false }
    val defaultOverlaySide: Flow<String> = context.dataStore.data.map {
        val raw = it[DEFAULT_OVERLAY_SIDE] ?: "RIGHT"
        if (raw.equals("LEFT", ignoreCase = true)) "LEFT" else "RIGHT"
    }
    val defaultHandleSizeDp: Flow<Float> = context.dataStore.data.map { (it[DEFAULT_HANDLE_SIZE_DP] ?: 76f).coerceIn(56f, 120f) }
    val defaultHandleOpacity: Flow<Float> = context.dataStore.data.map { (it[DEFAULT_HANDLE_OPACITY] ?: 0.90f).coerceIn(0.25f, 1.0f) }
    val defaultLongPressMs: Flow<Long> = context.dataStore.data.map { (it[DEFAULT_LONG_PRESS_MS] ?: 420L).coerceIn(250L, 800L) }

    val edgeSwipeConfig: Flow<EdgeSwipeConfig> = context.dataStore.data.map { prefs ->
        EdgeSwipeConfig(
            enabled = prefs[EDGE_SWIPE_ENABLED] ?: true,
            sensitivity = (prefs[EDGE_SWIPE_SENSITIVITY] ?: 0.55f).coerceIn(0.2f, 1.0f),
            triggerYFraction = (prefs[EDGE_TRIGGER_Y_FRACTION] ?: 0.35f).coerceIn(0.10f, 0.85f),
            triggerWidthDp = (prefs[EDGE_TRIGGER_WIDTH_DP] ?: 24f).coerceIn(16f, 36f),
            triggerHeightDp = (prefs[EDGE_TRIGGER_HEIGHT_DP] ?: 130f).coerceIn(60f, 220f)
        )
    }

    val bubblePosition: Flow<BubblePosition> = context.dataStore.data.map { prefs ->
        BubblePosition(
            x = prefs[BUBBLE_X] ?: 36,
            y = prefs[BUBBLE_Y] ?: 220
        )
    }

    val crosshairConfig: Flow<CrosshairConfig> = context.dataStore.data.map { prefs ->
        val styleStr = prefs[CROSSHAIR_STYLE] ?: CrosshairStyle.CLASSIC_CROSS.name
        val style = try {
            CrosshairStyle.valueOf(styleStr)
        } catch (_: Exception) {
            CrosshairStyle.CLASSIC_CROSS
        }
        CrosshairConfig(
            style = style,
            colorArgb = prefs[CROSSHAIR_COLOR] ?: 0xFFFFFFFF,
            sizeDp = prefs[CROSSHAIR_SIZE] ?: 28f,
            thicknessDp = prefs[CROSSHAIR_THICKNESS] ?: 2.5f,
            opacity = prefs[CROSSHAIR_OPACITY] ?: 0.95f,
            hasCenterDot = prefs[CROSSHAIR_CENTER_DOT] ?: true,
            gapDp = prefs[CROSSHAIR_GAP] ?: 6f,
            hasOutline = prefs[CROSSHAIR_OUTLINE] ?: true,
            rotationDegrees = prefs[CROSSHAIR_ROTATION] ?: 0f,
            positionX = prefs[CROSSHAIR_POSITION_X] ?: 0.5f,
            positionY = prefs[CROSSHAIR_POSITION_Y] ?: 0.5f
        )
    }

    suspend fun setSensitivity(x: Int, y: Int) {
        context.dataStore.edit {
            it[SENSITIVITY_X] = x.coerceIn(1, 26)
            it[SENSITIVITY_Y] = y.coerceIn(1, 26)
        }
    }

    suspend fun setSensitivityBaseline(x: Int, y: Int) {
        context.dataStore.edit {
            it[SENSITIVITY_BASELINE_X] = x.coerceIn(1, 26)
            it[SENSITIVITY_BASELINE_Y] = y.coerceIn(1, 26)
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[OVERLAY_ENABLED] = enabled }
    }

    suspend fun setCrosshairEnabled(enabled: Boolean) {
        context.dataStore.edit { it[CROSSHAIR_ENABLED] = enabled }
    }

    suspend fun setOverlayOpacity(opacity: Float) {
        context.dataStore.edit { it[OVERLAY_OPACITY] = opacity.coerceIn(0.3f, 1f) }
    }

    suspend fun setOverlayCompact(compact: Boolean) {
        context.dataStore.edit { it[OVERLAY_COMPACT] = compact }
    }

    suspend fun setMonitorIntervalMs(intervalMs: Long) {
        context.dataStore.edit { it[MONITOR_INTERVAL_MS] = intervalMs.coerceIn(500L, 5000L) }
    }

    suspend fun setEdgeSwipeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[EDGE_SWIPE_ENABLED] = enabled }
    }

    suspend fun setEdgeSwipeSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[EDGE_SWIPE_SENSITIVITY] = sensitivity.coerceIn(0.2f, 1.0f) }
    }

    suspend fun setEdgeTriggerYFraction(fraction: Float) {
        context.dataStore.edit { it[EDGE_TRIGGER_Y_FRACTION] = fraction.coerceIn(0.10f, 0.85f) }
    }

    suspend fun setEdgeTriggerSize(widthDp: Float, heightDp: Float) {
        context.dataStore.edit {
            it[EDGE_TRIGGER_WIDTH_DP] = widthDp.coerceIn(16f, 36f)
            it[EDGE_TRIGGER_HEIGHT_DP] = heightDp.coerceIn(60f, 220f)
        }
    }

    suspend fun setBubblePosition(x: Int, y: Int) {
        context.dataStore.edit {
            it[BUBBLE_X] = x
            it[BUBBLE_Y] = y
        }
    }

    suspend fun toggleFavoriteGame(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_GAMES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[FAVORITE_GAMES] = current
        }
    }

    suspend fun setGraphicsProfile(profileId: String) {
        context.dataStore.edit { it[SELECTED_GRAPHICS_PROFILE] = profileId }
    }

    suspend fun saveCrosshairConfig(config: CrosshairConfig) {
        context.dataStore.edit {
            it[CROSSHAIR_STYLE] = config.style.name
            it[CROSSHAIR_COLOR] = config.colorArgb
            it[CROSSHAIR_SIZE] = config.sizeDp
            it[CROSSHAIR_THICKNESS] = config.thicknessDp
            it[CROSSHAIR_OPACITY] = config.opacity
            it[CROSSHAIR_CENTER_DOT] = config.hasCenterDot
            it[CROSSHAIR_GAP] = config.gapDp
            it[CROSSHAIR_OUTLINE] = config.hasOutline
            it[CROSSHAIR_ROTATION] = config.rotationDegrees
            it[CROSSHAIR_POSITION_X] = config.positionX.coerceIn(0f, 1f)
            it[CROSSHAIR_POSITION_Y] = config.positionY.coerceIn(0f, 1f)
        }
    }

    suspend fun setSmartWatcherEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SMART_BOOSTER_WATCHER_ENABLED] = enabled }
    }

    suspend fun setDefaultHandleHidden(hidden: Boolean) {
        context.dataStore.edit { it[DEFAULT_HANDLE_HIDDEN] = hidden }
    }

    suspend fun setDefaultOverlaySide(side: String) {
        val clean = if (side.equals("LEFT", ignoreCase = true)) "LEFT" else "RIGHT"
        context.dataStore.edit { it[DEFAULT_OVERLAY_SIDE] = clean }
    }

    suspend fun setDefaultHandleSizeDp(sizeDp: Float) {
        context.dataStore.edit { it[DEFAULT_HANDLE_SIZE_DP] = sizeDp.coerceIn(56f, 120f) }
    }

    suspend fun setDefaultHandleOpacity(opacity: Float) {
        context.dataStore.edit { it[DEFAULT_HANDLE_OPACITY] = opacity.coerceIn(0.25f, 1.0f) }
    }

    suspend fun setDefaultLongPressMs(durationMs: Long) {
        context.dataStore.edit { it[DEFAULT_LONG_PRESS_MS] = durationMs.coerceIn(250L, 800L) }
    }
}
