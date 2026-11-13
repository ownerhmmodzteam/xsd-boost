package com.example.core.system.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.media.MusicPlayerState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

@Composable
fun FloatingMusicDynamicIsland(
    musicState: MusicPlayerState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onHideIsland: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(if (isExpanded) 20.dp else 28.dp)
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF141414).copy(alpha = 0.90f),
            Color(0xFF050505).copy(alpha = 0.94f)
        )
    )

    Box(
        modifier = modifier
            .widthIn(min = 196.dp, max = 286.dp)
            .clip(pillShape)
            .background(bgBrush)
            .border(1.dp, Color.White.copy(alpha = 0.28f), pillShape)
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music Dynamic Island",
                        tint = if (musicState.isPlaying) AccentGreen else PrimaryWhite,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = musicState.currentTitle.ifBlank { "XSD Audio" },
                        color = PrimaryWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (musicState.isPlaying) "Playing" else "Paused",
                        color = if (musicState.isPlaying) AccentGreen else MutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(PrimaryWhite)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (musicState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (musicState.isPlaying) "Pause" else "Play",
                        tint = BgPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = SecondaryWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = SecondaryWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleRepeat,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (musicState.isLooping) AccentGreen else MutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(0.75.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onHideIsland)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Hide Island",
                            tint = SecondaryWhite,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hide Island",
                            color = SecondaryWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
