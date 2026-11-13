package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.database.MusicHistoryEntity
import com.example.core.system.hardware.SystemStats
import com.example.core.system.media.MusicPlayerState
import com.example.core.system.overlay.formatDurationMs
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkSurfaceGradient
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

enum class IslandMode {
    STATS,
    MUSIC,
    BOOST_PROGRESS
}

@Composable
fun DynamicIsland(
    stats: SystemStats,
    fps: Float,
    musicState: MusicPlayerState,
    musicHistory: List<MusicHistoryEntity> = emptyList(),
    onMusicPlay: () -> Unit = {},
    onMusicPause: () -> Unit = {},
    onMusicStop: () -> Unit = {},
    onMusicPrevious: () -> Unit = {},
    onMusicNext: () -> Unit = {},
    onMusicPlayPause: () -> Unit,
    onMusicRepeat: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit = {},
    onPlayUrl: (String) -> Unit,
    onPlayLocalUri: (android.net.Uri, String?) -> Unit = { _, _ -> },
    onPlayHistoryItem: (MusicHistoryEntity) -> Unit = {},
    onOpenYouTube: (String) -> Unit = {},
    onOpenSpotify: (String) -> Unit = {},
    boostStatusMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(IslandMode.STATS) }
    var inputUrl by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val localAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = try {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val column = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (column >= 0) cursor.getString(column) else null
                    } else null
                }
            } catch (_: Exception) { null }
            onPlayLocalUri(uri, name)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                .clip(RoundedCornerShape(if (isExpanded) 24.dp else 32.dp))
                .background(DarkSurfaceGradient)
                .border(1.dp, BorderColor, RoundedCornerShape(if (isExpanded) 24.dp else 32.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("dynamic_island")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(if (isExpanded) 0.96f else 0.88f)
            ) {
                // Collapsed Compact Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (musicState.isPlaying) AccentCyan else AccentGreen)
                        )
                        Text(
                            text = if (boostStatusMessage != null) "BOOSTING" else if (musicState.isPlaying) "STREAM" else "XSD CORE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${fps.toInt()} FPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryWhite,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(text = "|", color = MutedText, fontSize = 11.sp)
                        Text(
                            text = "CPU ${stats.cpuUsagePercent.toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SecondaryWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Island",
                        tint = MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Expanded Panel Content
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IslandTabItem(
                                title = "Telemetry",
                                isSelected = currentMode == IslandMode.STATS,
                                onClick = { currentMode = IslandMode.STATS }
                            )
                            IslandTabItem(
                                title = "Music Player",
                                isSelected = currentMode == IslandMode.MUSIC,
                                onClick = { currentMode = IslandMode.MUSIC }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (currentMode) {
                            IslandMode.STATS -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MetricRow(label = "RAM Usage", value = "${stats.ramUsagePercent.toInt()}% (${stats.ramUsedBytes / (1024 * 1024)} MB)")
                                    MetricRow(label = "Display Hz", value = "${stats.refreshRate.toInt()} Hz Refresh")
                                    MetricRow(label = "Thermals", value = stats.effectiveTempCelsius?.let { "${it.toInt()} °C" } ?: "N/A")
                                    MetricRow(label = "Battery", value = "${stats.batteryLevel}% ${if (stats.isCharging) "(Charging)" else ""}")
                                }
                            }
                            IslandMode.MUSIC -> {
                                val ctx = LocalContext.current
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ElevatedSurface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!musicState.artworkUri.isNullOrBlank() || musicState.artworkBytes != null) {
                                                AsyncImage(
                                                    model = musicState.artworkBytes ?: musicState.artworkUri,
                                                    contentDescription = "Track Artwork",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = "Track",
                                                    tint = PrimaryWhite,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = musicState.currentTitle,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PrimaryWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${musicState.sourceName} • ${if (musicState.isBuffering) "Buffering..." else musicState.playbackStatus}",
                                                fontSize = 10.sp,
                                                color = when {
                                                    musicState.isPlaying -> AccentGreen
                                                    musicState.isBuffering -> AccentCyan
                                                    musicState.errorMessage != null -> AccentAmber
                                                    else -> MutedText
                                                },
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (musicState.errorMessage != null) {
                                        Text(
                                            text = musicState.errorMessage,
                                            fontSize = 11.sp,
                                            color = AccentAmber
                                        )
                                    }

                                    if (musicState.isYouTubeUrl && musicState.youtubeUrlToOpen != null) {
                                        TextButton(
                                            onClick = { onOpenYouTube(musicState.youtubeUrlToOpen) }
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = PrimaryWhite, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Buka di Aplikasi YouTube", color = PrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (musicState.isSpotifyUrl && musicState.spotifyUrlToOpen != null) {
                                        TextButton(
                                            onClick = { onOpenSpotify(musicState.spotifyUrlToOpen) }
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = PrimaryWhite, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Buka di Aplikasi Spotify", color = PrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (musicState.durationMs > 0L) {
                                        val progress = (musicState.currentPositionMs.toFloat() / musicState.durationMs.toFloat()).coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = PrimaryWhite,
                                            trackColor = ElevatedSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(formatDurationMs(musicState.currentPositionMs), color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            Text(formatDurationMs(musicState.durationMs), color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    } else if (musicState.isBuffering) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = PrimaryWhite,
                                            trackColor = ElevatedSurface
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = onMusicPrevious, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = PrimaryWhite, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = onMusicPlay,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryWhite)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = BgPrimary, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = onMusicPause, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = PrimaryWhite, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = onMusicStop, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = SecondaryWhite, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = onMusicNext, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = PrimaryWhite, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = onMusicRepeat, modifier = Modifier.size(34.dp)) {
                                            Icon(
                                                Icons.Default.Repeat,
                                                contentDescription = "Repeat",
                                                tint = if (musicState.isLooping) AccentGreen else MutedText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(onClick = { showHistory = !showHistory }, modifier = Modifier.size(34.dp)) {
                                            Icon(
                                                Icons.Default.History,
                                                contentDescription = "History",
                                                tint = if (showHistory) AccentCyan else SecondaryWhite,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = SecondaryWhite, modifier = Modifier.size(16.dp))
                                        Slider(
                                            value = musicState.volume,
                                            onValueChange = onMusicVolumeChange,
                                            valueRange = 0f..1f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                                        )
                                        Text("${(musicState.volume * 100).toInt()}%", color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }

                                    // URL Input row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = inputUrl,
                                            onValueChange = { inputUrl = it },
                                            placeholder = { Text("Paste music or media URL here...", fontSize = 11.sp, color = MutedText) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryWhite,
                                                unfocusedBorderColor = BorderColor,
                                                focusedTextColor = PrimaryWhite,
                                                unfocusedTextColor = SecondaryWhite,
                                                cursorColor = PrimaryWhite
                                            ),
                                            singleLine = true,
                                            textStyle = TextStyle(fontSize = 11.sp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(
                                            onClick = {
                                                if (inputUrl.isNotBlank()) {
                                                    onPlayUrl(inputUrl)
                                                }
                                            }
                                        ) {
                                            Text("Load", color = PrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        TextButton(
                                            onClick = { localAudioPicker.launch(arrayOf("audio/*")) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = AccentCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Pilih Musik dari Perangkat",
                                                color = AccentCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    if (showHistory) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(ElevatedSurface)
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (musicHistory.isEmpty()) {
                                                Text("Belum ada riwayat lagu.", color = MutedText, fontSize = 10.sp)
                                            } else {
                                                musicHistory.take(4).forEach { track ->
                                                    Text(
                                                        text = track.title,
                                                        color = PrimaryWhite,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                inputUrl = track.url
                                                                onPlayHistoryItem(track)
                                                            }
                                                            .padding(vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            IslandMode.BOOST_PROGRESS -> {
                                Text(
                                    text = boostStatusMessage ?: "Optimization in progress...",
                                    fontSize = 12.sp,
                                    color = SecondaryWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PrimaryWhite else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) BgPrimary else MutedText
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MutedText, fontSize = 11.sp)
        Text(text = value, color = PrimaryWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}
