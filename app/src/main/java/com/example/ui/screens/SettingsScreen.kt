package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkSurfaceGradient
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

@Composable
fun SettingsScreen(
    onNavigateToDeveloperProfile: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Developer Profile Highlight Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceGradient)
                    .border(1.dp, PrimaryWhite.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable { onNavigateToDeveloperProfile() }
                    .padding(18.dp)
                    .testTag("developer_profile_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.xsd_developer_avatar),
                            contentDescription = "XSD BOOST anime mascot",
                            modifier = Modifier.size(54.dp).clip(CircleShape).border(1.5.dp, AccentGreen, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "DEVELOPER PROFILE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedText,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Muhammad Raihan Musalah",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryWhite
                            )
                            Text(
                                text = "HM MODZ TEAM • Cyber Security & Software Eng",
                                fontSize = 11.sp,
                                color = SecondaryWhite
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = PrimaryWhite
                    )
                }
            }
        }

        // Section: GAMING & UTILITY
        item {
            SettingsCategoryHeader("GAMING & UTILITY")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsNavigationItem(
                    icon = Icons.Default.Speed,
                    title = "Gaming Monitor & HUD",
                    subtitle = "Window overlay positioning & refresh rate sync"
                )
                SettingsNavigationItem(
                    icon = Icons.Default.Security,
                    title = "System Permissions Center",
                    subtitle = "Usage Stats, Overlay & Foreground notifications",
                    onClick = onNavigateToPermissions
                )
            }
        }

        // Section: MEDIA & AUDIO
        item {
            SettingsCategoryHeader("MEDIA & DYNAMIC ISLAND")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsNavigationItem(
                    icon = Icons.Default.MusicNote,
                    title = "Audio Engine Preferences",
                    subtitle = "Media stream buffer and repeat defaults"
                )
            }
        }

        // Section: ABOUT & LEGAL
        item {
            SettingsCategoryHeader("ABOUT & LEGAL")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsNavigationItem(
                    icon = Icons.Default.Info,
                    title = "Application Information",
                    subtitle = "XSD BOOST v1.0 • app.xsdboostsystem"
                )
                SettingsNavigationItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy & Safe Operation Policy",
                    subtitle = "Zero data harvesting, zero memory injection"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun DeveloperProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurfaceGradient)
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.xsd_developer_avatar),
                        contentDescription = "XSD BOOST developer mascot",
                        modifier = Modifier.size(112.dp).clip(CircleShape).border(2.dp, AccentGreen, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "XSD BOOST • DEVELOPER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Muhammad Raihan Musalah",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "HM MODZ TEAM",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedText,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "CORE EXPERTISE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExpertiseCard(
                    title = "Software Engineer",
                    description = "Specialized in native Android systems architecture, Kotlin Clean Architecture, MVVM design patterns, low-latency telemetry, and production performance optimization."
                )
                ExpertiseCard(
                    title = "Reverse Engineer",
                    description = "Deep expertise in binary analysis, Android APK decompilation, bytecode inspection, signature verification, and application security heuristics."
                )
                ExpertiseCard(
                    title = "Cyber Security",
                    description = "Penetration testing, application vulnerability assessment, threat modeling, cryptography, secure local storage, and zero-trust mobile architecture."
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceGradient)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ABOUT & APPLICATION CREDITS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedText,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "XSD BOOST is an advanced Android Gaming Performance & System Utility engineered by Muhammad Raihan Musalah and HM MODZ TEAM. Designed with a strict security-first mindset, modern monochrome aesthetics, and real device telemetry without false claims or memory tampering.",
                        fontSize = 12.sp,
                        color = SecondaryWhite,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MutedText,
        letterSpacing = 1.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceGradient)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = PrimaryWhite, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryWhite)
                    Text(text = subtitle, fontSize = 11.sp, color = MutedText)
                }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next", tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ExpertiseCard(title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceGradient)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryWhite
                )
            }
            Text(
                text = description,
                fontSize = 12.sp,
                color = SecondaryWhite,
                lineHeight = 17.sp
            )
        }
    }
}
