package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class FocusRepository(private val dao: FocusDao) {
    val allGuardedApps: Flow<List<GuardedApp>> = dao.getAllGuardedApps()
    val recentBlockedAttempts: Flow<List<BlockedAttempt>> = dao.getRecentBlockedAttempts()
    val totalBlockedCount: Flow<Int> = dao.getTotalBlockedCount()
    val totalSavedSeconds: Flow<Int?> = dao.getTotalSavedSeconds()
    val recentFocusSessions: Flow<List<FocusSession>> = dao.getRecentFocusSessions()

    suspend fun updateGuardedApp(app: GuardedApp) {
        dao.updateApp(app)
    }

    suspend fun insertBlockedAttempt(attempt: BlockedAttempt) {
        dao.insertBlockedAttempt(attempt)
    }

    suspend fun insertFocusSession(session: FocusSession) {
        dao.insertFocusSession(session)
    }

    suspend fun initDefaultAppsIfEmpty() {
        val existingApps = dao.getAllGuardedApps().firstOrNull()
        if (!existingApps.isNullOrEmpty()) return

        val defaultApps = listOf(
            GuardedApp(
                packageName = "com.instagram.android",
                appName = "Instagram",
                iconType = "INSTAGRAM",
                isGuarded = true,
                isReelsBlockEnabled = true,
                isMindfulOverlayEnabled = true,
                dailyLimitMinutes = 15
            ),
            GuardedApp(
                packageName = "com.zhiliaoapp.musically",
                appName = "TikTok",
                iconType = "TIKTOK",
                isGuarded = true,
                isReelsBlockEnabled = true,
                isMindfulOverlayEnabled = true,
                dailyLimitMinutes = 10
            ),
            GuardedApp(
                packageName = "com.google.android.youtube",
                appName = "YouTube Shorts",
                iconType = "YOUTUBE",
                isGuarded = true,
                isReelsBlockEnabled = true,
                isMindfulOverlayEnabled = true,
                dailyLimitMinutes = 20
            ),
            GuardedApp(
                packageName = "com.twitter.android",
                appName = "X / Twitter",
                iconType = "TWITTER",
                isGuarded = false,
                isReelsBlockEnabled = false,
                isMindfulOverlayEnabled = true,
                dailyLimitMinutes = 20
            ),
            GuardedApp(
                packageName = "com.reddit.frontpage",
                appName = "Reddit",
                iconType = "REDDIT",
                isGuarded = false,
                isReelsBlockEnabled = false,
                isMindfulOverlayEnabled = true,
                dailyLimitMinutes = 25
            )
        )
        dao.insertAllApps(defaultApps)
    }
}
