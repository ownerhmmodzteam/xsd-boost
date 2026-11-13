package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.database.GameProfileEntity
import com.example.core.system.games.GameApp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBg
import com.example.ui.theme.DarkSurfaceGradient
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

@Composable
fun GameCenterScreen(
    games: List<GameApp>,
    gameProfiles: Map<String, GameProfileEntity>,
    hasUsageStatsPermission: Boolean,
    onRefreshPermissions: () -> Unit,
    onLaunchGame: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleGameBooster: (GameApp, Boolean) -> Unit,
    onUpdateGameSensitivity: (GameApp, Int, Int) -> Unit,
    onUpdateGameOverlayConfig: (GameApp, com.example.core.system.overlay.EdgeSide, Float, Float, Long) -> Unit = { _, _, _, _, _ -> },
    onSaveGameMusicUrl: (GameApp, String) -> Unit,
    onToggleGameDynamicIsland: (GameApp, Boolean) -> Unit,
    onSetGameHandleHidden: (GameApp, Boolean) -> Unit,
    onRestoreGameHandle: (GameApp) -> Unit,
    onResetGameProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var editingGame by remember { mutableStateOf<GameApp?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val registeredCount = games.count { gameProfiles[it.packageName]?.boosterEnabled == true }

    val filteredGames = games.filter { game ->
        val matchesSearch = game.appName.contains(searchQuery, ignoreCase = true) ||
            game.packageName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "REGISTERED" -> gameProfiles[game.packageName]?.boosterEnabled == true
            "FAVORITES" -> game.isFavorite
            "FREE_FIRE" -> game.packageName.contains("freefire", ignoreCase = true)
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed games...", color = MutedText, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MutedText,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("game_search_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryWhite,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = PrimaryWhite,
                unfocusedTextColor = SecondaryWhite,
                cursorColor = PrimaryWhite
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "REGISTERED", "FAVORITES", "FREE_FIRE").forEach { filterKey ->
                val label = when (filterKey) {
                    "ALL" -> "All (${games.size})"
                    "REGISTERED" -> "Booster Active ($registeredCount)"
                    "FAVORITES" -> "Favorites"
                    "FREE_FIRE" -> "Free Fire"
                    else -> filterKey
                }
                val isSelected = selectedFilter == filterKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryWhite else ElevatedSurface)
                        .clickable { selectedFilter = filterKey }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BgPrimary else SecondaryWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true

        // Smart Game Detection Permission Status Card
        if (!hasUsageStatsPermission || !hasOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AccentRed.copy(alpha = 0.14f))
                    .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Permission needed", tint = AccentAmber)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!hasUsageStatsPermission) "Izin Usage Access Diperlukan" else "Izin Overlay Diperlukan",
                            color = PrimaryWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (!hasUsageStatsPermission) {
                                "Agar overlay XSD BOOST hanya tampil saat game target aktif di foreground."
                            } else {
                                "Agar handle kanan dan Music Dynamic Island dapat tampil di atas game."
                            },
                            color = SecondaryWhite,
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = {
                            val intent = if (!hasUsageStatsPermission) {
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            } else {
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryWhite, contentColor = BgPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Izinkan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Smart Game Detection aktif ($registeredCount game terdaftar) — tahan handle tepi kiri/kanan saat in-game",
                    color = AccentGreen,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredGames.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "No Games",
                        tint = MutedText,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Games Detected",
                        color = PrimaryWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Install games or switch filter to view installed titles.",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 86.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredGames, key = { it.packageName }) { game ->
                    val profile = gameProfiles[game.packageName]
                    GameCardItem(
                        game = game,
                        profile = profile,
                        onLaunch = { onLaunchGame(game.packageName) },
                        onToggleFavorite = { onToggleFavorite(game.packageName) },
                        onToggleBooster = { enabled -> onToggleGameBooster(game, enabled) },
                        onOpenProfileDialog = { editingGame = game }
                    )
                }
            }
        }
    }

    editingGame?.let { targetGame ->
        val currentProfile = gameProfiles[targetGame.packageName]
            ?: GameProfileEntity(packageName = targetGame.packageName, appName = targetGame.appName)
        GameProfileConfigDialog(
            game = targetGame,
            profile = currentProfile,
            onDismiss = { editingGame = null },
            onUpdateSensitivity = { x, y -> onUpdateGameSensitivity(targetGame, x, y) },
            onUpdateOverlayConfig = { side, sizeDp, opacity, lpMs ->
                onUpdateGameOverlayConfig(targetGame, side, sizeDp, opacity, lpMs)
            },
            onLoadMusicUrl = { url -> onSaveGameMusicUrl(targetGame, url) },
            onToggleDynamicIsland = { enabled -> onToggleGameDynamicIsland(targetGame, enabled) },
            onToggleHandleHidden = { hidden -> onSetGameHandleHidden(targetGame, hidden) },
            onRestoreHandle = { onRestoreGameHandle(targetGame) },
            onResetProfile = { onResetGameProfile(targetGame.packageName) }
        )
    }
}

