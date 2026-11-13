package com.example.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "music_history")
data class MusicHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val gamePackageName: String = "",
    val durationSeconds: Long = 0,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sensitivity_presets")
data class SensitivityPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val gamePackageName: String = "",
    val sensitivityX: Int,
    val sensitivityY: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val boosterEnabled: Boolean = false,
    val sensitivityX: Int = 1,
    val sensitivityY: Int = 1,
    val crosshairEnabled: Boolean = false,
    val crosshairStyle: String = "CLASSIC_CROSS",
    val crosshairColorArgb: Long = 0xFFFFFFFF,
    val crosshairSizeDp: Float = 28f,
    val crosshairOpacity: Float = 0.95f,
    val overlayEnabled: Boolean = true,
    val overlaySide: String = "RIGHT",
    val isHandleHidden: Boolean = false,
    val handleYFraction: Float = 0.42f,
    val handleSizeDp: Float = 76f,
    val handleOpacity: Float = 0.90f,
    val longPressDurationMs: Long = 420L,
    val monitorIntervalMs: Long = 1000L,
    val graphicsProfileId: String = "ultra_smooth",
    val musicUrl: String = "",
    val musicTitle: String = "",
    val musicLooping: Boolean = false,
    val dynamicIslandEnabled: Boolean = true,
    val selectedOverlayTab: String = "PERFORMANCE",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface MusicHistoryDao {
    @Query("SELECT * FROM music_history ORDER BY playedAt DESC")
    fun getAllHistory(): Flow<List<MusicHistoryEntity>>

    @Query("SELECT * FROM music_history WHERE gamePackageName = :pkg OR gamePackageName = '' ORDER BY playedAt DESC")
    fun getHistoryForGame(pkg: String): Flow<List<MusicHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MusicHistoryEntity): Long

    @Query("DELETE FROM music_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM music_history")
    suspend fun clearAll()
}

@Dao
interface SensitivityPresetDao {
    @Query("SELECT * FROM sensitivity_presets ORDER BY createdAt DESC")
    fun observeAllPresets(): Flow<List<SensitivityPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: SensitivityPresetEntity): Long

    @Query("DELETE FROM sensitivity_presets WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles ORDER BY updatedAt DESC")
    fun observeAllProfiles(): Flow<List<GameProfileEntity>>

    @Query("SELECT * FROM game_profiles WHERE boosterEnabled = 1")
    fun observeEnabledProfiles(): Flow<List<GameProfileEntity>>

    @Query("SELECT * FROM game_profiles WHERE packageName = :packageName LIMIT 1")
    fun observeProfile(packageName: String): Flow<GameProfileEntity?>

    @Query("SELECT * FROM game_profiles WHERE packageName = :packageName LIMIT 1")
    suspend fun getProfile(packageName: String): GameProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: GameProfileEntity)

    @Query("DELETE FROM game_profiles WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}

@Database(
    entities = [MusicHistoryEntity::class, GameProfileEntity::class, SensitivityPresetEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun musicHistoryDao(): MusicHistoryDao
    abstract fun gameProfileDao(): GameProfileDao
    abstract fun sensitivityPresetDao(): SensitivityPresetDao
}
