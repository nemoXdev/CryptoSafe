package com.cryptosafe.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CryptoSafeApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoSafeApp() {
    var mode by remember { mutableStateOf("encrypt") }
    var password by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val strength = remember(password) { CryptoEngine.checkPasswordStrength(password) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode == "encrypt") "🔒 Encrypt / تشفير" else "🔓 Decrypt / فك التشفير") },
                navigationIcon = {
                    IconButton(onClick = { mode = if (mode == "encrypt") "home" else "encrypt" }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (mode == "home") {
                HomeButtons(onEncrypt = { mode = "encrypt" }, onDecrypt = { mode = "decrypt" })
            } else {
                InputFields(password, inputText, showPassword, { password = it }, { inputText = it }, { showPassword = !showPassword }, strength)
                
                Button(
                    onClick = {
                        if (password.length < 10) return@Button Toast.makeText(context, "Password too short / كلمة المرور قصيرة", Toast.LENGTH_SHORT).show()
                        if (inputText.isBlank()) return@Button Toast.makeText(context, "Enter text first / أدخل النص أولاً", Toast.LENGTH_SHORT).show()
                        
                        isLoading = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val result = if (mode == "encrypt")
                                    CryptoEngine.encrypt(inputText, password)
                                else
                                    CryptoEngine.decrypt(inputText, password)
                                outputText = result
                            } catch (e: Exception) {
                                outputText = ""
                                Toast.makeText(context, "Failed: Wrong password or corrupt data / فشل: كلمة مرور خاطئة أو نص تالف", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Text(if (mode == "encrypt") "Encrypt / تشفير" else "Decrypt / فك التشفير")
                }

                if (outputText.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Output / المخرج:", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = outputText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 200.dp),
                        textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            clipboard.setText(AnnotatedString(outputText))
                            Toast.makeText(context, "Copied / تم النسخ", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.weight(1f)) { Text("Copy / نسخ") }
                        OutlinedButton(onClick = { outputText = ""; inputText = ""; password = "" }, modifier = Modifier.weight(1f)) { Text("Clear / مسح") }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeButtons(onEncrypt: () -> Unit, onDecrypt: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Shield, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Offline & Secure Encryption\nتشفير آمن يعمل بدون إنترنت", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Spacer(Modifier.height(32.dp))
        Button(onClick = onEncrypt, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("🔒 Encrypt / تشفير") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onDecrypt, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("🔓 Decrypt / فك التشفير") }
    }
}

@Composable
fun InputFields(pass: String, text: String, showPass: Boolean, setPass: (String) -> Unit, setText: (String) -> Unit, togglePass: () -> Unit, strength: Pair<Int, String>) {
    OutlinedTextField(
        value = pass, onValueChange = setPass,
        label = { Text("Password / كلمة المرور") },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (showPass) PasswordVisualTransformation() else PasswordVisualTransformation(),
        trailingIcon = { IconButton(onClick = togglePass) { Icon(if (showPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } },
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
    )
    
    // Strength Indicator
    val color = when (strength.first) { 0, 1 -> androidx.compose.ui.graphics.Color(0xFFEF4444) 2, 3 -> androidx.compose.ui.graphics.Color(0xFFF59E0B) else -> androidx.compose.ui.graphics.Color(0xFF10B981) }
    LinearProgressIndicator(progress = strength.first / 4f, modifier = Modifier.fillMaxWidth(), color = color)
    Text(strength.second, modifier = Modifier.align(Alignment.End), color = color, style = MaterialTheme.typography.labelSmall)

    OutlinedTextField(
        value = text, onValueChange = setText,
        label = { Text("Input Text / أدخل النص") },
        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
        maxLines = 10
    )
    Spacer(Modifier.height(16.dp))
}
