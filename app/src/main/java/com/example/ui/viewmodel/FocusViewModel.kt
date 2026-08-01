package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BlockedAttempt
import com.example.data.FocusDao
import com.example.data.FocusRepository
import com.example.data.GuardedApp
import com.example.service.FocusGuardAccessibilityService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FocusRepository

    init {
        val dao: FocusDao = AppDatabase.getDatabase(application).focusDao()
        repository = FocusRepository(dao)
        viewModelScope.launch {
            repository.initDefaultAppsIfEmpty()
        }
    }

    val guardedApps: StateFlow<List<GuardedApp>> = repository.allGuardedApps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentAttempts: StateFlow<List<BlockedAttempt>> = repository.recentBlockedAttempts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalBlockedCount: StateFlow<Int> = repository.totalBlockedCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalSavedMinutes: StateFlow<Int> = repository.totalSavedSeconds
        .map { seconds -> (seconds ?: 0) / 60 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isSimulatingOverlay = MutableStateFlow(false)
    val isSimulatingOverlay: StateFlow<Boolean> = _isSimulatingOverlay.asStateFlow()

    // Focus Session Timer state
    private val _timerSeconds = MutableStateFlow(1500) // 25 minutes default
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun toggleAppGuard(app: GuardedApp) {
        viewModelScope.launch {
            repository.updateGuardedApp(app.copy(isGuarded = !app.isGuarded))
        }
    }

    fun toggleReelsBlock(app: GuardedApp) {
        viewModelScope.launch {
            repository.updateGuardedApp(app.copy(isReelsBlockEnabled = !app.isReelsBlockEnabled))
        }
    }

    fun toggleMindfulOverlay(app: GuardedApp) {
        viewModelScope.launch {
            repository.updateGuardedApp(app.copy(isMindfulOverlayEnabled = !app.isMindfulOverlayEnabled))
        }
    }

    fun updateDailyLimit(app: GuardedApp, newLimitMinutes: Int) {
        viewModelScope.launch {
            repository.updateGuardedApp(app.copy(dailyLimitMinutes = newLimitMinutes))
        }
    }

    fun updateReelsTimer(app: GuardedApp, seconds: Int) {
        viewModelScope.launch {
            repository.updateGuardedApp(app.copy(reelsTimerSeconds = seconds))
        }
    }

    fun toggleMainFeedNudge(app: GuardedApp) {
        viewModelScope.launch {
            repository.updateGuardedApp(app.copy(enableMainFeedNudge = !app.enableMainFeedNudge))
        }
    }

    fun updateMainFeedTimer(app: GuardedApp, minutes: Int) {
        viewModelScope.launch {
            repository.updateGuardedApp(app.copy(mainFeedTimerMinutes = minutes))
        }
    }

    fun updateEnforcementMode(app: GuardedApp, mode: String) {
        viewModelScope.launch {
            repository.updateGuardedApp(app.copy(enforcementMode = mode))
        }
    }

    fun simulateReelsBlockAttempt(appName: String = "Instagram") {
        viewModelScope.launch {
            repository.insertBlockedAttempt(
                BlockedAttempt(
                    packageName = "com.instagram.android",
                    appName = appName,
                    featureBlocked = "Reels Feed",
                    actionTaken = "MINDFUL_OVERLAY",
                    savedSeconds = 300
                )
            )
            _isSimulatingOverlay.value = true
        }
    }

    fun dismissSimulatedOverlay() {
        _isSimulatingOverlay.value = false
    }

    fun startFocusSession(minutes: Int = 25) {
        _timerSeconds.value = minutes * 60
        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _timerSeconds.value -= 1
            }
            if (_timerSeconds.value == 0) {
                _isTimerRunning.value = false
            }
        }
    }

    fun pauseFocusSession() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetFocusSession() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerSeconds.value = 1500
    }

    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + getApplication<Application>().packageName)
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        return FocusGuardAccessibilityService.isServiceRunning
    }
}
