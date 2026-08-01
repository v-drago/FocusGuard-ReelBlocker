package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Query("SELECT * FROM guarded_apps")
    fun getAllGuardedApps(): Flow<List<GuardedApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: GuardedApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllApps(apps: List<GuardedApp>)

    @Update
    suspend fun updateApp(app: GuardedApp)

    @Query("SELECT * FROM blocked_attempts ORDER BY timestamp DESC LIMIT 50")
    fun getRecentBlockedAttempts(): Flow<List<BlockedAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedAttempt(attempt: BlockedAttempt)

    @Query("SELECT COUNT(*) FROM blocked_attempts")
    fun getTotalBlockedCount(): Flow<Int>

    @Query("SELECT SUM(savedSeconds) FROM blocked_attempts")
    fun getTotalSavedSeconds(): Flow<Int?>

    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC LIMIT 20")
    fun getRecentFocusSessions(): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSession)
}
