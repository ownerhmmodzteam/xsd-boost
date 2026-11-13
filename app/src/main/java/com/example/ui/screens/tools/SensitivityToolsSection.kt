package com.example.ui.screens.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.SensitivityPresetEntity
import com.example.core.system.sensitivity.InputCompatibilityLayer
import com.example.core.system.sensitivity.InputCompatibilityReport
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderActive
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBg
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

fun LazyListScope.sensitivityControllerSection(
    sensX: Int,
    sensY: Int,
    baselineX: Int,
    baselineY: Int,
    presets: List<SensitivityPresetEntity>,
    compatibilityReport: InputCompatibilityReport,
    touchFeedbackMessage: String?,
    onCalibrateTouch: () -> Unit,
    onSave: (Int, Int) -> Unit,
    onSavePreset: (String, Int, Int) -> Unit,
    onLoadPreset: (SensitivityPresetEntity) -> Unit,
    onDeletePreset: (Long) -> Unit
) {
    item(key = "sensitivity_header_card") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ToolCardBrush)
                .border(1.dp, BorderActive, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RESPONSIVE SENSITIVITY X / Y",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryWhite,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "RANGE 1 - 26",
                            color = AccentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    text = "Atur profil X (horizontal) dan Y (vertikal). Perubahan langsung memperbarui preview dan profil lokal per game.",
                    fontSize = 12.sp,
                    color = SecondaryWhite,
                    lineHeight = 17.sp
                )

                Button(
                    onClick = onCalibrateTouch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElevatedSurface,
                        contentColor = PrimaryWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Sync Touch Pipeline",
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Check Touch Pipeline via Shizuku", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (touchFeedbackMessage != null) {
                    Text(
                        text = "Status: $touchFeedbackMessage",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    item(key = "sensitivity_x_card") {
        SensitivityAxisCard(
            axisName = "SENSITIVITY X (HORIZONTAL)",
            value = sensX,
            baseline = baselineX,
            onValueChange = { newX -> onSave(newX, sensY) }
        )
    }

    item(key = "sensitivity_y_card") {
        SensitivityAxisCard(
            axisName = "SENSITIVITY Y (VERTICAL)",
            value = sensY,
            baseline = baselineY,
            onValueChange = { newY -> onSave(sensX, newY) }
        )
    }

    item(key = "sensitivity_sim_pad") {
        SensitivitySimulationPadCard(sensX = sensX, sensY = sensY)
    }

    item(key = "sensitivity_profile_actions") {
        SensitivityProfileManagementCard(
            sensX = sensX,
            sensY = sensY,
            presets = presets,
            onReset = { onSave(1, 1) },
            onSavePreset = onSavePreset,
            onLoadPreset = onLoadPreset,
            onDeletePreset = onDeletePreset
        )
    }

    item(key = "sensitivity_compatibility_layer") {
        InputCompatibilityCard(report = compatibilityReport)
    }
}

@Composable
private fun SensitivityAxisCard(
    axisName: String,
    value: Int,
    baseline: Int,
    onValueChange: (Int) -> Unit
) {
    val delta = value - baseline
    val deltaLabel = when {
        delta > 0 -> "+$delta"
        delta < 0 -> "$delta"
        else -> "0"
    }
    val deltaColor = when {
        delta > 0 -> AccentGreen
        delta < 0 -> AccentAmber
        else -> MutedText
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ToolCardBrush)
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = axisName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedText,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Delta: $baseline → $value ($deltaLabel)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = deltaColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "$value",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryWhite,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt().coerceIn(1, 26)) },
                valueRange = 1f..26f,
                steps = 24,
                colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onValueChange((value - 1).coerceAtLeast(1)) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = PrimaryWhite)
                }

                Text(
                    text = "Range: 1 - 26 • Preview ${(0.50f + (((value - 1).coerceIn(0, 25) / 25f).let { it * it } * 2.0f)).let { String.format("%.2fx", it) }}",
                    fontSize = 11.sp,
                    color = SecondaryWhite,
                    fontFamily = FontFamily.Monospace
                )

                IconButton(
                    onClick = { onValueChange((value + 1).coerceAtMost(26)) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = PrimaryWhite)
                }
            }
        }
    }
}

