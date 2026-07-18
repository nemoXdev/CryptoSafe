package com.cryptosafe.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cryptosafe.app.screens.AboutScreen
import com.cryptosafe.app.screens.DecryptScreen
import com.cryptosafe.app.screens.EncryptScreen
import com.cryptosafe.app.screens.HomeButtons
import com.cryptosafe.app.theme.CryptoSafeTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val ENABLE_SCREENSHOT_PROTECTION = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalizationManager.initialize(this)
        if (ENABLE_SCREENSHOT_PROTECTION) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContent {
            CryptoSafeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CryptoSafeApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoSafeApp() {
    var mode by remember { mutableStateOf("home") }
    var showLanguageMenu by remember { mutableStateOf(false) }

    var encryptPassword by remember { mutableStateOf(charArrayOf()) }
    var encryptInput by remember { mutableStateOf("") }
    var encryptOutput by remember { mutableStateOf("") }
    var encryptShowPassword by remember { mutableStateOf(false) }

    var decryptPassword by remember { mutableStateOf(charArrayOf()) }
    var decryptInput by remember { mutableStateOf("") }
    var decryptOutput by remember { mutableStateOf("") }
    var decryptShowPassword by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    BackHandler(enabled = mode != "home") {
        mode = "home"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (mode) {
                            "encrypt" -> "\uD83D\uDD12 ${LocalizationManager.getString("encrypt")}"
                            "decrypt" -> "\uD83D\uDD13 ${LocalizationManager.getString("decrypt")}"
                            "about" -> "\u2139\uFE0F ${LocalizationManager.getString("about")}"
                            else -> LocalizationManager.getString("app_name")
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (mode != "home") {
                        IconButton(onClick = { mode = "home" }) {
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
                    if (mode == "home") {
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            when (mode) {
                "home" -> HomeButtons(
                    onEncrypt = {
                        encryptPassword.fill('\u0000')
                        encryptPassword = charArrayOf()
                        encryptInput = ""
                        encryptOutput = ""
                        mode = "encrypt"
                    },
                    onDecrypt = {
                        decryptPassword.fill('\u0000')
                        decryptPassword = charArrayOf()
                        decryptInput = ""
                        decryptOutput = ""
                        mode = "decrypt"
                    }
                )
                "encrypt" -> {
                    EncryptScreen(
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
                            encryptPassword.fill('\u0000')
                            encryptPassword = charArrayOf()
                            encryptInput = ""
                            encryptOutput = ""
                        }
                    )
                }
                "decrypt" -> {
                    DecryptScreen(
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
                            decryptPassword.fill('\u0000')
                            decryptPassword = charArrayOf()
                            decryptInput = ""
                            decryptOutput = ""
                        }
                    )
                }
                "about" -> AboutScreen()
            }
        }
    }
}
