package com.cryptosafe.app

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // منع لقطة الشاشة
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF4CAF50),
                    secondary = Color(0xFF2196F3),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E)
                )
            ) {
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
    var password by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val strength = remember(password) {
        CryptoEngine.checkPasswordStrength(password.toCharArray())
    }
    val isPasswordWeak = strength.first <= 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (mode) {
                            "encrypt" -> "🔒 Encrypt / تشفير"
                            "decrypt" -> "🔓 Decrypt / فك التشفير"
                            "about" -> "ℹ️ About / حول"
                            else -> "CryptoSafe"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (mode != "home") {
                        IconButton(onClick = { mode = "home" }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    if (mode == "home") {
                        IconButton(onClick = { mode = "about" }) {
                            Icon(Icons.Default.Info, "About", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
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
                    onEncrypt = { mode = "encrypt" },
                    onDecrypt = { mode = "decrypt" }
                )
                "encrypt", "decrypt" -> {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        InputFields(
                            password = password,
                            inputText = inputText,
                            showPassword = showPassword,
                            onPasswordChange = { password = it },
                            onInputChange = { inputText = it },
                            onTogglePassword = { showPassword = !showPassword },
                            strength = strength,
                            mode = mode
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isPasswordWeak && mode == "encrypt") {
                                    Toast.makeText(context, "Password too weak / كلمة المرور ضعيفة جداً", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (inputText.isBlank()) {
                                    Toast.makeText(context, "Enter text first / أدخل النص أولاً", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isLoading = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val passChars = password.toCharArray()
                                        val result = if (mode == "encrypt")
                                            CryptoEngine.encrypt(inputText, passChars)
                                        else {
                                            try {
                                                CryptoEngine.decrypt(inputText, passChars)
                                            } catch (e: Exception) {
                                                throw e
                                            }
                                        }

                                        // Clear password from memory after use
                                        passChars.fill('0')
                                        outputText = result
                                    } catch (e: Exception) {
                                        outputText = ""
                                        // عرض رسالة خطأ بدلاً من الخروج
                                        scope.launch(Dispatchers.Main) {
                                            Toast.makeText(context, "Failed: Wrong password or corrupt data / فشل: كلمة مرور خاطئة أو نص تالف", Toast.LENGTH_LONG).show()
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isLoading && (mode == "decrypt" || !isPasswordWeak)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    if (mode == "encrypt") "Encrypt / تشفير" else "Decrypt / فك التشفير",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (outputText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Output / المخرج:",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = outputText,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 200.dp),
                                        textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(outputText))
                                                Toast.makeText(context, "Copied / تم النسخ", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                outputText = ""
                                                inputText = ""
                                                password = ""
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "about" -> AboutScreen()
            }
        }
    }
}

@Composable
fun HomeButtons(onEncrypt: () -> Unit, onDecrypt: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Shield,
                    null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "CryptoSafe",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Offline & Secure Encryption\nتشفير آمن يعمل بدون إنترنت",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onEncrypt,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Encrypt / تشفير", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onDecrypt,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Decrypt / فك التشفير", fontSize = 16.sp)
        }
    }
}

@Composable
fun InputFields(
    password: String,
    inputText: String,
    showPassword: Boolean,
    onPasswordChange: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    strength: Pair<Int, String>,
    mode: String
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
                label = { Text("Password / كلمة المرور") },
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
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Strength Indicator
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
                    "Strength / القوة:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    strength.second,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                label = {
                    Text(
                        if (mode == "encrypt") "Text to encrypt / النص للتشفير"
                        else "Encrypted text (Base64) / النص المشفر"
                    )
                },
                placeholder = {
                    Text(
                        if (mode == "encrypt") "Enter any text..."
                        else "Paste the encrypted text here..."
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 200.dp),
                maxLines = 8,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun AboutScreen() {
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
                Icon(
                    Icons.Default.Shield,
                    null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "CryptoSafe",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Version 1.0",
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
                    "📖 How to Use / طريقة الاستخدام",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "• Enter a strong password (10+ chars, mix of letters, numbers, symbols).\n" +
                    "• Enter text to encrypt or paste encrypted text to decrypt.\n" +
                    "• Press Encrypt/Decrypt.\n" +
                    "• Copy the result and store it safely.\n\n" +
                    "• أدخل كلمة مرور قوية (10+ أحرف، مزيج من الحروف والأرقام والرموز).\n" +
                    "• أدخل النص لتشفيره أو الصق النص المشفر لفك تشفيره.\n" +
                    "• اضغط تشفير/فك تشفير.\n" +
                    "• انسخ النتيجة واحفظها بأمان.",
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
                    "⚠️ Disclaimer / إخلاء مسؤولية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This app is provided 'as is' without warranties. The developer is not responsible for any data loss or security breaches. Always keep backups of your encrypted data and passwords in a safe place.\n\n" +
                    "يتم توفير هذا التطبيق 'كما هو' دون أي ضمانات. المطور غير مسؤول عن أي فقدان للبيانات أو خروقات أمنية. احتفظ دائماً بنسخ احتياطية من بياناتك المشفرة وكلمات المرور في مكان آمن.",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
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
                Text(
                    "Developer / المطور: GitHub Actions CI",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}