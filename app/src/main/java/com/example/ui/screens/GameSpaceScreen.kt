package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBg
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bundledVideos = remember {
        context.assets.list("game_backgrounds")?.filter { it.endsWith(".mp4", true) }?.sorted().orEmpty()
    }
    var selectedBundledIndex by remember { mutableIntStateOf(0) }
    var selectedExternalVideo by remember { mutableStateOf<Uri?>(null) }
    var videoVolume by remember { mutableStateOf(0f) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            selectedExternalVideo = uri
        }
    }
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) {
        player.repeatMode = Player.REPEAT_MODE_ONE
        onDispose { player.clearVideoSurface(); player.release() }
    }
    LaunchedEffect(selectedBundledIndex, selectedExternalVideo, bundledVideos) {
        val uri = selectedExternalVideo ?: bundledVideos.getOrNull(selectedBundledIndex)?.let { Uri.parse("asset:///game_backgrounds/$it") }
        if (uri != null) {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = true
        } else {
            player.stop()
            player.clearMediaItems()
        }
    }
    LaunchedEffect(videoVolume) { player.volume = videoVolume }

    Column(
        modifier = modifier.fillMaxSize().background(BgPrimary).verticalScroll(rememberScrollState()).padding(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(232.dp).clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF101713)).border(1.dp, BorderColor, RoundedCornerShape(24.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    val exo = player
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.rgb(12, 17, 15))
                        this.player = exo
                    }
                },
                update = { view -> if (view.player !== player) view.player = player },
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xAA050807), Color(0x33050807), Color(0xDD050807)))))
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).background(AccentGreen, CircleShape))
                    Text("XSD BOOST  /  GAME SPACE", color = PrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("YOUR GAME. YOUR SPACE.", color = PrimaryWhite, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (selectedExternalVideo != null) "CUSTOM VIDEO BACKGROUND"
                        else if (bundledVideos.isEmpty()) "ADD MP4 FILES TO game_backgrounds/"
                        else "BACKGROUND ${selectedBundledIndex + 1} / ${bundledVideos.size}",
                        color = SecondaryWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { videoPicker.launch(arrayOf("video/mp4", "video/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryWhite, contentColor = BgPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("CHANGE VIDEO", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { selectedExternalVideo = null; selectedBundledIndex = 0 }, enabled = bundledVideos.isNotEmpty()) {
                            Text("RESET", color = PrimaryWhite, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        if (bundledVideos.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                bundledVideos.forEachIndexed { index, name ->
                    Box(Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (selectedExternalVideo == null && selectedBundledIndex == index) AccentGreen else CardBg)
                        .clickable { selectedExternalVideo = null; selectedBundledIndex = index }
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(name, color = if (selectedExternalVideo == null && selectedBundledIndex == index) BgPrimary else SecondaryWhite, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg).border(1.dp, BorderColor, RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("VIDEO AUDIO", color = PrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${(videoVolume * 100).toInt()}%", color = AccentGreen, fontSize = 10.sp)
            }
            Slider(value = videoVolume, onValueChange = { videoVolume = it }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen))
            Text("Atur suara video atau mute agar tidak bertabrakan dengan musik Dynamic Island.", color = MutedText, fontSize = 10.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("CPU", stats.rawCpuUsagePercent?.let { "${it.toInt()}%" } ?: "N/A", Icons.Default.Speed, Modifier.weight(1f))
            MetricCard("RAM", "${stats.ramUsagePercent.toInt()}%", Icons.Default.Memory, Modifier.weight(1f))
            MetricCard("TEMP", stats.effectiveTempCelsius?.let { "${it.toInt()}°C" } ?: "N/A", Icons.Default.Thermostat, Modifier.weight(1f))
            MetricCard("FPS EST.", "${fps.toInt()}", Icons.Default.Bolt, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionCard("SMART BOOST", "Analisis memori dan pembersihan cache yang diizinkan Android", Icons.Default.Bolt, Modifier.weight(1f), onRunSmartBoost)
            ActionCard(if (isRogOverlayActive) "ROG MONITOR ON" else "FLOATING ROG MONITOR", "Tampilkan metrik perangkat di atas game", Icons.Default.Speed, Modifier.weight(1f)) { onToggleRogMonitor(!isRogOverlayActive) }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg).border(1.dp, BorderColor, RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("GAME LIBRARY", color = PrimaryWhite, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("${games.size} installed games", color = MutedText, fontSize = 10.sp)
                }
                Icon(Icons.Default.Gamepad, null, tint = AccentGreen)
            }
            if (games.isEmpty()) {
                Text("Belum ada game terdeteksi. Periksa izin visibilitas aplikasi.", color = MutedText, fontSize = 11.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 4.dp)) {
                    items(games, key = { it.packageName }) { game ->
                        Column(Modifier.width(148.dp).clip(RoundedCornerShape(14.dp)).background(ElevatedSurface).clickable { onLaunchGame(game.packageName) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Gamepad, null, tint = AccentGreen, modifier = Modifier.size(25.dp))
                            Text(game.appName, color = PrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                            Text("TAP TO PLAY", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg).border(1.dp, BorderColor, RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, tint = AccentGreen)
                Spacer(Modifier.width(7.dp))
                Text("DYNAMIC ISLAND MUSIC", color = PrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (musicState.currentTitle.isBlank()) "Belum ada musik diputar" else musicState.currentTitle, color = SecondaryWhite, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (musicState.isPlaying) "PLAYING" else "READY", color = if (musicState.isPlaying) AccentGreen else MutedText, fontSize = 9.sp)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(CardBg).border(1.dp, BorderColor, RoundedCornerShape(14.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
        Text(value, color = PrimaryWhite, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(label, color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(ElevatedSurface).border(1.dp, BorderColor, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, null, tint = AccentGreen, modifier = Modifier.size(19.dp))
        Text(title, color = PrimaryWhite, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(description, color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
    }
}
