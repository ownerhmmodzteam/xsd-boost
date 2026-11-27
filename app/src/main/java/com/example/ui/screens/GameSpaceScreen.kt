package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.system.games.GameApp
import com.example.core.system.hardware.SystemStats
import com.example.core.system.media.MusicPlayerState

private val GameSpaceGreen = Color(0xFF39F58A)
private val GameSpaceDark = Color(0xFF080D0B)

@Composable
fun GameSpaceScreen(
    stats: SystemStats,
    fps: Float,
    games: List<GameApp>,
    musicState: MusicPlayerState,
    isRogOverlayActive: Boolean,
    onLaunchGame: (String) -> Unit,
    onRunSmartBoost: () -> Unit,
    onToggleRogMonitor: (Boolean) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("xsd_game_space", Context.MODE_PRIVATE) }
    var selectedAnimation by rememberSaveable { mutableIntStateOf(prefs.getInt("selected_animation", 1).coerceIn(1, 5)) }
    var selectedGamePackages by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showGamePicker by remember { mutableStateOf(false) }
    var showMonitorPanel by rememberSaveable { mutableStateOf(false) }
    var showEntrance by rememberSaveable { mutableStateOf(true) }
    val ambientTransition = rememberInfiniteTransition(label = "game_space_ambient")
    val ambientProgress by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = androidx.compose.animation.core.tween(9000), repeatMode = RepeatMode.Reverse),
        label = "ambient_progress"
    )
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); showEntrance = false }
    LaunchedEffect(selectedAnimation) { prefs.edit().putInt("selected_animation", selectedAnimation).apply() }
    val selectedGames = remember(games, selectedGamePackages) { games.filter { it.packageName in selectedGamePackages } }
    val accentPalettes = listOf(
        Color(0xFF39F58A), Color(0xFF40D9FF), Color(0xFFB18CFF), Color(0xFFFFB45C), Color(0xFFFF668A)
    )
    val ambientAccent = accentPalettes[selectedAnimation - 1]
    BackHandler(onBack = onExit)

    BoxWithConstraints(
        modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF050B08), Color(0xFF101D17), Color(0xFF050907))))
            .drawBehind {
                val glowX = size.width * (0.18f + 0.64f * ambientProgress)
                val glowY = size.height * (0.24f + 0.12f * ambientProgress)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ambientAccent.copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(glowX, glowY),
                        radius = size.maxDimension * 0.72f
                    ),
                    radius = size.maxDimension * 0.72f,
                    center = Offset(glowX, glowY)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF39F58A).copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.82f),
                        radius = size.maxDimension * 0.52f
                    ),
                    radius = size.maxDimension * 0.52f,
                    center = Offset(size.width * 0.82f, size.height * 0.82f)
                )
            }
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color(0x55040A07), Color(0x08040A07), Color(0xAA040A07))
        )))
        Row(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // LEFT: compact game library
            Surface(Modifier.width(210.dp).fillMaxHeight(), color = Color(0xAA0D1511), shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2239F58A)) ) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("MY GAMES", color = GameSpaceGreen, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.2.sp)
                            Text("${selectedGames.size} games selected", color = Color.LightGray, fontSize = 9.sp)
                        }
                        IconButton(onClick = { showGamePicker = true }, modifier = Modifier.size(34.dp).background(Color(0xFF20382A), CircleShape)) {
                            Icon(Icons.Default.Add, "Tambah game", tint = GameSpaceGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (selectedGames.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Gamepad, null, tint = Color(0xFF65816D), modifier = Modifier.size(30.dp))
                                Text("Add your games", color = Color.LightGray, fontSize = 10.sp)
                                Text("Tap + to select", color = Color.Gray, fontSize = 9.sp)
                            }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(selectedGames, key = { it.packageName }) { game ->
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x66233229)).clickable { onLaunchGame(game.packageName) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AndroidView(factory = { ctx -> ImageView(ctx).apply { try { setImageDrawable(ctx.packageManager.getApplicationIcon(game.packageName)) } catch (_: Exception) {}; scaleType = ImageView.ScaleType.FIT_CENTER } }, modifier = Modifier.size(30.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(game.appName, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Text("AMBIENT THEME", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { num ->
                            Box(Modifier.weight(1f).height(25.dp).clip(RoundedCornerShape(7.dp)).background(if (selectedAnimation == num) accentPalettes[num - 1] else Color(0xFF202A23)).clickable { selectedAnimation = num }, contentAlignment = Alignment.Center) {
                                Text(num.toString(), color = if (selectedAnimation == num) GameSpaceDark else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            // CENTER: identity + live device stats
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("XSD", color = GameSpaceGreen, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
                Text("BOOST", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                Spacer(Modifier.height(5.dp))
                Text("GAMING COCKPIT", color = Color(0xFFB8C8BC), fontSize = 9.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonitorTile("CPU", "${stats.rawCpuUsagePercent?.toInt() ?: 0}%")
                    MonitorTile("RAM", "${stats.ramUsagePercent.toInt()}%")
                    MonitorTile("FPS", fps.toInt().toString())
                }
                Spacer(Modifier.height(12.dp))
                Text("SYSTEM READY", color = ambientAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            }
            // RIGHT: compact quick controls
            Surface(Modifier.width(170.dp).fillMaxHeight(), color = Color(0xAA0D1511), shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2239F58A)) ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("QUICK CONTROL", color = GameSpaceGreen, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Button(onClick = onRunSmartBoost, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GameSpaceGreen, contentColor = GameSpaceDark), contentPadding = PaddingValues(8.dp)) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text("SMART BOOST", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(onClick = { onToggleRogMonitor(!isRogOverlayActive) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(7.dp)) {
                        Icon(Icons.Default.Speed, null, tint = GameSpaceGreen, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(5.dp)); Text(if (isRogOverlayActive) "HUD ON" else "ROG HUD", color = Color.White, fontSize = 9.sp)
                    }
                    OutlinedButton(onClick = { showMonitorPanel = !showMonitorPanel }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(7.dp)) {
                        Icon(Icons.Default.Speed, null, tint = GameSpaceGreen, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(5.dp)); Text("MONITOR", color = Color.White, fontSize = 9.sp)
                    }
                    OutlinedButton(onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(7.dp)) {
                        Text("WI-FI", color = Color.White, fontSize = 9.sp)
                    }
                    OutlinedButton(onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(7.dp)) {
                        Text("DISPLAY", color = Color.White, fontSize = 9.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onExit, modifier = Modifier.size(36.dp).background(Color(0xFF24352A), CircleShape)) { Icon(Icons.Default.ArrowBack, "Exit", tint = GameSpaceGreen) }
                    Text("EXIT GAME SPACE", color = Color.Gray, fontSize = 8.sp)
                }
            }
        }

        if (showMonitorPanel) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 78.dp).widthIn(max = 310.dp),
                color = Color(0xEE101B17), shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6639F58A)),
                shadowElevation = 20.dp
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SYSTEM MONITOR", color = GameSpaceGreen, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.5.sp)
                            Text("Live device status", color = Color.LightGray, fontSize = 9.sp)
                        }
                        IconButton(onClick = { showMonitorPanel = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowBack, "Tutup monitor", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        MonitorTile("CPU", "${stats.rawCpuUsagePercent?.toInt() ?: 0}%", Modifier.weight(1f))
                        MonitorTile("RAM", "${stats.ramUsagePercent.toInt()}%", Modifier.weight(1f))
                        MonitorTile("FPS", "${fps.toInt()}", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onRunSmartBoost, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF254333), contentColor = GameSpaceGreen),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp)) {
                            Icon(Icons.Default.Bolt, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("SMART BOOST", fontSize = 9.sp)
                        }
                        FilledTonalButton(onClick = { onToggleRogMonitor(!isRogOverlayActive) }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF254333), contentColor = GameSpaceGreen),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp)) {
                            Icon(Icons.Default.Speed, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(if (isRogOverlayActive) "HUD ON" else "HUD", fontSize = 9.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) },
                            modifier = Modifier.weight(1f)) { Text("Wi-Fi Settings", color = Color.White, fontSize = 9.sp) }
                        TextButton(onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)) },
                            modifier = Modifier.weight(1f)) { Text("Display Settings", color = Color.White, fontSize = 9.sp) }
                    }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(18.dp).size(48.dp).clip(CircleShape)
                .background(if (showMonitorPanel) GameSpaceGreen else Color(0xDD101B17))
                .border(1.5.dp, Color(0xFF39F58A), CircleShape)
                .clickable { showMonitorPanel = !showMonitorPanel },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Speed, contentDescription = "Monitor floating", tint = if (showMonitorPanel) GameSpaceDark else GameSpaceGreen)
        }
        val entranceAlpha by animateFloatAsState(targetValue = if (showEntrance) 1f else 0f, animationSpec = tween(550, easing = FastOutSlowInEasing), label = "gs_intro_alpha")
        val entranceScale by animateFloatAsState(targetValue = if (showEntrance) 1f else 1.15f, animationSpec = tween(650, easing = FastOutSlowInEasing), label = "gs_intro_scale")
        if (entranceAlpha > 0.01f) {
            Box(Modifier.fillMaxSize().alpha(entranceAlpha).scale(entranceScale).background(Color(0xFF050907)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("XSD BOOST", color = GameSpaceGreen, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                    Text("GAME SPACE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Box(Modifier.width(170.dp).height(2.dp).background(Color(0x332FFFFFF))) {
                        Box(Modifier.fillMaxWidth().background(GameSpaceGreen))
                    }
                }
            }
        }
    }

    if (showGamePicker) {
        AlertDialog(
            onDismissRequest = { showGamePicker = false },
            containerColor = Color(0xFF151C18),
            title = { Text("Pilih Game", color = Color.White, fontWeight = FontWeight.Black) },
            text = {
                if (games.isEmpty()) Text("Belum ada game yang terdeteksi.", color = Color.LightGray)
                else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(games, key = { it.packageName }) { game ->
                        val chosen = game.packageName in selectedGamePackages
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                            selectedGamePackages = if (chosen) selectedGamePackages - game.packageName else selectedGamePackages + game.packageName
                        }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AndroidView(factory = { ctx ->
                                ImageView(ctx).apply {
                                    try { setImageDrawable(ctx.packageManager.getApplicationIcon(game.packageName)) }
                                    catch (_: Exception) { setImageDrawable(null) }
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                }
                            }, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(10.dp))
                            if (chosen) Icon(Icons.Default.Check, null, tint = GameSpaceGreen, modifier = Modifier.size(15.dp))
                            Text(game.appName, color = Color.White, modifier = Modifier.weight(1f))
                            Text(if (chosen) "SELECTED" else "ADD", color = GameSpaceGreen, fontSize = 9.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGamePicker = false }) { Text("DONE", color = GameSpaceGreen) } }
        )
    }
}

@Composable
private fun MonitorTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A2921)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color(0xFF9DB2A3), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}
