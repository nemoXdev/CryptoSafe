package com.cryptosafe.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.fragment.app.FragmentActivity
import com.cryptosafe.app.data.AppDatabase
import com.cryptosafe.app.screens.AboutScreen
import com.cryptosafe.app.screens.BoxesScreen
import com.cryptosafe.app.screens.BoxSettingsScreen
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
    private var sharedText by mutableStateOf<String?>(null)
    private var showSharePicker by mutableStateOf(false)
    // ذاكرة مؤقتة لكلمات مرور الصناديق اللي وضعها المستخدم "تذكّرها" (timed/never) فقط.
    // لا تُخزَّن على القرص أبداً، وتُمسح بالكامل بمجرد ما التطبيق يروح للخلفية (ON_STOP).
    private var boxSessionCache by mutableStateOf<Map<Long, Pair<String, Long>>>(emptyMap())

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

        // تحديد حالة القفل الأولية بناءً على المؤقت
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
            // إذا خرج المستخدم من شاشة القفل (إلغاء/إنهاء) دون نجاح فك القفل،
            // يجب أن تطلب العودة الرمز دائماً مهما كان المؤقت.
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
                            boxSessionCache = emptyMap()
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
                            boxSessionCache = emptyMap()
                        } else if (timer > 0) {
                            val lastStop = SecurePasswordStorage.getLastStopTime()
                            if (lastStop > 0 && System.currentTimeMillis() - lastStop >= timer * 1000L) {
                                isLocked = true
                                boxSessionCache = emptyMap()
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
                    var dbError by remember { mutableStateOf<String?>(null) }
                    var dbErrorDetail by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        val (db, error, detail) = withContext(Dispatchers.IO) {
                            try {
                                val dbFile = getDatabasePath("cryptosafe.db")
                                val hasKey = SecurePasswordStorage.getDatabasePassphrase() != null
                                if (dbFile.exists() && dbFile.length() > 0 && !hasKey) {
                                    // بيانات موجودة لكن مفتاحها مفقود — لا ننشئ مفتاحاً جديداً أبداً
                                    // (إلا سيتعذر فتح البيانات القديمة وتضيع للأبد).
                                    Triple<AppDatabase?, String?, String?>(null, "key_missing", null)
                                } else {
                                    val passphrase = SecurePasswordStorage.getOrCreateDatabasePassphrase()
                                    val instance = AppDatabase.getInstance(this@MainActivity, passphrase)
                                    instance.openHelper.writableDatabase
                                    Triple<AppDatabase?, String?, String?>(instance, null, null)
                                }
                            } catch (e: Exception) {
                                DiagnosticsLogger.logEvent("WARN", "db_open_failed class=${e.javaClass.simpleName}")
                                Triple<AppDatabase?, String?, String?>(null, "db_failed", e.toString())
                            }
                        }
                        database = db
                        dbError = error
                        dbErrorDetail = detail
                        DiagnosticsLogger.logEvent(
                            "INFO",
                            "startup_state db_ok=${db != null} error=$error" +
                                " has_pin=${SecurePasswordStorage.hasPin()} locked=$isLocked"
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (dbError != null) {
                            DatabaseErrorScreen(
                                errorType = dbError ?: "db_failed",
                                detail = dbErrorDetail,
                                isBackupAvailable = SecurePasswordStorage.isBackupAvailable,
                                onExit = { finish() },
                                onContinue = { dbError = null },
                                onRecover = { pin ->
                                    if (SecurePasswordStorage.recoverFromBackup(pin)) {
                                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                                        finish()
                                        startActivity(intent)
                                        true
                                    } else false
                                },
                                onReset = {
                                    try {
                                        getDatabasePath("cryptosafe.db").delete()
                                        getDatabasePath("cryptosafe.db-shm").delete()
                                        getDatabasePath("cryptosafe.db-wal").delete()
                                    } catch (_: Exception) {}
                                    SecurePasswordStorage.clearDatabasePassphrase()
                                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                                    finish()
                                    startActivity(intent)
                                }
                            )
                        } else {
                            CryptoSafeApp(
                                sharedText = sharedText,
                                onSharedTextConsumed = { sharedText = null },
                                showSharePicker = if (isLocked) false else showSharePicker,
                                onSharePickerDismiss = { showSharePicker = false },
                                database = database,
                                locked = isLocked,
                                boxSessionCache = boxSessionCache,
                                onBoxSessionCacheChange = { boxSessionCache = it }
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
        // ملاحظة أمنية: لا نلغي القفل هنا أبداً. المشاركة تُخزَّن فقط وتُعرض
        // تلقائياً بعد ما المستخدم يفتح القفل بنفسه (PIN/بصمة) بشكل طبيعي —
        // شاشة الصناديق/التشفير لا تُركَّب (compose) أصلاً إلا بعد فك القفل.
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                sharedText = text
                showSharePicker = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoSafeApp(
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
    showSharePicker: Boolean = false,
    onSharePickerDismiss: () -> Unit = {},
    database: AppDatabase? = null,
    locked: Boolean = false,
    boxSessionCache: Map<Long, Pair<String, Long>> = emptyMap(),
    onBoxSessionCacheChange: (Map<Long, Pair<String, Long>>) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("home") }
    var selectedTab by remember { mutableStateOf(0) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var selectedBoxId by remember { mutableLongStateOf(0L) }
    var selectedBoxPassword by remember { mutableStateOf("") }

    // صندوق بانتظار إدخال كلمة مروره قبل فتح المحادثة
    var boxPendingUnlock by remember { mutableStateOf<com.cryptosafe.app.data.Box?>(null) }
    // صندوق بانتظار إدخال كلمة مروره قبل حفظ نص مُشارَك فيه
    var boxPendingShareUnlock by remember { mutableStateOf<com.cryptosafe.app.data.Box?>(null) }

    // عند القفل، تُغلق أي حوارات إدخال كلمة مرور (نوافذ منفصلة تبقى ظاهرة فوق شاشة القفل)
    LaunchedEffect(locked) {
        if (locked) {
            boxPendingUnlock = null
            boxPendingShareUnlock = null
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

    // ملاحظة: توجيه النص المشارك يتم بالكامل عبر SharePickerDialog بالأسفل
    // (تشفير سريع / فك تشفير سريع / اختيار صندوق) - لا حاجة لأي توجيه تلقائي هنا.

    fun clearBoxSession() {
        selectedBoxPassword = ""
    }

    // يفتح الصندوق مباشرة لو فيه كلمة مرور محفوظة صالحة (ذاكرة مؤقتة أو تخزين دائم)،
    // وإلا يطلب كلمة المرور عبر BoxUnlockDialog كالمعتاد.
    fun openBox(box: com.cryptosafe.app.data.Box) {
        val cached = boxSessionCache[box.id]
        var password = cached?.first
        if (password == null && box.lockMode == "permanent") {
            val savedPassword = SecurePasswordStorage.getBoxPassword(box.id)
            if (savedPassword != null) {
                password = savedPassword
                onBoxSessionCacheChange(
                    boxSessionCache + (box.id to (savedPassword to System.currentTimeMillis()))
                )
            }
        }
        val stillValid = when (box.lockMode) {
            "never" -> cached != null
            "timed" -> cached != null &&
                (System.currentTimeMillis() - cached.second) < (box.lockTimeoutMinutes ?: 5) * 60_000L
            "permanent" -> password != null
            else -> false // "always"
        }
        if (stillValid && password != null) {
            selectedBoxId = box.id
            selectedBoxPassword = password
            mode = "chat"
        } else {
            boxPendingUnlock = box
        }
    }

    BackHandler(enabled = mode != "home" && mode != "boxes") {
        when (mode) {
            "encrypt", "decrypt", "about", "settings" -> { mode = "home"; selectedTab = 0 }
            "help" -> { mode = "settings" }
            "create_box" -> { mode = "boxes"; selectedTab = 1 }
            "chat", "box_settings" -> { clearBoxSession(); mode = "boxes"; selectedTab = 1 }
            else -> { mode = "home"; selectedTab = 0 }
        }
    }

    val isMainScreen = mode == "home" || mode == "boxes"

    // بيانات الصندوق المفتوح لعرض اسمه وعدد رسائله في عنوان شريط الدردشة
    val chatBox by if (mode == "chat" && database != null) {
        database!!.boxDao().getBoxById(selectedBoxId).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<com.cryptosafe.app.data.Box?>(null) }
    }
    val chatMessageCount by if (mode == "chat" && database != null) {
        database!!.boxDao().getMessageCount(selectedBoxId).collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }

    val screenTitle = when {
        mode == "encrypt" -> "\uD83D\uDD12 ${LocalizationManager.getString("encrypt")}"
        mode == "decrypt" -> "\uD83D\uDD13 ${LocalizationManager.getString("decrypt")}"
        mode == "about" -> "\u2139\uFE0F ${LocalizationManager.getString("about")}"
        mode == "settings" -> LocalizationManager.getString("settings")
        mode == "help" -> "\u2753 ${LocalizationManager.getString("help")}"
        mode == "create_box" -> LocalizationManager.getString("create_box")
        mode == "chat" -> {
            val boxName = chatBox?.name
            if (!boxName.isNullOrBlank()) {
                "$boxName (${chatMessageCount} ${LocalizationManager.getString("messages")})"
            } else {
                LocalizationManager.getString("messages")
            }
        }
        mode == "box_settings" -> LocalizationManager.getString("box_settings")
        selectedTab == 0 -> LocalizationManager.getString("app_name")
        selectedTab == 1 -> LocalizationManager.getString("boxes")
        else -> LocalizationManager.getString("app_name")
    }

    val boxesTitle = LocalizationManager.getString("boxes")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val isBoxes = selectedTab == 1 && isMainScreen
                    val isCreateBox = mode == "create_box"
                    val isSettings = mode == "settings"
                    val isBoxSettings = mode == "box_settings"
                    if (mode == "chat") {
                        // الاسم يُقتطع بثلاث نقاط عند الطول، وعدد الرسائل يبقى ظاهراً دائماً
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
                        IconButton(onClick = {
                            when (mode) {
                                "encrypt", "decrypt", "about", "settings" -> { mode = "home"; selectedTab = 0 }
            "help" -> { mode = "settings" }
                                "create_box" -> { mode = "boxes"; selectedTab = 1 }
                                "chat", "box_settings" -> { clearBoxSession(); mode = "boxes"; selectedTab = 1 }
                                else -> { mode = "home"; selectedTab = 0 }
                            }
                        }) {
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
                        IconButton(onClick = { mode = "settings" }) {
                            Icon(painterResource(R.drawable.ic_settings), LocalizationManager.getString("settings"), tint = Color.Unspecified)
                        }
                    }
                    if (selectedTab == 0 && isMainScreen) {
                        IconButton(onClick = { mode = "about" }) {
                            Icon(Icons.Default.Info, LocalizationManager.getString("content_desc_about"))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (mode == "decrypt") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
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
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; mode = "home" },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(LocalizationManager.getString("home")) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; mode = "boxes" },
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
                mode == "home" && selectedTab == 0 -> HomeButtons(
                    onEncrypt = {
                        encryptPassword.fill('\u0000'); encryptPassword = charArrayOf()
                        encryptInput = ""; encryptOutput = ""; mode = "encrypt"
                    },
                    onDecrypt = {
                        decryptPassword.fill('\u0000'); decryptPassword = charArrayOf()
                        decryptInput = ""; decryptOutput = ""; mode = "decrypt"
                    }
                )
                mode == "encrypt" -> EncryptScreen(
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
                mode == "decrypt" -> DecryptScreen(
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
                mode == "about" -> AboutScreen()
                mode == "help" -> HelpScreen()
                mode == "settings" -> SettingsScreen(
                    onBack = { mode = "home"; selectedTab = 0 },
                    onHelp = { mode = "help" },
                    locked = locked
                )
                database != null -> {
                    when {
                        mode == "boxes" && selectedTab == 1 -> BoxesScreen(
                            database = database!!,
                            onBoxClick = { box -> openBox(box) },
                            onCreateBox = { mode = "create_box" },
                            onBoxSettings = { box -> selectedBoxId = box.id; mode = "box_settings" }
                        )
                        mode == "create_box" -> CreateBoxScreen(
                            database = database!!,
                            onBack = { mode = "boxes"; selectedTab = 1 }
                        )
                        mode == "chat" -> {
                            val box by database!!.boxDao().getBoxById(selectedBoxId)
                                .collectAsState(initial = null)
                            box?.let {
                                ChatScreen(
                                    box = it,
                                    boxPassword = selectedBoxPassword,
                                    database = database!!,
                                    locked = locked,
                                    onBack = { clearBoxSession(); mode = "boxes"; selectedTab = 1 }
                                )
                            }
                        }
                        mode == "box_settings" -> {
                            val box by database!!.boxDao().getBoxById(selectedBoxId)
                                .collectAsState(initial = null)
                            box?.let {
                                BoxSettingsScreen(
                                    box = it,
                                    database = database!!,
                                    onBack = { mode = "boxes"; selectedTab = 1 },
                                    onPasswordChanged = {
                                        onBoxSessionCacheChange(boxSessionCache - it.id)
                                    },
                                    boxSessionCache = boxSessionCache
                                )
                            }
                        }
                    }
                }
            }

            if (showSharePicker && database != null && sharedText != null) {
                SharePickerDialog(
                    database = database!!,
                    sharedText = sharedText,
                    onDismiss = {
                        onSharePickerDismiss()
                        onSharedTextConsumed()
                    },
                    onQuickEncrypt = {
                        encryptInput = sharedText ?: ""
                        encryptPassword = charArrayOf()
                        encryptOutput = ""
                        mode = "encrypt"
                        onSharePickerDismiss()
                        onSharedTextConsumed()
                    },
                    onQuickDecrypt = {
                        decryptInput = sharedText ?: ""
                        decryptPassword = charArrayOf()
                        decryptOutput = ""
                        mode = "decrypt"
                        onSharePickerDismiss()
                        onSharedTextConsumed()
                    },
                    onBoxSelected = { box ->
                        val cached = boxSessionCache[box.id]
                        var password = cached?.first
                        if (password == null && box.lockMode == "permanent") {
                            val saved = SecurePasswordStorage.getBoxPassword(box.id)
                            if (saved != null) {
                                password = saved
                                onBoxSessionCacheChange(
                                    boxSessionCache + (box.id to (saved to System.currentTimeMillis()))
                                )
                            }
                        }
                        val stillValid = when (box.lockMode) {
                            "never" -> cached != null
                            "timed" -> cached != null &&
                                (System.currentTimeMillis() - cached.second) < (box.lockTimeoutMinutes ?: 5) * 60_000L
                            "permanent" -> password != null
                            else -> false
                        }
                        if (stillValid && password != null) {
                            val textToSave = sharedText
                            onSharePickerDismiss()
                            onSharedTextConsumed()
                            if (textToSave != null) {
                                val pass = password
                                scope.launch {
                                    val success = saveSharedTextToBox(database!!, box, pass, textToSave)
                                    if (success) {
                                        Toast.makeText(context, LocalizationManager.getString("success"), Toast.LENGTH_SHORT).show()
                                        selectedBoxId = box.id
                                        selectedBoxPassword = pass
                                        mode = "chat"
                                    } else {
                                        Toast.makeText(context, LocalizationManager.getString("error"), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            boxPendingShareUnlock = box
                        }
                    },
                    onRequestHide = { onSharePickerDismiss() }
                )
            }

            boxPendingUnlock?.let { box ->
                com.cryptosafe.app.screens.BoxUnlockDialog(
                    box = box,
                    onDismiss = { boxPendingUnlock = null },
                    onUnlocked = { password ->
                        selectedBoxId = box.id
                        selectedBoxPassword = password
                        if (box.lockMode == "timed" || box.lockMode == "never") {
                            onBoxSessionCacheChange(
                                boxSessionCache + (box.id to (password to System.currentTimeMillis()))
                            )
                        }
                        if (box.lockMode == "permanent") {
                            SecurePasswordStorage.saveBoxPassword(box.id, password)
                            onBoxSessionCacheChange(
                                boxSessionCache + (box.id to (password to System.currentTimeMillis()))
                            )
                        }
                        boxPendingUnlock = null
                        mode = "chat"
                    }
                )
            }

            boxPendingShareUnlock?.let { box ->
                val textToSave = sharedText
                com.cryptosafe.app.screens.BoxUnlockDialog(
                    box = box,
                    onDismiss = { boxPendingShareUnlock = null },
                    onUnlocked = { password ->
                        boxPendingShareUnlock = null
                        if (textToSave != null) {
                            scope.launch {
                                val success = saveSharedTextToBox(database!!, box, password, textToSave)
                                onSharePickerDismiss()
                                onSharedTextConsumed()
                                if (success) {
                                    Toast.makeText(context, LocalizationManager.getString("success"), Toast.LENGTH_SHORT).show()
                                    if (box.lockMode == "timed" || box.lockMode == "never") {
                                        onBoxSessionCacheChange(
                                            boxSessionCache + (box.id to (password to System.currentTimeMillis()))
                                        )
                                    }
                                    if (box.lockMode == "permanent") {
                                        SecurePasswordStorage.saveBoxPassword(box.id, password)
                                        onBoxSessionCacheChange(
                                            boxSessionCache + (box.id to (password to System.currentTimeMillis()))
                                        )
                                    }
                                    selectedBoxId = box.id
                                    selectedBoxPassword = password
                                    mode = "chat"
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

@Composable
private fun DatabaseErrorScreen(
    errorType: String,
    detail: String?,
    isBackupAvailable: Boolean = false,
    onExit: () -> Unit,
    onContinue: () -> Unit,
    onRecover: (String) -> Boolean,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var isRecovering by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                LocalizationManager.getString("db_error_title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (isBackupAvailable) {
                    LocalizationManager.getString("db_error_backup_available")
                } else if (errorType == "key_missing") {
                    LocalizationManager.getString("db_error_key_missing")
                } else {
                    LocalizationManager.getString("db_error_failed")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            if (detail != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            if (isBackupAvailable) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it; pinError = false },
                    label = { Text(LocalizationManager.getString("pin")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = pinError,
                    supportingText = if (pinError) {
                        { Text(LocalizationManager.getString("pin_wrong"), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (pinInput.isNotEmpty()) {
                            isRecovering = true
                            val success = onRecover(pinInput)
                            isRecovering = false
                            if (!success) pinError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = pinInput.isNotEmpty() && !isRecovering
                ) {
                    if (isRecovering) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(LocalizationManager.getString("db_error_recover"))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val file = DiagnosticsLogger.getExportFile()
                    if (file != null) {
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_TEXT, "CryptoSafe diagnostics log")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        } catch (_: Exception) {
                            Toast.makeText(context, LocalizationManager.getString("error"), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, LocalizationManager.getString("diagnostics_no_log"), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(LocalizationManager.getString("diagnostics_share"))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(LocalizationManager.getString("db_error_continue"))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(LocalizationManager.getString("db_error_reset"))
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onExit) {
                Text(LocalizationManager.getString("db_error_exit"))
            }
        }
    }
}
