package com.cryptosafe.app

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


object ClipboardHelper {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun sensitiveClipData(label: String, text: String): ClipData {
        val description = ClipDescription(label, arrayOf("text/plain"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        return ClipData(description, ClipData.Item(text))
    }

    fun copySensitive(
        context: Context,
        text: String,
        onCopied: () -> Unit
    ) {
        val cm = context.applicationContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(sensitiveClipData("cryptosafe", text))
        onCopied()
        scope.launch(Dispatchers.Main) {
            delay(10_000)
            val current = cm?.primaryClip
            val stillOurs = current != null &&
                current.itemCount == 1 &&
                (current.getDescription().label?.contains("cryptosafe") == true ||
                    current.getItemAt(0).text?.toString() == text)
            if (stillOurs) {
                cm?.setPrimaryClip(sensitiveClipData("cryptosafe", ""))
            }
        }
    }
}
