package com.cryptosafe.app.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


class KeystoreEncryptedPrefs private constructor(
    private val raw: SharedPreferences,
    private val getKey: () -> SecretKey
) : SharedPreferences {

    private val transform = "AES/GCM/NoPadding"
    private val prefix = "e_"
    private val gcmTagBits = 128

    private fun encKey(key: String): String = prefix + hash(key)

    private fun hash(s: String): String {
        val d = MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(d, Base64.NO_WRAP)
            .substring(0, 22)
            .replace('/', '_')
    }

    private fun encrypt(key: String, type: Char, value: String): String {
        val payload = JSONObject()
            .put("k", key)
            .put("t", type.toString())
            .put("v", value)
            .toString()
        val cipher = Cipher.getInstance(transform)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val out = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(entry: String): Triple<String, Char, String> {
        val parts = entry.split(":")
        if (parts.size != 2) throw IllegalArgumentException("bad entry")
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val data = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(transform)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(gcmTagBits, iv))
        val json = String(cipher.doFinal(data), Charsets.UTF_8)
        val obj = JSONObject(json)
        return Triple(
            obj.getString("k"),
            obj.getString("t").first(),
            obj.getString("v")
        )
    }

    private fun readEntry(key: String): Triple<String, Char, String>? {
        val rawVal = raw.getString(encKey(key), null) ?: return null
        return try {
            decrypt(rawVal)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseStringSet(value: String): Set<String> {
        val arr = JSONArray(value)
        val set = HashSet<String>(arr.length())
        for (i in 0 until arr.length()) set.add(arr.getString(i))
        return set
    }

    override fun getAll(): MutableMap<String, Any?> {
        val result = HashMap<String, Any?>()
        for ((rawKey, rawVal) in raw.getAll()) {
            if (!rawKey.startsWith(prefix)) continue
            if (rawVal !is String) continue
            try {
                val (k, t, v) = decrypt(rawVal)
                result[k] = when (t) {
                    's' -> v
                    'i' -> v.toInt()
                    'l' -> v.toLong()
                    'f' -> v.toFloat()
                    'b' -> v == "1"
                    't' -> parseStringSet(v)
                    else -> null
                }
            } catch (_: Exception) {
            }
        }
        return result
    }

    override fun getString(key: String, defValue: String?): String? {
        val e = readEntry(key) ?: return defValue
        return if (e.second == 's') e.third else defValue
    }

    override fun getStringSet(key: String, defValue: Set<String>?): Set<String>? {
        val e = readEntry(key) ?: return defValue
        return if (e.second == 't') parseStringSet(e.third) else defValue
    }

    override fun getInt(key: String, defValue: Int): Int {
        val e = readEntry(key) ?: return defValue
        return if (e.second == 'i') e.third.toInt() else defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        val e = readEntry(key) ?: return defValue
        return if (e.second == 'l') e.third.toLong() else defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        val e = readEntry(key) ?: return defValue
        return if (e.second == 'f') e.third.toFloat() else defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        val e = readEntry(key) ?: return defValue
        return if (e.second == 'b') e.third == "1" else defValue
    }

    override fun contains(key: String): Boolean = raw.contains(encKey(key))

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        raw.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        raw.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private inner class Editor : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, String?>()
        private var cleared = false

        private fun put(key: String, type: Char, value: String): Editor {
            pending[encKey(key)] = encrypt(key, type, value)
            return this
        }

        override fun putString(key: String, value: String?): Editor {
            if (value == null) return remove(key)
            return put(key, 's', value)
        }

        override fun putStringSet(key: String, values: Set<String>?): Editor {
            if (values == null) return remove(key)
            val arr = JSONArray()
            values.forEach { arr.put(it) }
            return put(key, 't', arr.toString())
        }

        override fun putInt(key: String, value: Int): Editor = put(key, 'i', value.toString())

        override fun putLong(key: String, value: Long): Editor = put(key, 'l', value.toString())

        override fun putFloat(key: String, value: Float): Editor = put(key, 'f', value.toString())

        override fun putBoolean(key: String, value: Boolean): Editor =
            put(key, 'b', if (value) "1" else "0")

        override fun remove(key: String): Editor {
            pending[encKey(key)] = null
            return this
        }

        override fun clear(): Editor {
            cleared = true
            return this
        }

        override fun commit(): Boolean {
            val ed = raw.edit()
            if (cleared) ed.clear()
            for ((k, v) in pending) {
                if (v == null) ed.remove(k) else ed.putString(k, v)
            }
            return ed.commit()
        }

        override fun apply() {
            val ed = raw.edit()
            if (cleared) ed.clear()
            for ((k, v) in pending) {
                if (v == null) ed.remove(k) else ed.putString(k, v)
            }
            ed.apply()
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "cryptosafe_prefs_key"

        
        fun create(context: Context, name: String): KeystoreEncryptedPrefs? = try {
            val raw = context.applicationContext
                .getSharedPreferences(name, Context.MODE_PRIVATE)
            KeystoreEncryptedPrefs(raw) { getOrCreateKey() }
        } catch (_: Exception) {
            null
        }

        private fun getOrCreateKey(): SecretKey {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            val gen = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            gen.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            return gen.generateKey()
        }
    }
}