@Composable
fun GameCardItem(
    game: GameApp,
    profile: GameProfileEntity?,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleBooster: (Boolean) -> Unit,
    onOpenProfileDialog: () -> Unit
) {
    val isBoosterEnabled = profile?.boosterEnabled == true

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceGradient)
            .border(
                width = if (isBoosterEnabled) 1.2.dp else 1.dp,
                color = if (isBoosterEnabled) AccentGreen.copy(alpha = 0.65f) else BorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(12.dp)
            .testTag("game_card_${game.packageName}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                val bitmap = remember(game.icon) {
                    try {
                        game.icon?.toBitmap(96, 96)?.asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = game.appName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = game.appName,
                            tint = PrimaryWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (game.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (game.isFavorite) AccentAmber else MutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = game.appName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (profile != null) {
                        val sideShort = if (profile.overlaySide.equals("LEFT", ignoreCase = true)) "Left" else "Right"
                        "Sens ${profile.sensitivityX}:${profile.sensitivityY} • ${if (profile.isHandleHidden) "Hidden" else sideShort}"
                    } else {
                        "v${game.versionName} • ${(game.sizeBytes / (1024 * 1024))} MB"
                    },
                    fontSize = 10.sp,
                    color = if (isBoosterEnabled) AccentGreen else MutedText,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Per-Game Booster Toggle + Profile Config Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBoosterEnabled) "Booster ON" else "Booster OFF",
                    fontSize = 10.sp,
                    color = if (isBoosterEnabled) PrimaryWhite else MutedText,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = isBoosterEnabled,
                    onCheckedChange = onToggleBooster,
                    modifier = Modifier.testTag("booster_switch_${game.packageName}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryWhite,
                        checkedTrackColor = ElevatedSurface,
                        uncheckedThumbColor = MutedText,
                        uncheckedTrackColor = ElevatedSurface
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenProfileDialog)
                        .testTag("game_profile_btn_${game.packageName}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure Game Profile",
                        tint = PrimaryWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = onLaunch,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("launch_game_button_${game.packageName}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryWhite,
                        contentColor = BgPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Launch",
                            tint = BgPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("PLAY", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameProfileConfigDialog(
    game: GameApp,
    profile: GameProfileEntity,
    onDismiss: () -> Unit,
    onUpdateSensitivity: (Int, Int) -> Unit,
    onUpdateOverlayConfig: (com.example.core.system.overlay.EdgeSide, Float, Float, Long) -> Unit,
    onLoadMusicUrl: (String) -> Unit,
    onToggleDynamicIsland: (Boolean) -> Unit,
    onToggleHandleHidden: (Boolean) -> Unit,
    onRestoreHandle: () -> Unit,
    onResetProfile: () -> Unit
) {
    var sensX by remember(profile.sensitivityX) { mutableIntStateOf(profile.sensitivityX) }
    var sensY by remember(profile.sensitivityY) { mutableIntStateOf(profile.sensitivityY) }
    var musicUrl by remember(profile.musicUrl) { mutableStateOf(profile.musicUrl) }
    var overlaySide by remember(profile.overlaySide) {
        mutableStateOf(com.example.core.system.overlay.EdgeSide.fromString(profile.overlaySide))
    }
    var handleSizeDp by remember(profile.handleSizeDp) { androidx.compose.runtime.mutableFloatStateOf(profile.handleSizeDp) }
    var handleOpacity by remember(profile.handleOpacity) { androidx.compose.runtime.mutableFloatStateOf(profile.handleOpacity) }
    var longPressMs by remember(profile.longPressDurationMs) { androidx.compose.runtime.mutableLongStateOf(profile.longPressDurationMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgPrimary,
        title = {
            Column {
                Text(
                    text = game.appName,
                    color = PrimaryWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = game.packageName,
                    color = MutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Overlay Position (Per-Game)", color = PrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.core.system.overlay.EdgeSide.entries.forEach { side ->
                        val selected = overlaySide == side
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) PrimaryWhite else ElevatedSurface)
                                .border(1.dp, if (selected) PrimaryWhite else BorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    overlaySide = side
                                    onUpdateOverlayConfig(side, handleSizeDp, handleOpacity, longPressMs)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = side.label,
                                color = if (selected) BgPrimary else SecondaryWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text("Handle Size: ${handleSizeDp.toInt()} dp", color = SecondaryWhite, fontSize = 11.sp)
                Slider(
                    value = handleSizeDp,
                    onValueChange = {
                        handleSizeDp = it
                        onUpdateOverlayConfig(overlaySide, handleSizeDp, handleOpacity, longPressMs)
                    },
                    valueRange = 56f..120f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                )

                Text("Handle Opacity: ${(handleOpacity * 100).toInt()}%", color = SecondaryWhite, fontSize = 11.sp)
                Slider(
                    value = handleOpacity,
                    onValueChange = {
                        handleOpacity = it
                        onUpdateOverlayConfig(overlaySide, handleSizeDp, handleOpacity, longPressMs)
                    },
                    valueRange = 0.25f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                )

                Text("Long Press Duration: ${longPressMs} ms", color = SecondaryWhite, fontSize = 11.sp)
                Slider(
                    value = longPressMs.toFloat(),
                    onValueChange = {
                        longPressMs = it.toLong()
                        onUpdateOverlayConfig(overlaySide, handleSizeDp, handleOpacity, longPressMs)
                    },
                    valueRange = 250f..800f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                )

                Text("Sensitivity X: $sensX / 26", color = SecondaryWhite, fontSize = 11.sp)
                Slider(
                    value = sensX.toFloat(),
                    onValueChange = {
                        sensX = it.toInt().coerceIn(1, 26)
                        onUpdateSensitivity(sensX, sensY)
                    },
                    valueRange = 1f..26f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                )
                Text("Sensitivity Y: $sensY / 26", color = SecondaryWhite, fontSize = 11.sp)
                Slider(
                    value = sensY.toFloat(),
                    onValueChange = {
                        sensY = it.toInt().coerceIn(1, 26)
                        onUpdateSensitivity(sensX, sensY)
                    },
                    valueRange = 1f..26f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                )

                Text("Music URL Khusus Game Ini", color = SecondaryWhite, fontSize = 11.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = musicUrl,
                        onValueChange = { musicUrl = it },
                        placeholder = { Text("Paste music or media URL here...", fontSize = 11.sp, color = MutedText) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryWhite,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = PrimaryWhite,
                            unfocusedTextColor = SecondaryWhite
                        )
                    )
                    Button(
                        onClick = { onLoadMusicUrl(musicUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryWhite, contentColor = BgPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Load", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Music Dynamic Island", color = PrimaryWhite, fontSize = 12.sp)
                    Switch(
                        checked = profile.dynamicIslandEnabled,
                        onCheckedChange = onToggleDynamicIsland,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryWhite,
                            checkedTrackColor = ElevatedSurface
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hidden Handle Mode", color = PrimaryWhite, fontSize = 12.sp)
                    Switch(
                        checked = profile.isHandleHidden,
                        onCheckedChange = onToggleHandleHidden,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryWhite,
                            checkedTrackColor = ElevatedSurface
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRestoreHandle,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface, contentColor = PrimaryWhite),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Pulihkan Handle", fontSize = 10.sp)
                    }
                    Button(
                        onClick = onResetProfile,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.25f), contentColor = PrimaryWhite),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Profile", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = PrimaryWhite, fontWeight = FontWeight.Bold)
            }
        }
    )
}
