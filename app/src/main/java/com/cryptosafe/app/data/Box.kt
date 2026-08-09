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
    
    
    
    val lockMode: String = "always",
    val lockTimeoutMinutes: Int? = null,
    
    
    
    
    val encryptionSalt: String? = null
)
