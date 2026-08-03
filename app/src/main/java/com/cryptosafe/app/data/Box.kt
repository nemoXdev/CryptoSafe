package com.cryptosafe.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boxes")
data class Box(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val autoDeleteHours: Int? = null,
    // "always" = يطلب كلمة المرور كل مرة تدخل الصندوق (الأكثر أماناً، الافتراضي)
    // "timed"  = يتذكرها لمدة lockTimeoutMinutes بعد آخر فتح ناجح ثم يطلبها مجدداً
    // "never"  = يتذكرها طوال الجلسة الحالية (لحد ما يقفل التطبيق تلقائياً أو يدوياً)
    val lockMode: String = "always",
    val lockTimeoutMinutes: Int? = null
)
