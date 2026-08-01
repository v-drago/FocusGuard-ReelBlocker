package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.data.AppDatabase
import com.example.data.BlockedAttempt
import com.example.data.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FocusGuardAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var repository: FocusRepository

    private var windowManager: WindowManager? = null
    
    // Popup mindfulness overlay
    private var overlayView: View? = null
    private var isOverlayShowing = false

    // Direct Element Masking Overlay (hides Reels button in Instagram)
    private var elementMaskView: View? = null
    private var isElementMaskShowing = false
    private var currentMaskRect: Rect? = null

    private val handler = Handler(Looper.getMainLooper())

    // Timing & State management to avoid loops or spam
    private var reelsSessionStartTime = 0L
    private var isCurrentlyInReels = false
    private var hasPromptedForCurrentReelsSession = false

    private var mainFeedStartTime = 0L
    private var isCurrentlyInMainFeed = false
    private var hasPromptedForMainFeedSession = false

    private var lastActionTime = 0L

    companion object {
        private const val ACTION_COOLDOWN_MS = 15_000L // 15 seconds minimum between automated actions

        var isServiceRunning = false
            private set
        var lastInterceptedApp = ""
            private set
        var totalBlocksThisSession = 0
            private set

        private const val TAG = "FocusGuardService"
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        val database = AppDatabase.getDatabase(this)
        repository = FocusRepository(database.focusDao())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "FocusGuardAccessibilityService created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 150
        }
        serviceInfo = info
        Log.d(TAG, "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // We check if the active app is in our guarded apps list
        if (packageName == "com.instagram.android" ||
            packageName == "com.zhiliaoapp.musically" ||
            packageName == "com.google.android.youtube" ||
            packageName.contains("instagram") ||
            packageName.contains("tiktok")
        ) {
            val rootNode = rootInActiveWindow ?: return
            inspectAndGuardNode(rootNode, packageName)
        } else {
            // User left social media app, remove element mask & reset active timers
            handler.post { dismissElementMask() }
            resetTimers()
        }
    }

    private fun inspectAndGuardNode(rootNode: AccessibilityNodeInfo, packageName: String) {
        scope.launch {
            val apps = repository.allGuardedApps.firstOrNull() ?: emptyList()
            val appGuard = apps.find {
                it.packageName == packageName || (packageName.contains("instagram") && it.packageName.contains("instagram"))
            }

            if (appGuard == null || !appGuard.isGuarded) {
                handler.post { dismissElementMask() }
                return@launch
            }

            val now = System.currentTimeMillis()

            // 1. Direct Reels Button Masking (Remove Reels Button from Instagram UI)
            if (appGuard.isReelsBlockEnabled && (appGuard.enforcementMode == "REMOVE_ELEMENT" || appGuard.reelsTimerSeconds == 0)) {
                val reelsTabNode = findReelsButtonNode(rootNode)
                if (reelsTabNode != null) {
                    val rect = Rect()
                    reelsTabNode.getBoundsInScreen(rect)
                    if (rect.width() > 0 && rect.height() > 0) {
                        handler.post { maskReelsButton(rect) }
                    }
                }
            } else {
                handler.post { dismissElementMask() }
            }

            // 2. Direct Navigation Interception & Reels Viewer Blocker
            val isInReelsViewer = isReelsViewerActive(rootNode)

            if (isInReelsViewer && appGuard.isReelsBlockEnabled) {
                isCurrentlyInMainFeed = false
                hasPromptedForMainFeedSession = false

                if (appGuard.enforcementMode == "REMOVE_ELEMENT" || appGuard.reelsTimerSeconds == 0) {
                    // INSTANT REMOVAL / REDIRECTION: Don't show pop-up dialog, immediately close Reels player!
                    triggerInstantReelsBlock(appGuard, packageName)
                } else {
                    // Standard timer pop-up mode
                    if (!isCurrentlyInReels) {
                        isCurrentlyInReels = true
                        reelsSessionStartTime = now
                        hasPromptedForCurrentReelsSession = false
                    } else if (!hasPromptedForCurrentReelsSession) {
                        val timeInReelsSeconds = (now - reelsSessionStartTime) / 1000
                        val limitSeconds = appGuard.reelsTimerSeconds.coerceAtLeast(5)

                        if (timeInReelsSeconds >= limitSeconds) {
                            hasPromptedForCurrentReelsSession = true
                            triggerMindfulnessAction(appGuard, packageName, "Reels / Shorts Feed (${timeInReelsSeconds}s continuous)")
                        }
                    }
                }
            } else {
                // Not in active Reels viewer (e.g. on Main Feed or Home Screen)
                isCurrentlyInReels = false
                hasPromptedForCurrentReelsSession = false

                if (appGuard.enableMainFeedNudge) {
                    if (!isCurrentlyInMainFeed) {
                        isCurrentlyInMainFeed = true
                        mainFeedStartTime = now
                        hasPromptedForMainFeedSession = false
                    } else if (!hasPromptedForMainFeedSession) {
                        val timeInFeedMinutes = ((now - mainFeedStartTime) / 1000) / 60
                        val limitMinutes = appGuard.mainFeedTimerMinutes.coerceAtLeast(1)

                        if (timeInFeedMinutes >= limitMinutes) {
                            hasPromptedForMainFeedSession = true
                            triggerMindfulnessAction(appGuard, packageName, "Main Feed ($timeInFeedMinutes mins spent)")
                        }
                    }
                }
            }
        }
    }

    private fun findReelsButtonNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        val reelsTabKeywords = listOf("reels_tab", "clips_tab", "shorts_tab", "tab_reels", "tab_clips", "reels tab", "clips tab")

        for (kw in reelsTabKeywords) {
            if (viewId.contains(kw) || contentDesc.contains(kw) || text.contains(kw)) {
                return node
            }
        }

        if ((contentDesc == "reels" || contentDesc == "reel" || text == "reels" || text == "reel") && node.isClickable) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findReelsButtonNode(child)
            if (result != null) return result
        }

        return null
    }

    private fun maskReelsButton(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted for element mask.")
            return
        }

        if (currentMaskRect != null && currentMaskRect == rect && isElementMaskShowing) {
            return // Position hasn't changed
        }

        currentMaskRect = rect

        try {
            val params = WindowManager.LayoutParams(
                rect.width(),
                rect.height(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.LEFT
                x = rect.left
                y = rect.top
            }

            if (elementMaskView == null) {
                val maskLayout = FrameLayout(this).apply {
                    // Dark slate background matching Instagram bottom navigation bar to obscure the Reels button
                    setBackgroundColor(0xFF121212.toInt())
                    contentDescription = "FocusGuard Reels Mask"
                }

                val title = TextView(this).apply {
                    text = "🚫"
                    textSize = 14f
                    gravity = Gravity.CENTER
                }
                maskLayout.addView(title)
                elementMaskView = maskLayout
            }

            if (!isElementMaskShowing) {
                windowManager?.addView(elementMaskView, params)
                isElementMaskShowing = true
                Log.d(TAG, "Reels button masked on screen at $rect")
            } else {
                windowManager?.updateViewLayout(elementMaskView, params)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mask Reels button: ${e.message}")
        }
    }

    private fun dismissElementMask() {
        if (isElementMaskShowing && elementMaskView != null) {
            try {
                windowManager?.removeView(elementMaskView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing element mask: ${e.message}")
            } finally {
                elementMaskView = null
                isElementMaskShowing = false
                currentMaskRect = null
            }
        }
    }

    private fun triggerInstantReelsBlock(appGuard: com.example.data.GuardedApp, packageName: String) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 1_500) return // Throttling

        lastActionTime = now
        totalBlocksThisSession++
        lastInterceptedApp = appGuard.appName

        scope.launch {
            repository.insertBlockedAttempt(
                BlockedAttempt(
                    packageName = packageName,
                    appName = appGuard.appName,
                    featureBlocked = "Reels Button & Viewer",
                    actionTaken = "Direct Element Removal & Back Gesture",
                    savedSeconds = 300
                )
            )
        }

        handler.post {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun triggerMindfulnessAction(appGuard: com.example.data.GuardedApp, packageName: String, triggerContext: String) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 10_000) return // Safety throttle

        lastActionTime = now
        totalBlocksThisSession++
        lastInterceptedApp = appGuard.appName

        scope.launch {
            repository.insertBlockedAttempt(
                BlockedAttempt(
                    packageName = packageName,
                    appName = appGuard.appName,
                    featureBlocked = triggerContext,
                    actionTaken = if (appGuard.enforcementMode == "BACK_GESTURE") "Smart Back Gesture" else "Mindfulness Overlay Pop-up",
                    savedSeconds = 300
                )
            )
        }

        handler.post {
            if (appGuard.enforcementMode == "BACK_GESTURE") {
                performGlobalAction(GLOBAL_ACTION_BACK)
            } else {
                showMindfulnessPauseOverlay(appGuard.appName, triggerContext)
            }
        }
    }

    private fun isReelsViewerActive(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        val reelsViewerKeywords = listOf(
            "reel_viewer", "reels_viewer", "clips_video_container",
            "reel_video_view", "clips_swipe_refresh_layout", "shorts_player_container",
            "shorts_video_view", "tiktok_video_view"
        )

        for (kw in reelsViewerKeywords) {
            if (viewId.contains(kw) || contentDesc.contains(kw)) {
                return true
            }
        }

        if (contentDesc.contains("reels video") || contentDesc.contains("shorts player")) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (isReelsViewerActive(child)) {
                return true
            }
        }

        return false
    }

    private fun resetTimers() {
        isCurrentlyInReels = false
        hasPromptedForCurrentReelsSession = false
        isCurrentlyInMainFeed = false
        hasPromptedForMainFeedSession = false
    }

    private fun showMindfulnessPauseOverlay(appName: String, triggerContext: String) {
        if (isOverlayShowing) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted. Falling back to soft back gesture.")
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val overlay = FrameLayout(this).apply {
                setBackgroundColor(0xFA0F172A.toInt())
                setPadding(56, 56, 56, 56)
            }

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }

            val title = TextView(this).apply {
                text = "Take a Mindful Breath"
                textSize = 22f
                setTextColor(0xFF2DD4BF.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, 12)
            }

            val subtitle = TextView(this).apply {
                text = "FocusGuard detected extended usage of $appName ($triggerContext).\n\nTake a moment to check in with yourself before continuing."
                textSize = 14f
                setTextColor(0xFFF1F5F9.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }

            val buttonRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val returnHomeBtn = Button(this).apply {
                text = "Return Home"
                setBackgroundColor(0xFF0D9488.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setOnClickListener {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    dismissOverlay()
                }
            }

            val snoozeBtn = Button(this).apply {
                text = "Snooze 5m"
                setBackgroundColor(0xFF334155.toInt())
                setTextColor(0xFFCBD5E1.toInt())
                setOnClickListener {
                    dismissOverlay()
                }
            }

            buttonRow.addView(returnHomeBtn)
            val spacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(24, 1)
            }
            buttonRow.addView(spacer)
            buttonRow.addView(snoozeBtn)

            container.addView(title)
            container.addView(subtitle)
            container.addView(buttonRow)
            overlay.addView(container)

            overlayView = overlay
            windowManager?.addView(overlayView, params)
            isOverlayShowing = true

            handler.postDelayed({
                dismissOverlay()
            }, 10_000)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay: ${e.message}")
        }
    }

    private fun dismissOverlay() {
        if (isOverlayShowing && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing view: ${e.message}")
            } finally {
                overlayView = null
                isOverlayShowing = false
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        dismissOverlay()
        dismissElementMask()
        job.cancel()
    }
}
