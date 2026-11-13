package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.booster.RamAnalysisReport
import com.example.core.system.crosshair.CrosshairConfig
import com.example.core.system.graphics.GraphicsHardwareInfo
import com.example.core.system.graphics.GraphicsPresetProfile
import com.example.core.system.scanner.AppSecurityReport
import com.example.core.system.shizuku.ShizukuStatus
import com.example.core.system.shizuku.ShizukuSystemBridge
import com.example.ui.screens.tools.apkScannerSection
import com.example.ui.screens.tools.crosshairStudioSection
import com.example.ui.screens.tools.graphicsDriverSection
import com.example.ui.screens.tools.ramBoosterSection
import com.example.ui.screens.tools.sensitivityControllerSection
import com.example.ui.screens.tools.shizukuEngineSection
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderActive
import com.example.ui.theme.BorderColor
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite
import kotlinx.coroutines.launch

enum class ActiveToolSection(val title: String, val icon: ImageVector) {
    SHIZUKU_ENGINE("Shizuku Engine", Icons.Default.Terminal),
    RAM_BOOSTER("RAM Booster", Icons.Default.CleaningServices),
    CROSSHAIR("Crosshair Studio", Icons.Default.CenterFocusStrong),
    SENSITIVITY("Sensitivity X/Y", Icons.Default.Tune),
    SCANNER("APK Scanner", Icons.Default.Security),
    GRAPHICS("Graphics Driver", Icons.Default.DisplaySettings)
}

@Composable
fun ToolsScreen(
    // Booster props
    lastBoosterReport: RamAnalysisReport?,
    isBoosting: Boolean,
    onRunSmartBoost: () -> Unit,
    // Crosshair props
    crosshairConfig: CrosshairConfig,
    isCrosshairOverlayActive: Boolean,
    onSaveCrosshairConfig: (CrosshairConfig) -> Unit,
    onToggleCrosshairOverlay: (Boolean) -> Unit,
    // Sensitivity props
    sensitivityX: Int,
    sensitivityY: Int,
    sensitivityBaselineX: Int = 1,
    sensitivityBaselineY: Int = 1,
    sensitivityPresets: List<com.example.core.database.SensitivityPresetEntity> = emptyList(),
    inputCompatibilityReport: com.example.core.system.sensitivity.InputCompatibilityReport? = null,
    onSaveSensitivity: (Int, Int) -> Unit,
    onSaveSensitivityPreset: (String, Int, Int) -> Unit = { _, _, _ -> },
    onLoadSensitivityPreset: (com.example.core.database.SensitivityPresetEntity) -> Unit = {},
    onDeleteSensitivityPreset: (Long) -> Unit = {},
    // Scanner props
    scanReports: List<AppSecurityReport>,
    isScanning: Boolean,
    onRunSecurityScan: () -> Unit,
    // Graphics props
    graphicsInfo: GraphicsHardwareInfo,
    graphicsProfiles: List<GraphicsPresetProfile>,
    selectedGraphicsProfileId: String,
    onSelectGraphicsProfile: (String) -> Unit,
    // Shizuku props
    shizukuStatus: ShizukuStatus,
    shizukuBridge: ShizukuSystemBridge,
    modifier: Modifier = Modifier
) {
    var activeSection by remember { mutableStateOf(ActiveToolSection.SHIZUKU_ENGINE) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Section-scoped transient states hoisted above LazyListScope builders
    var isShizukuExecuting by remember { mutableStateOf(false) }
    var shizukuExecutionText by remember { mutableStateOf<String?>(null) }
    var shizukuTrimResult by remember { mutableStateOf<String?>(null) }
    var touchFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var graphicsApplyResult by remember { mutableStateOf<String?>(null) }
    var localCrosshairConfig by remember(crosshairConfig) { mutableStateOf(crosshairConfig) }
    val resolvedCompatibilityReport = inputCompatibilityReport ?: remember(shizukuStatus) {
        com.example.core.system.sensitivity.InputCompatibilityLayer.inspect(context, shizukuStatus)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Clean header inside the scrollable LazyColumn so nothing pins/overlaps at the top
        item(key = "tools_header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "PERFORMANCE TOOLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = activeSection.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryWhite
                )
            }
        }

        // Scrollable horizontal selector chips inside the main LazyColumn
        item(key = "tools_selector_row") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(ActiveToolSection.values(), key = { it.name }) { section ->
                    val isSelected = section == activeSection
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) PrimaryWhite else ElevatedSurface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) PrimaryWhite else BorderActive,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { activeSection = section }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("tool_tab_${section.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.title,
                                tint = if (isSelected) BgPrimary else SecondaryWhite,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = section.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BgPrimary else SecondaryWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Emit active section items directly into this single LazyColumn
        when (activeSection) {
            ActiveToolSection.SHIZUKU_ENGINE -> shizukuEngineSection(
                status = shizukuStatus,
                bridge = shizukuBridge,
                isExecuting = isShizukuExecuting,
                executionStatusText = shizukuExecutionText,
                onExecuteCommand = { label, command ->
                    coroutineScope.launch {
                        isShizukuExecuting = true
                        shizukuExecutionText = label
                        shizukuExecutionText = command()
                        isShizukuExecuting = false
                    }
                }
            )
            ActiveToolSection.RAM_BOOSTER -> ramBoosterSection(
                report = lastBoosterReport,
                isBoosting = isBoosting,
                shizukuTrimResult = shizukuTrimResult,
                onBoostAndTrim = {
                    onRunSmartBoost()
                    coroutineScope.launch {
                        shizukuTrimResult = shizukuBridge.clearAllGameBackgroundCaches()
                    }
                }
            )
            ActiveToolSection.CROSSHAIR -> crosshairStudioSection(
                config = localCrosshairConfig,
                isOverlayActive = isCrosshairOverlayActive,
                onUpdateConfig = { updated ->
                    localCrosshairConfig = updated
                    onSaveCrosshairConfig(updated)
                },
                onToggleOverlay = onToggleCrosshairOverlay
            )
            ActiveToolSection.SENSITIVITY -> sensitivityControllerSection(
                sensX = sensitivityX,
                sensY = sensitivityY,
                baselineX = sensitivityBaselineX,
                baselineY = sensitivityBaselineY,
                presets = sensitivityPresets,
                compatibilityReport = resolvedCompatibilityReport,
                touchFeedbackMessage = touchFeedbackMessage,
                onCalibrateTouch = {
                    coroutineScope.launch {
                        touchFeedbackMessage = shizukuBridge.optimizeTouchSamplingRate()
                    }
                },
                onSave = onSaveSensitivity,
                onSavePreset = onSaveSensitivityPreset,
                onLoadPreset = onLoadSensitivityPreset,
                onDeletePreset = onDeleteSensitivityPreset
            )
            ActiveToolSection.SCANNER -> apkScannerSection(
                reports = scanReports,
                isScanning = isScanning,
                onScan = onRunSecurityScan
            )
            ActiveToolSection.GRAPHICS -> graphicsDriverSection(
                info = graphicsInfo,
                profiles = graphicsProfiles,
                selectedId = selectedGraphicsProfileId,
                applyResult = graphicsApplyResult,
                onSelectProfile = { profileId ->
                    onSelectGraphicsProfile(profileId)
                    coroutineScope.launch {
                        graphicsApplyResult = shizukuBridge.applyGamingGovernorProfile()
                    }
                }
            )
        }
    }
}