@Composable
private fun SensitivitySimulationPadCard(sensX: Int, sensY: Int) {
    var reticleX by remember { mutableFloatStateOf(0f) }
    var reticleY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ToolCardBrush)
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SENSITIVITY SIMULATION PAD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryWhite,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "X:$sensX | Y:$sensY",
                    fontSize = 11.sp,
                    color = AccentCyan,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF050505))
                    .border(1.dp, BorderActive, RoundedCornerShape(14.dp))
                    .testTag("sensitivity_simulation_pad")
                    .pointerInput(sensX, sensY) {
                        detectDragGestures(
                            onDragEnd = {
                                reticleX = 0f
                                reticleY = 0f
                            },
                            onDragCancel = {
                                reticleX = 0f
                                reticleY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val (simDx, simDy) = InputCompatibilityLayer.computeSimulatedReticleDelta(
                                    dragAmount.x,
                                    dragAmount.y,
                                    sensX,
                                    sensY
                                )
                                val maxW = (size.width / 2f - 14f).coerceAtLeast(14f)
                                val maxH = (size.height / 2f - 14f).coerceAtLeast(14f)
                                reticleX = (reticleX + simDx).coerceIn(-maxW, maxW)
                                reticleY = (reticleY + simDy).coerceIn(-maxH, maxH)
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(Color.White.copy(alpha = 0.14f), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
                    drawLine(Color.White.copy(alpha = 0.14f), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)
                    val target = Offset(cx + reticleX, cy + reticleY)
                    drawLine(Color.White.copy(alpha = 0.4f), Offset(cx, cy), target, strokeWidth = 2f)
                    drawCircle(Color.White, radius = 12f, center = target, style = Stroke(width = 2.5f))
                    drawCircle(AccentCyan, radius = 4.5f, center = target)
                }
            }

            Text(
                text = "Geser jari di dalam pad untuk merasakan simulasi responsivitas visual X/Y. Simulasi ini lokal dan tidak mengubah sensitivitas internal Free Fire.",
                fontSize = 11.sp,
                color = MutedText,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SensitivityProfileManagementCard(
    sensX: Int,
    sensY: Int,
    presets: List<SensitivityPresetEntity>,
    onReset: () -> Unit,
    onSavePreset: (String, Int, Int) -> Unit,
    onLoadPreset: (SensitivityPresetEntity) -> Unit,
    onDeletePreset: (Long) -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var showSavedList by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ToolCardBrush)
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "SAVE / LOAD / RESET PROFILE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryWhite,
                fontFamily = FontFamily.Monospace
            )

            OutlinedTextField(
                value = profileName,
                onValueChange = { profileName = it },
                placeholder = { Text("Nama profil (cth: Free Fire Rush)", fontSize = 11.sp, color = MutedText) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, color = PrimaryWhite),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryWhite,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = PrimaryWhite,
                    unfocusedTextColor = SecondaryWhite,
                    cursorColor = PrimaryWhite
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val finalName = profileName.trim().ifEmpty { "Preset X$sensX Y$sensY" }
                        onSavePreset(finalName, sensX, sensY)
                        profileName = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_sensitivity_profile_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryWhite, contentColor = BgPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showSavedList = !showSavedList },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("load_sensitivity_profile_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface, contentColor = PrimaryWhite),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load (${presets.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_sensitivity_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElevatedSurface,
                    contentColor = PrimaryWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Sensitivity")
                    Text("RESET TO DEFAULT (X: 1, Y: 1)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (showSavedList && presets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardBg)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                .clickable { onLoadPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    color = PrimaryWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Sensitivity X: ${preset.sensitivityX} • Y: ${preset.sensitivityY} (Ketuk untuk Load)",
                                    color = MutedText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(onClick = { onDeletePreset(preset.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete preset", tint = MutedText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputCompatibilityCard(report: InputCompatibilityReport) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ToolCardBrush)
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "INPUT COMPATIBILITY LAYER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryWhite,
                fontFamily = FontFamily.Monospace
            )
            CompatRow("Active Tier", report.activeTier.title)
            CompatRow("1. Android Game API", report.officialGameApiStatus)
            CompatRow("2. OEM / Shizuku API", report.oemInputApiStatus)
            CompatRow(
                "3. System Pointer Speed",
                report.systemPointerSpeed?.toString() ?: "N/A (Android Read-only)"
            )
            CompatRow(
                "4. Touch Hardware",
                report.touchDeviceNames.firstOrNull() ?: "Standard Android Touchscreen"
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = report.honestExplanation,
                fontSize = 11.sp,
                color = MutedText,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun CompatRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(text = label, color = SecondaryWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = MutedText, fontSize = 10.sp, lineHeight = 13.sp)
    }
}
