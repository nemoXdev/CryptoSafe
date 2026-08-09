package com.cryptosafe.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import com.cryptosafe.app.data.AppDatabase
import com.cryptosafe.app.data.Box
import com.cryptosafe.app.screens.AboutScreen
import com.cryptosafe.app.screens.BoxesScreen
import com.cryptosafe.app.screens.BoxSettingsScreen
import com.cryptosafe.app.screens.BoxUnlockDialog
import com.cryptosafe.app.screens.ChatScreen
import com.cryptosafe.app.screens.CreateBoxScreen
import com.cryptosafe.app.screens.DecryptScreen
import com.cryptosafe.app.screens.EncryptScreen
import com.cryptosafe.app.screens.HelpScreen
import com.cryptosafe.app.screens.HomeButtons
import com.cryptosafe.app.screens.LockScreen
import com.cryptosafe.app.screens.SettingsScreen
import com.cryptosafe.app.screens.SharePickerDialog
import com.cryptosafe.app.screens.saveSharedTextToBox
import com.cryptosafe.app.security.SecurePasswordStorage
import com.cryptosafe.app.theme.CryptoSafeTheme
import com.cryptosafe.app.workers.AutoDeleteWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    private var isLocked by mutableStateOf(false)

    
    private val vm: MainViewModel by viewModels()

    companion object {
        private const val ENABLE_SCREENSHOT_PROTECTION = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalizationManager.initialize(this)
        SecurePasswordStorage.initialize(this)
        DiagnosticsLogger.initialize(this)
        if (ENABLE_SCREENSHOT_PROTECTION && SecurePasswordStorage.isScreenshotProtectionEnabled()) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        AutoDeleteWorker.schedule(this)

        
        if (SecurePasswordStorage.hasPin()) {
            val timer = SecurePasswordStorage.getAutoLockTimer()
            if (timer == 0) {
                isLocked = true
            } else {
                val lastStop = SecurePasswordStorage.getLastStopTime()
                if (lastStop > 0 && System.currentTimeMillis() - lastStop >= timer * 1000L) {
                    isLocked = true
                    SecurePasswordStorage.setLastStopTime(0L)
                }
            }
            
            
            if (SecurePasswordStorage.isLockedOnExit()) {
                isLocked = true
            }
        }

        lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                    DiagnosticsLogger.markCleanExit(this)
                }
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    if (SecurePasswordStorage.hasPin()) {
                        val timer = SecurePasswordStorage.getAutoLockTimer()
                        if (timer == 0) {
                            isLocked = true
                            vm.clearSessionCache()
                        } else {
                            SecurePasswordStorage.setLastStopTime(System.currentTimeMillis())
                        }
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    if (SecurePasswordStorage.hasPin() && !isLocked) {
                        val timer = SecurePasswordStorage.getAutoLockTimer()
                        if (SecurePasswordStorage.isLockedOnExit()) {
                            isLocked = true
                            vm.clearSessionCache()
                        } else if (timer > 0) {
                            val lastStop = SecurePasswordStorage.getLastStopTime()
                            if (lastStop > 0 && System.currentTimeMillis() - lastStop >= timer * 1000L) {
                                isLocked = true
                                vm.clearSessionCache()
                                SecurePasswordStorage.setLastStopTime(0L)
                            }
                        }
                    }
                }
                else -> {}
            }
        })

        setContent {
            CryptoSafeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    var database by remember { mutableStateOf<AppDatabase?>(null) }
                    var dataLocked by remember { mutableStateOf(false) }
                    var dataLockedDismissed by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        val (db, error) = withContext(Dispatchers.IO) {
                            try {
                                if (!SecurePasswordStorage.canSafelyOpenExistingDb(this@MainActivity)) {
                                    
                                    
                                    Pair<AppDatabase?, String?>(null, "key_missing")
                                } else {
                                    val passphrase = SecurePasswordStorage.getOrCreateDatabasePassphrase()
                                    val instance = AppDatabase.getInstance(this@MainActivity, passphrase)
                                    instance.openHelper.writableDatabase
                                    Pair<AppDatabase?, String?>(instance, null)
                                }
                            } catch (e: Exception) {
                                DiagnosticsLogger.logEvent("WARN", "db_open_failed class=${e.javaClass.simpleName}")
                                Pair<AppDatabase?, String?>(null, "db_failed")
                            }
                        }
                        database = db
                        dataLocked = error == "key_missing"
                        DiagnosticsLogger.logEvent(
                            "INFO",
                            "startup_state db_ok=${db != null} error=$error" +
                                " has_pin=${SecurePasswordStorage.hasPin()} locked=$isLocked"
                        )
                    }

                    
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        CryptoSafeApp(
                            vm = vm,
                            database = database,
                            locked = isLocked
                        )

                        if (isLocked && SecurePasswordStorage.hasPin()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                            ) {
                                LockScreen(
                                    onUnlock = {
                                        SecurePasswordStorage.setLockedOnExit(false)
                                        isLocked = false
                                    },
                                    onExit = {
                                        SecurePasswordStorage.setLockedOnExit(true)
                                        finish()
                                    }
                                )
                            }
                        }

                        if (dataLocked && !dataLockedDismissed && !isLocked) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .zIndex(10f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                ) {
                                    Text(
                                        LocalizationManager.getString("key_missing_banner"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                                    )
                                    IconButton(onClick = { dataLockedDismissed = true }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        
        
        
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                vm.onSharedTextReceived(text)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoSafeApp(
    vm: MainViewModel,
    database: AppDatabase? = null,
    locked: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLanguageMenu by remember { mutableStateOf(false) }

    
    LaunchedEffect(locked) {
        if (locked) {
            vm.onLocked()
        }
    }

    var encryptPassword by remember { mutableStateOf(charArrayOf()) }
    var encryptInput by remember { mutableStateOf("") }
    var encryptOutput by remember { mutableStateOf("") }
    var encryptShowPassword by remember { mutableStateOf(false) }

    var decryptPassword by remember { mutableStateOf(charArrayOf()) }
    var decryptInput by remember { mutableStateOf("") }
    var decryptOutput by remember { mutableStateOf("") }
    var decryptShowPassword by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    val isMainScreen = vm.mode == Screen.Home || vm.mode == Screen.Boxes

    BackHandler(enabled = !isMainScreen) {
        vm.navigateBack()
    }

    
    val chatBox by if (vm.mode == Screen.Chat && database != null) {
        database!!.boxDao().getBoxById(vm.selectedBoxId).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<Box?>(null) }
    }
    val chatMessageCount by if (vm.mode == Screen.Chat && database != null) {
        database!!.boxDao().getMessageCount(vm.selectedBoxId).collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }

    val screenTitle = when (vm.mode) {
        Screen.Encrypt -> "\uD83D\uDD12 ${LocalizationManager.getString("encrypt")}"
        Screen.Decrypt -> "\uD83D\uDD13 ${LocalizationManager.getString("decrypt")}"
        Screen.About -> "\u2139\uFE0F ${LocalizationManager.getString("about")}"
        Screen.Settings -> LocalizationManager.getString("settings")
        Screen.Help -> "\u2753 ${LocalizationManager.getString("help")}"
        Screen.CreateBox -> LocalizationManager.getString("create_box")
        Screen.Chat -> {
            val boxName = chatBox?.name
            if (!boxName.isNullOrBlank()) {
                "$boxName (${chatMessageCount} ${LocalizationManager.getString("messages")})"
            } else {
                LocalizationManager.getString("messages")
            }
        }
        Screen.BoxSettings -> LocalizationManager.getString("box_settings")
        else -> when (vm.selectedTab) {
            0 -> LocalizationManager.getString("app_name")
            else -> LocalizationManager.getString("boxes")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val isBoxes = vm.selectedTab == 1 && isMainScreen
                    val isCreateBox = vm.mode == Screen.CreateBox
                    val isSettings = vm.mode == Screen.Settings
                    val isBoxSettings = vm.mode == Screen.BoxSettings
                    if (vm.mode == Screen.Chat) {
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                chatBox?.name.orEmpty(),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                " (${chatMessageCount} ${LocalizationManager.getString("messages")})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isBoxes || isCreateBox || isSettings || isBoxSettings) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(
                                    when {
                                        isCreateBox -> com.cryptosafe.app.R.drawable.ic_add_box
                                        isBoxSettings || isSettings -> com.cryptosafe.app.R.drawable.ic_settings
                                        else -> com.cryptosafe.app.R.drawable.ic_box
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                screenTitle,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            screenTitle,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (!isMainScreen) {
                        IconButton(onClick = { vm.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, LocalizationManager.getString("content_desc_back"))
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showLanguageMenu = !showLanguageMenu }) {
                            Icon(Icons.Default.Language, LocalizationManager.getString("content_desc_language"))
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(LocalizationManager.getString("select_language")) },
                                onClick = {},
                                enabled = false
                            )
                            LocalizationManager.getAvailableLocales().forEach { locale ->
                                DropdownMenuItem(
                                    text = { Text(LocalizationManager.getLocaleDisplayName(locale)) },
                                    onClick = {
                                        LocalizationManager.setLocale(locale)
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }
                    if (isMainScreen) {
                        IconButton(onClick = { vm.goSettings() }) {
                            Icon(painterResource(R.drawable.ic_settings), LocalizationManager.getString("settings"), tint = Color.Unspecified)
                        }
                    }
                    if (vm.selectedTab == 0 && isMainScreen) {
                        IconButton(onClick = { vm.navigate(Screen.About) }) {
                            Icon(Icons.Default.Info, LocalizationManager.getString("content_desc_about"))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (vm.mode == Screen.Decrypt) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (isMainScreen) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = vm.selectedTab == 0,
                        onClick = { vm.selectTab(0); vm.navigate(Screen.Home) },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(LocalizationManager.getString("home")) }
                    )
                    NavigationBarItem(
                        selected = vm.selectedTab == 1,
                        onClick = { vm.selectTab(1); vm.navigate(Screen.Boxes) },
                        icon = {
                            Icon(
                                painter = painterResource(com.cryptosafe.app.R.drawable.ic_box),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                        },
                        label = { Text(LocalizationManager.getString("boxes")) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when {
                vm.mode == Screen.Home && vm.selectedTab == 0 -> HomeButtons(
                    onEncrypt = {
                        encryptPassword.fill('\u0000'); encryptPassword = charArrayOf()
                        encryptInput = ""; encryptOutput = ""; vm.navigate(Screen.Encrypt)
                    },
                    onDecrypt = {
                        decryptPassword.fill('\u0000'); decryptPassword = charArrayOf()
                        decryptInput = ""; decryptOutput = ""; vm.navigate(Screen.Decrypt)
                    }
                )
                vm.mode == Screen.Encrypt -> EncryptScreen(
                    password = encryptPassword,
                    onPasswordChange = { encryptPassword = it },
                    inputText = encryptInput,
                    onInputChange = { encryptInput = it },
                    outputText = encryptOutput,
                    onOutputChange = { encryptOutput = it },
                    showPassword = encryptShowPassword,
                    onTogglePassword = { encryptShowPassword = !encryptShowPassword },
                    isLoading = isLoading,
                    onStartLoading = { isLoading = true },
                    onFinishLoading = { isLoading = false },
                    onClear = {
                        encryptPassword.fill('\u0000'); encryptPassword = charArrayOf()
                        encryptInput = ""; encryptOutput = ""
                    }
                )
                vm.mode == Screen.Decrypt -> DecryptScreen(
                    password = decryptPassword,
                    onPasswordChange = { decryptPassword = it },
                    inputText = decryptInput,
                    onInputChange = { decryptInput = it },
                    outputText = decryptOutput,
                    onOutputChange = { decryptOutput = it },
                    showPassword = decryptShowPassword,
                    onTogglePassword = { decryptShowPassword = !decryptShowPassword },
                    isLoading = isLoading,
                    onStartLoading = { isLoading = true },
                    onFinishLoading = { isLoading = false },
                    onClear = {
                        decryptPassword.fill('\u0000'); decryptPassword = charArrayOf()
                        decryptInput = ""; decryptOutput = ""
                    }
                )
                vm.mode == Screen.About -> AboutScreen()
                vm.mode == Screen.Help -> HelpScreen()
                vm.mode == Screen.Settings -> SettingsScreen(
                    onBack = { vm.goHome() },
                    onHelp = { vm.navigate(Screen.Help) },
                    locked = locked
                )
                database != null -> {
                    when {
                        vm.mode == Screen.Boxes && vm.selectedTab == 1 -> BoxesScreen(
                            database = database!!,
                            onBoxClick = { box -> vm.openBox(box) },
                            onCreateBox = { vm.navigate(Screen.CreateBox) },
                            onBoxSettings = { box -> vm.selectBox(box.id); vm.navigate(Screen.BoxSettings) }
                        )
                        vm.mode == Screen.CreateBox -> CreateBoxScreen(
                            database = database!!,
                            onBack = { vm.goBoxes() }
                        )
                        vm.mode == Screen.Chat -> {
                            val box by database!!.boxDao().getBoxById(vm.selectedBoxId)
                                .collectAsState(initial = null)
                            box?.let {
                                ChatScreen(
                                    box = it,
                                    boxPassword = vm.selectedBoxPassword,
                                    database = database!!,
                                    locked = locked,
                                    onBack = { vm.clearBoxSession(); vm.goBoxes() }
                                )
                            }
                        }
                        vm.mode == Screen.BoxSettings -> {
                            val box by database!!.boxDao().getBoxById(vm.selectedBoxId)
                                .collectAsState(initial = null)
                            box?.let {
                                BoxSettingsScreen(
                                    box = it,
                                    database = database!!,
                                    onBack = { vm.goBoxes() },
                                    onPasswordChanged = { vm.onBoxPasswordChanged(it.id) },
                                    boxSessionCache = vm.boxSessionCache
                                )
                            }
                        }
                    }
                }
            }

            if (!locked && vm.showSharePicker && database != null && vm.sharedText != null) {
                SharePickerDialog(
                    database = database!!,
                    sharedText = vm.sharedText ?: "",
                    onDismiss = {
                        vm.hideSharePicker()
                        vm.consumeSharedText()
                    },
                    onQuickEncrypt = {
                        encryptInput = vm.sharedText ?: ""
                        encryptPassword = charArrayOf()
                        encryptOutput = ""
                        vm.navigate(Screen.Encrypt)
                        vm.hideSharePicker()
                        vm.consumeSharedText()
                    },
                    onQuickDecrypt = {
                        decryptInput = vm.sharedText ?: ""
                        decryptPassword = charArrayOf()
                        decryptOutput = ""
                        vm.navigate(Screen.Decrypt)
                        vm.hideSharePicker()
                        vm.consumeSharedText()
                    },
                    onBoxSelected = { box ->
                        val password = vm.cachedPasswordFor(box)
                        if (password != null) {
                            val textToSave = vm.sharedText
                            vm.hideSharePicker()
                            vm.consumeSharedText()
                            if (textToSave != null) {
                                val pass = password
                                scope.launch {
                                    val success = saveSharedTextToBox(database!!, box, pass, textToSave)
                                    if (success) {
                                        Toast.makeText(context, LocalizationManager.getString("success"), Toast.LENGTH_SHORT).show()
                                        vm.openChatWith(box, pass)
                                    } else {
                                        Toast.makeText(context, LocalizationManager.getString("error"), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            vm.requestBoxShareUnlock(box)
                        }
                    },
                    onRequestHide = { vm.hideSharePicker() }
                )
            }

            vm.boxPendingUnlock?.let { box ->
                BoxUnlockDialog(
                    box = box,
                    onDismiss = { vm.dismissPendingUnlock() },
                    onUnlocked = { password ->
                        vm.unlockBox(box, password)
                    }
                )
            }

            vm.boxPendingShareUnlock?.let { box ->
                val textToSave = vm.sharedText
                BoxUnlockDialog(
                    box = box,
                    onDismiss = { vm.dismissPendingShareUnlock() },
                    onUnlocked = { password ->
                        vm.dismissPendingShareUnlock()
                        if (textToSave != null) {
                            scope.launch {
                                val success = saveSharedTextToBox(database!!, box, password, textToSave)
                                vm.hideSharePicker()
                                vm.consumeSharedText()
                                if (success) {
                                    Toast.makeText(context, LocalizationManager.getString("success"), Toast.LENGTH_SHORT).show()
                                    vm.openChatWith(box, password)
                                } else {
                                    Toast.makeText(context, LocalizationManager.getString("error"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
