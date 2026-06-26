package com.cryptosafe.app

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        // Set to false to temporarily disable screenshot protection
        private const val ENABLE_SCREENSHOT_PROTECTION = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize localization
        LocalizationManager.initialize(this)
        // Prevent screenshots
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

// Custom attractive color scheme
@Composable
fun CryptoSafeTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFF00BFA5),           // Teal
        onPrimary = Color.White,
        primaryContainer = Color(0xFF00574B),
        secondary = Color(0xFF7C4DFF),         // Purple
        secondaryContainer = Color(0xFF311B92),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E2A),
        surfaceVariant = Color(0xFF2A2A3C),
        error = Color(0xFFCF6679),
        onBackground = Color.White,
        onSurface = Color.White,
    )
    val layoutDirection = if (LocalizationManager.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoSafeApp() {
    var mode by remember { mutableStateOf("home") }
    var showLanguageMenu by remember { mutableStateOf(false) }
    
    // Encrypt mode state
    var encryptPassword by remember { mutableStateOf("") }
    var encryptInput by remember { mutableStateOf("") }
    var encryptOutput by remember { mutableStateOf("") }
    var encryptShowPassword by remember { mutableStateOf(false) }

    // Decrypt mode state
    var decryptPassword by remember { mutableStateOf("") }
    var decryptInput by remember { mutableStateOf("") }
    var decryptOutput by remember { mutableStateOf("") }
    var decryptShowPassword by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Handle system back button
    BackHandler(enabled = mode != "home") {
        mode = "home"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (mode) {
                            "encrypt" -> "🔒 ${LocalizationManager.getString("encrypt")}"
                            "decrypt" -> "🔓 ${LocalizationManager.getString("decrypt")}"
                            "about" -> "ℹ️ ${LocalizationManager.getString("about")}"
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
                    // Language switcher button
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
                    
                    // زر حول
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
                        // Clear old values on navigation
                        encryptPassword = ""
                        encryptInput = ""
                        encryptOutput = ""
                        mode = "encrypt"
                    },
                    onDecrypt = {
                        decryptPassword = ""
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
                            encryptPassword = ""
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
                            decryptPassword = ""
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

@Composable
fun HomeButtons(onEncrypt: () -> Unit, onDecrypt: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                LocalizationManager.getString("app_name"),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                LocalizationManager.getString("offline_secure") + "\n" + LocalizationManager.getString("offline_desc"),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onEncrypt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(LocalizationManager.getString("encrypt_button"), fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDecrypt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(LocalizationManager.getString("decrypt_button"), fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun EncryptScreen(
    password: String,
    onPasswordChange: (String) -> Unit,
    inputText: String,
    onInputChange: (String) -> Unit,
    outputText: String,
    onOutputChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    isLoading: Boolean,
    onStartLoading: () -> Unit,
    onFinishLoading: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val strength = remember(password) {
        CryptoEngine.checkPasswordStrength(password.toCharArray())
    }
    val isPasswordWeak = strength.first <= 1

    val formContent: @Composable () -> Unit = {
        PasswordStrengthCard(password, showPassword, onPasswordChange, onTogglePassword, strength)

        Spacer(modifier = Modifier.height(16.dp))

        InputCard(
            value = inputText,
            onValueChange = onInputChange,
            label = LocalizationManager.getString("input_label"),
            placeholder = LocalizationManager.getString("input_text")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isPasswordWeak) {
                    Toast.makeText(context, LocalizationManager.getString("password_too_weak"), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (inputText.isBlank()) {
                    Toast.makeText(context, LocalizationManager.getString("input_text"), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                onStartLoading()
                scope.launch(Dispatchers.IO) {
                    val passChars = password.toCharArray()
                    try {
                        val result = CryptoEngine.encrypt(inputText, passChars)
                        onOutputChange(result)
                        onPasswordChange("")
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, LocalizationManager.getString("success"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        onOutputChange("")
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, LocalizationManager.getString("error"), Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        passChars.fill('0')
                        onFinishLoading()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isLoading && !isPasswordWeak
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(LocalizationManager.getString("encrypt_button"), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    val outputContent: @Composable () -> Unit = {
        if (outputText.isNotEmpty()) {
            OutputCard(
                outputText = outputText,
                onCopy = {
                    clipboard.setText(AnnotatedString(outputText))
                    Toast.makeText(context, LocalizationManager.getString("copied"), Toast.LENGTH_SHORT).show()
                },
                onClear = onClear
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    formContent()
                }
                if (outputText.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        outputContent()
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                formContent()
                if (outputText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
                outputContent()
            }
        }
    }
}

@Composable
fun DecryptScreen(
    password: String,
    onPasswordChange: (String) -> Unit,
    inputText: String,
    onInputChange: (String) -> Unit,
    outputText: String,
    onOutputChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    isLoading: Boolean,
    onStartLoading: () -> Unit,
    onFinishLoading: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val formContent: @Composable () -> Unit = {
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(LocalizationManager.getString("password")) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                cursorColor = MaterialTheme.colorScheme.secondary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        InputCard(
            value = inputText,
            onValueChange = onInputChange,
            label = LocalizationManager.getString("input_label"),
            placeholder = LocalizationManager.getString("output_text"),
            focusedColor = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (inputText.isBlank()) {
                    Toast.makeText(context, LocalizationManager.getString("input_text"), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password.isBlank()) {
                    Toast.makeText(context, LocalizationManager.getString("password_required"), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                onStartLoading()
                scope.launch(Dispatchers.IO) {
                    val passChars = password.toCharArray()
                    try {
                        val result = CryptoEngine.decrypt(inputText, passChars)
                        onOutputChange(result)
                        onPasswordChange("")
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, LocalizationManager.getString("success"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        onOutputChange("")
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, LocalizationManager.getString("decrypt_error"), Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        passChars.fill('0')
                        onFinishLoading()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(LocalizationManager.getString("decrypt_button"), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    val outputContent: @Composable () -> Unit = {
        if (outputText.isNotEmpty()) {
            OutputCard(
                outputText = outputText,
                onCopy = {
                    clipboard.setText(AnnotatedString(outputText))
                    Toast.makeText(context, LocalizationManager.getString("copied"), Toast.LENGTH_SHORT).show()
                },
                onClear = onClear
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    formContent()
                }
                if (outputText.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        outputContent()
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                formContent()
                if (outputText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
                outputContent()
            }
        }
    }
}

@Composable
fun PasswordStrengthCard(
    password: String,
    showPassword: Boolean,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    strength: Pair<Int, String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(LocalizationManager.getString("password")) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            val color = when (strength.first) {
                0, 1 -> Color(0xFFEF4444)
                2, 3 -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            }
            LinearProgressIndicator(
                progress = { strength.first / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    LocalizationManager.getString("password_strength") + ":",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    LocalizationManager.getString(strength.second),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun InputCard(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    focusedColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 200.dp)
                .padding(8.dp),
            maxLines = 8,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = focusedColor,
                cursorColor = focusedColor
            )
        )
    }
}

@Composable
fun OutputCard(
    outputText: String,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                LocalizationManager.getString("output") + ":",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = outputText,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 200.dp),
                textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(LocalizationManager.getString("copy"))
                }
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(LocalizationManager.getString("clear"))
                }
            }
        }
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    val developerLink = "https://github.com/nemoXdev"
    val sourceCodeLink = "https://github.com/nemoXdev/CryptoSafe"
    val annotatedDeveloperText = buildAnnotatedString {
        append(LocalizationManager.getString("developer") + ": ")
        pushStringAnnotation(tag = "URL", annotation = developerLink)
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)) {
            append("NemoXdev")
        }
        pop()
    }
    val annotatedSourceCodeText = buildAnnotatedString {
        append(LocalizationManager.getString("source_code") + ": ")
        pushStringAnnotation(tag = "URL", annotation = sourceCodeLink)
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)) {
            append("GitHub")
        }
        pop()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    LocalizationManager.getString("app_name"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    LocalizationManager.getString("version") + " " + BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    LocalizationManager.getString("whats_new_title"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    LocalizationManager.getString("whats_new_body"),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    LocalizationManager.getString("how_to_use_title"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    LocalizationManager.getString("how_to_use"),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    LocalizationManager.getString("disclaimer_title"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    LocalizationManager.getString("disclaimer_body"),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Code,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ClickableText(
                        text = annotatedDeveloperText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            annotatedDeveloperText
                                .getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()
                                ?.let { annotation ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)))
                                }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Code,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ClickableText(
                        text = annotatedSourceCodeText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            annotatedSourceCodeText
                                .getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()
                                ?.let { annotation ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)))
                                }
                        }
                    )
                }
            }
        }
    }
}