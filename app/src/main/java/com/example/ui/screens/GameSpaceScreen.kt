package com.example.ui.screens

import android.content.Context
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
    val animationFiles = remember { (1..5).map { index -> "background_" + index.toString().padStart(2, '0') + ".mp4" } }
    val bundledFiles = remember { context.assets.list("game_backgrounds")?.toSet().orEmpty() }
    val prefs = remember { context.getSharedPreferences("xsd_game_space", Context.MODE_PRIVATE) }
    var selectedAnimation by rememberSaveable { mutableIntStateOf(prefs.getInt("selected_animation", 1).coerceIn(1, 5)) }
    var selectedGamePackages by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showGamePicker by remember { mutableStateOf(false) }
    var showMonitorPanel by rememberSaveable { mutableStateOf(false) }
    var showEntrance by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); showEntrance = false }
    val selectedGames = remember(games, selectedGamePackages) { games.filter { it.packageName in selectedGamePackages } }
    val player = remember(context) { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = 0f
        onDispose { player.stop(); player.clearVideoSurface(); player.release() }
    }
    LaunchedEffect(selectedAnimation) {
        prefs.edit().putInt("selected_animation", selectedAnimation).apply()
        val file = animationFiles[selectedAnimation - 1]
        if (file in bundledFiles) {
            player.setMediaItem(MediaItem.fromUri(Uri.parse("asset:///game_backgrounds/$file")))
            player.prepare()
            player.playWhenReady = true
        } else {
            player.stop()
            player.clearMediaItems()
        }
    }
    BackHandler(onBack = onExit)

    BoxWithConstraints(modifier.fillMaxSize().background(GameSpaceDark)) {
        val exoPlayer = player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.rgb(8, 13, 11))
                    this.player = exoPlayer
                }
            },
            update = { if (it.player !== exoPlayer) it.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color(0xAA040A07), Color(0x22040A07), Color(0xDD040A07))
        )))
        Column(
            Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit, modifier = Modifier.size(42.dp).background(Color(0x660B1710), CircleShape)) {
                    Icon(Icons.Default.ArrowBack, "Keluar Game Space", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("XSD BOOST", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 2.sp)
                    Text("GAME SPACE", color = GameSpaceGreen, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 2.2.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("CPU ${stats.rawCpuUsagePercent?.toInt() ?: 0}%   •   RAM ${stats.ramUsagePercent.toInt()}%   •   ${fps.toInt()} FPS",
                    color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CHANGE ANIMATION", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.6.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        animationFiles.forEachIndexed { index, _ ->
                            val number = index + 1
                            Box(
                                Modifier.size(width = 52.dp, height = 42.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedAnimation == number) GameSpaceGreen else Color(0xAA101A15))
                                    .border(1.dp, if (selectedAnimation == number) GameSpaceGreen else Color(0x55FFFFFF), RoundedCornerShape(12.dp))
                                    .clickable { selectedAnimation = number },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(number.toString().padStart(2, '0'), color = if (selectedAnimation == number) GameSpaceDark else Color.White,
                                    fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("MY GAMES", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                        Text("${selectedGames.size} SELECTED", color = GameSpaceGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showGamePicker = true }, modifier = Modifier.size(30.dp).background(Color(0xAA101A15), CircleShape)) {
                            Icon(Icons.Default.Add, "Tambah game", tint = GameSpaceGreen, modifier = Modifier.size(18.dp))
                        }
                        if (selectedGames.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(selectedGames, key = { it.packageName }) { game ->
                                    TextButton(onClick = { onLaunchGame(game.packageName) }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                                        AndroidView(factory = { ctx ->
                                        ImageView(ctx).apply {
                                            try { setImageDrawable(ctx.packageManager.getApplicationIcon(game.packageName)) }
                                            catch (_: Exception) { setImageDrawable(null) }
                                            scaleType = ImageView.ScaleType.FIT_CENTER
                                        }
                                    }, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(game.appName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRunSmartBoost, colors = ButtonDefaults.buttonColors(containerColor = GameSpaceGreen, contentColor = GameSpaceDark),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("SMART BOOST", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(onClick = { onToggleRogMonitor(!isRogOverlayActive) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)) {
                        Icon(Icons.Default.Speed, null, tint = GameSpaceGreen, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (isRogOverlayActive) "MONITOR ON" else "ROG MONITOR", color = Color.White, fontSize = 9.sp)
                    }
                }
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
        if (showEntrance) {
            val entranceAlpha by animateFloatAsState(targetValue = if (showEntrance) 1f else 0f, animationSpec = tween(550, easing = FastOutSlowInEasing), label = "gs_intro_alpha")
            val entranceScale by animateFloatAsState(targetValue = if (showEntrance) 1f else 1.15f, animationSpec = tween(650, easing = FastOutSlowInEasing), label = "gs_intro_scale")
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
                            Icon(if (chosen) Icons.Default.Check else Icons.Default.Gamepad, null, tint = if (chosen) GameSpaceGreen else Color.LightGray)
                            Spacer(Modifier.width(10.dp))
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
