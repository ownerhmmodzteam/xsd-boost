package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val SpaceBg = Color(0xFF080A10)
private val SpaceSurface = Color(0xFF131722)
private val SpaceElevated = Color(0xFF1B2230)
private val SpaceAccent = Color(0xFFFF334F)
private val SpaceTextMuted = Color(0xFF8D98AA)
private val SpaceBorder = Color(0xFF293142)

private data class GameSpaceVideo(val title: String, val url: String)

private val gameSpaceVideos = listOf(
    GameSpaceVideo("ROG RED", "https://raw.githubusercontent.com/ownerhmmodzteam/xsd-boost/b14ab10f71e51340b080935abed792fdc90ca6bd/app/src/main/assets/game_backgrounds/background_01.mp4"),
    GameSpaceVideo("NEON CORE", "https://raw.githubusercontent.com/ownerhmmodzteam/xsd-boost/b14ab10f71e51340b080935abed792fdc90ca6bd/app/src/main/assets/game_backgrounds/background_02.mp4"),
    GameSpaceVideo("CYBER FLOW", "https://raw.githubusercontent.com/ownerhmmodzteam/xsd-boost/b14ab10f71e51340b080935abed792fdc90ca6bd/app/src/main/assets/game_backgrounds/background_03.mp4"),
    GameSpaceVideo("DARK ENERGY", "https://raw.githubusercontent.com/ownerhmmodzteam/xsd-boost/b14ab10f71e51340b080935abed792fdc90ca6bd/app/src/main/assets/game_backgrounds/background_04.mp4"),
    GameSpaceVideo("NIGHT DRIVE", "https://raw.githubusercontent.com/ownerhmmodzteam/xsd-boost/b14ab10f71e51340b080935abed792fdc90ca6bd/app/src/main/assets/game_backgrounds/background_05.mp4")
)

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
    var selectedVideo by remember { mutableIntStateOf(prefs.getInt("selected_video", 0).coerceIn(0, gameSpaceVideos.lastIndex)) }
    var customVideo by remember { mutableStateOf<Uri?>(null) }
    var videoVolume by remember { mutableFloatStateOf(0f) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            customVideo = uri
        }
    }
    val player = remember(context) { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = videoVolume
        onDispose {
            player.stop()
            player.clearVideoSurface()
            player.release()
        }
    }

    LaunchedEffect(selectedVideo, customVideo) {
        val mediaUri = customVideo ?: Uri.parse(gameSpaceVideos[selectedVideo].url)
        player.setMediaItem(MediaItem.fromUri(mediaUri))
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(videoVolume) {
        player.volume = videoVolume
    }

    LaunchedEffect(selectedVideo) {
        prefs.edit().putInt("selected_video", selectedVideo).apply()
    }

    BackHandler(onBack = onExit)
    val activePlayer = player

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("XSD BOOST", color = SpaceAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("GAME SPACE", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Surface(
                modifier = Modifier.size(42.dp).clickable { onExit() },
                shape = CircleShape,
                color = SpaceSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Keluar Game Space", tint = Color.White)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF10131B))
                    .border(1.dp, SpaceBorder, RoundedCornerShape(22.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            setShutterBackgroundColor(android.graphics.Color.rgb(8, 10, 16))
                            this.player = activePlayer
                        }
                    },
                    update = { view ->
                        if (view.player !== activePlayer) view.player = activePlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color(0xAA05070C), Color(0x1105070C), Color(0xDD05070C))
                        )
                    )
                )
                Column(
                    Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).background(SpaceAccent, CircleShape))
                        Text("CINEMATIC GAME SPACE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("ENTER THE GAME.", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                        Text(
                            if (customVideo != null) "CUSTOM VIDEO BACKGROUND" else gameSpaceVideos[selectedVideo].title,
                            color = Color(0xFFE4E8F0), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { videoPicker.launch(arrayOf("video/mp4", "video/*")) },
                                colors = ButtonDefaults.buttonColors(containerColor = SpaceAccent, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("PILIH VIDEO", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { customVideo = null; selectedVideo = 0 },
                                enabled = customVideo != null
                            ) {
                                Text("RESET", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gameSpaceVideos.forEachIndexed { index, video ->
                    val selected = customVideo == null && selectedVideo == index
                    Surface(
                        modifier = Modifier.clickable { customVideo = null; selectedVideo = index },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) SpaceAccent else SpaceSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) SpaceAccent else SpaceBorder)
                    ) {
                        Text(
                            video.title,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SpaceSurface)
                .border(1.dp, SpaceBorder, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("BACKGROUND AUDIO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(if (videoVolume == 0f) "MUTED" else "${(videoVolume * 100).toInt()}%", color = SpaceAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = videoVolume,
                onValueChange = { videoVolume = it },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = SpaceAccent, activeTrackColor = SpaceAccent, inactiveTrackColor = SpaceBorder)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionHeading("SYSTEM STATUS", "LIVE DEVICE METRICS")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("CPU", stats.rawCpuUsagePercent?.let { "${it.toInt()}%" } ?: "N/A", Icons.Default.Speed, Modifier.weight(1f))
                MetricCard("RAM", "${stats.ramUsagePercent.toInt()}%", Icons.Default.Memory, Modifier.weight(1f))
                MetricCard("TEMP", stats.effectiveTempCelsius?.let { "${it.toInt()}°C" } ?: "N/A", Icons.Default.Thermostat, Modifier.weight(1f))
                MetricCard("FPS", fps.toInt().toString(), Icons.Default.Bolt, Modifier.weight(1f))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionHeading("PERFORMANCE", "QUICK GAME TOOLS")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                ActionCard(
                    "SMART BOOST",
                    "Jalankan optimasi sistem yang tersedia.",
                    Icons.Default.Bolt,
                    Modifier.weight(1f),
                    onRunSmartBoost
                )
                ActionCard(
                    if (isRogOverlayActive) "ROG HUD ACTIVE" else "FLOATING ROG HUD",
                    "Tampilkan monitor saat bermain.",
                    Icons.Default.Speed,
                    Modifier.weight(1f)
                ) { onToggleRogMonitor(!isRogOverlayActive) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionHeading("YOUR LIBRARY", "${games.size} INSTALLED GAMES")
            if (games.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SpaceSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
                ) {
                    Text("Belum ada game terdeteksi. Periksa izin visibilitas aplikasi.", color = SpaceTextMuted, fontSize = 11.sp, modifier = Modifier.padding(16.dp))
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 4.dp)) {
                    items(games, key = { it.packageName }) { game ->
                        Column(
                            Modifier.width(142.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SpaceSurface)
                                .border(1.dp, SpaceBorder, RoundedCornerShape(16.dp))
                                .clickable { onLaunchGame(game.packageName) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    ImageView(ctx).apply {
                                        try { setImageDrawable(ctx.packageManager.getApplicationIcon(game.packageName)) } catch (_: Exception) {}
                                        scaleType = ImageView.ScaleType.FIT_CENTER
                                    }
                                },
                                modifier = Modifier.size(38.dp)
                            )
                            Text(game.appName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("TAP TO PLAY", color = SpaceAccent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SpaceSurface)
                .border(1.dp, SpaceBorder, RoundedCornerShape(18.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, tint = SpaceAccent, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("DYNAMIC ISLAND MUSIC", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            }
            Text(
                if (musicState.currentTitle.isBlank()) "Belum ada musik diputar" else musicState.currentTitle,
                color = Color(0xFFD7DCE5), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(if (musicState.isPlaying) "PLAYING" else "READY", color = if (musicState.isPlaying) SpaceAccent else SpaceTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f))
        Text(subtitle, color = SpaceTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(SpaceSurface)
            .border(1.dp, SpaceBorder, RoundedCornerShape(14.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = SpaceAccent, modifier = Modifier.size(15.dp))
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(label, color = SpaceTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(SpaceSurface)
            .border(1.dp, SpaceBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(icon, null, tint = SpaceAccent, modifier = Modifier.size(19.dp))
        Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(description, color = SpaceTextMuted, fontSize = 9.sp, lineHeight = 13.sp)
    }
}
