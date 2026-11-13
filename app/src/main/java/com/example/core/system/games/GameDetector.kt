package com.example.core.system.games

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import java.io.File

data class GameApp(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val sizeBytes: Long,
    val isCategoryGame: Boolean,
    val isFavorite: Boolean = false,
    val icon: Drawable? = null
)

class GameDetector(private val context: Context) {

    private val knownGamePackages = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "com.tencent.ig",
        "com.mobile.legends",
        "com.pubg.imobile",
        "com.pubg.krmobile",
        "com.vng.pubgmobile",
        "com.miHoYo.GenshinImpact",
        "com.roblox.client",
        "com.epicgames.portal",
        "com.garena.game.codm",
        "com.riotgames.league.wildrift",
        "com.ea.gp.apexlegendsmobilefps",
        "com.supercell.clashofclans",
        "com.supercell.clashroyale",
        "com.supercell.brawlstars",
        "com.innersloth.spacemafia",
        "com.mojang.minecraftpe"
    )

    fun detectInstalledGames(favoritePackages: Set<String> = emptySet()): List<GameApp> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val gameList = mutableListOf<GameApp>()
        val seenPackages = mutableSetOf<String>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val isGameCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME
                } else {
                    @Suppress("DEPRECATION")
                    (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                }

                val isKnownGame = knownGamePackages.contains(pkg) ||
                        pkg.contains("game", ignoreCase = true) ||
                        pkg.contains("play", ignoreCase = true) ||
                        isGameCategory

                if (isKnownGame) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val packageInfo = pm.getPackageInfo(pkg, 0)
                    val versionName = packageInfo.versionName ?: "1.0"
                    val sourceDir = appInfo.sourceDir
                    val sizeBytes = if (sourceDir != null) File(sourceDir).length() else 0L
                    val icon = pm.getApplicationIcon(appInfo)

                    gameList.add(
                        GameApp(
                            packageName = pkg,
                            appName = appName,
                            versionName = versionName,
                            sizeBytes = sizeBytes,
                            isCategoryGame = isGameCategory,
                            isFavorite = favoritePackages.contains(pkg),
                            icon = icon
                        )
                    )
                }
            } catch (_: Exception) {
                // Application uninstalled or inaccessible
            }
        }

        // Sort: favorites first, then alphabetically
        return gameList.sortedWith(
            compareByDescending<GameApp> { it.isFavorite }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun launchGame(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
