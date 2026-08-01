package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guarded_apps")
data class GuardedApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val iconType: String = "SOCIAL",
    val isGuarded: Boolean = true,
    val isReelsBlockEnabled: Boolean = true,
    val isMindfulOverlayEnabled: Boolean = true,
    val dailyLimitMinutes: Int = 15,
    val todayUsageMinutes: Int = 0,
    val enforcementMode: String = "REMOVE_ELEMENT", // "REMOVE_ELEMENT", "OVERLAY", or "BACK_GESTURE"
    val reelsTimerSeconds: Int = 0,           // 0 = instant block & hide
    val enableMainFeedNudge: Boolean = false,  // Enable pop-up on main feed
    val mainFeedTimerMinutes: Int = 3         // Minutes on main feed before pop-up
)

@Entity(tableName = "blocked_attempts")
data class BlockedAttempt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val featureBlocked: String,
    val actionTaken: String,
    val savedSeconds: Int = 300 // Estimated 5 minutes saved per block
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int,
    val sessionType: String = "DEEP_FOCUS",
    val completed: Boolean = true
)
