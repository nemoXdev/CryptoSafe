package com.cryptosafe.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cryptosafe.app.data.Box
import com.cryptosafe.app.security.SecurePasswordStorage


sealed interface Screen {
    data object Home : Screen
    data object Boxes : Screen
    data object Encrypt : Screen
    data object Decrypt : Screen
    data object About : Screen
    data object Settings : Screen
    data object Help : Screen
    data object CreateBox : Screen
    data object Chat : Screen
    data object BoxSettings : Screen
}


class MainViewModel : ViewModel() {

    var mode by mutableStateOf<Screen>(Screen.Home)
        private set
    var selectedTab by mutableStateOf(0)
        private set
    var selectedBoxId by mutableLongStateOf(0L)
        private set
    var selectedBoxPassword by mutableStateOf("")
        private set
    
    var boxSessionCache by mutableStateOf<Map<Long, Pair<String, Long>>>(emptyMap())
        private set
    var boxPendingUnlock by mutableStateOf<Box?>(null)
        private set
    var boxPendingShareUnlock by mutableStateOf<Box?>(null)
        private set
    var sharedText by mutableStateOf<String?>(null)
        private set
    var showSharePicker by mutableStateOf(false)
        private set

    

    fun navigate(screen: Screen) {
        mode = screen
    }

    fun selectTab(tab: Int) {
        selectedTab = tab
    }

    fun selectBox(boxId: Long) {
        selectedBoxId = boxId
    }

    fun goHome() {
        mode = Screen.Home
        selectedTab = 0
    }

    fun goBoxes() {
        mode = Screen.Boxes
        selectedTab = 1
    }

    fun goSettings() {
        mode = Screen.Settings
    }

    
    fun navigateBack() {
        when (mode) {
            Screen.Encrypt, Screen.Decrypt, Screen.About, Screen.Settings -> goHome()
            Screen.Help -> goSettings()
            Screen.CreateBox -> goBoxes()
            Screen.Chat, Screen.BoxSettings -> {
                clearBoxSession()
                goBoxes()
            }
            else -> goHome()
        }
    }

    

    fun onSharedTextReceived(text: String) {
        sharedText = text
        showSharePicker = true
    }

    fun consumeSharedText() {
        sharedText = null
    }

    fun hideSharePicker() {
        showSharePicker = false
    }

    

    
    fun onLocked() {
        boxPendingUnlock = null
        boxPendingShareUnlock = null
    }

    fun clearBoxSession() {
        selectedBoxPassword = ""
    }

    
    fun clearSessionCache() {
        selectedBoxPassword = ""
        boxSessionCache = emptyMap()
        boxPendingUnlock = null
        boxPendingShareUnlock = null
    }

    fun onBoxPasswordChanged(boxId: Long) {
        boxSessionCache = boxSessionCache - boxId
    }

    

    fun openBox(box: Box) {
        val password = passwordFor(box)
        if (password != null) {
            selectedBoxId = box.id
            selectedBoxPassword = password
            mode = Screen.Chat
        } else {
            boxPendingUnlock = box
        }
    }

    fun requestBoxUnlock(box: Box) {
        boxPendingUnlock = box
    }

    fun dismissPendingUnlock() {
        boxPendingUnlock = null
    }

    fun unlockBox(box: Box, password: String) {
        selectedBoxId = box.id
        selectedBoxPassword = password
        cachePassword(box, password)
        boxPendingUnlock = null
        mode = Screen.Chat
    }

    fun requestBoxShareUnlock(box: Box) {
        boxPendingShareUnlock = box
    }

    fun dismissPendingShareUnlock() {
        boxPendingShareUnlock = null
    }

    
    fun openChatWith(box: Box, password: String) {
        selectedBoxId = box.id
        selectedBoxPassword = password
        cachePassword(box, password)
        boxPendingShareUnlock = null
        mode = Screen.Chat
    }

    

    private fun cachePassword(box: Box, password: String) {
        if (box.lockMode == "timed" || box.lockMode == "never") {
            boxSessionCache = boxSessionCache + (box.id to (password to System.currentTimeMillis()))
        }
        if (box.lockMode == "permanent") {
            SecurePasswordStorage.saveBoxPassword(box.id, password)
            boxSessionCache = boxSessionCache + (box.id to (password to System.currentTimeMillis()))
        }
    }

    
    fun cachedPasswordFor(box: Box): String? = passwordFor(box)

    private fun passwordFor(box: Box): String? {
        val cached = boxSessionCache[box.id]
        var password = cached?.first
        if (password == null && box.lockMode == "permanent") {
            val saved = SecurePasswordStorage.getBoxPassword(box.id)
            if (saved != null) {
                password = saved
                boxSessionCache = boxSessionCache + (box.id to (saved to System.currentTimeMillis()))
            }
        }
        val stillValid = when (box.lockMode) {
            "never" -> cached != null
            "timed" -> cached != null &&
                (System.currentTimeMillis() - cached.second) < (box.lockTimeoutMinutes ?: 5) * 60_000L
            "permanent" -> password != null
            else -> false 
        }
        return if (stillValid) password else null
    }

    override fun onCleared() {
        selectedBoxPassword = ""
        boxSessionCache = emptyMap()
        super.onCleared()
    }
}
