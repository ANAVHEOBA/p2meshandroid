package com.example.p2meshandroid.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Handles persistent storage for wallet data.
 *
 * Storage strategy:
 * - Secret key: EncryptedSharedPreferences (secure, encrypted)
 * - Wallet state: Internal file storage (binary blob)
 *
 * Data persists until:
 * - App is uninstalled
 * - User clears app data
 * - Factory reset
 */
class WalletStorage(private val context: Context) {

    companion object {
        private const val SECURE_PREFS_NAME = "wallet_secure_prefs"
        private const val KEY_SECRET_KEY = "secret_key"
        private const val KEY_HAS_WALLET = "has_wallet"
        private const val STATE_FILE_NAME = "wallet_state.bin"
    }

    // Master key for encryption
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    // Encrypted SharedPreferences for sensitive data (secret key)
    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // File for wallet state
    private val stateFile: File
        get() = File(context.filesDir, STATE_FILE_NAME)

    /**
     * Check if a wallet exists in storage
     */
    fun hasWallet(): Boolean {
        return securePrefs.getBoolean(KEY_HAS_WALLET, false) &&
               getSecretKey() != null
    }

    /**
     * Save the wallet's secret key (encrypted)
     */
    fun saveSecretKey(secretKey: ByteArray) {
        val encoded = Base64.encodeToString(secretKey, Base64.NO_WRAP)
        securePrefs.edit()
            .putString(KEY_SECRET_KEY, encoded)
            .putBoolean(KEY_HAS_WALLET, true)
            .apply()
    }

    /**
     * Get the wallet's secret key
     */
    fun getSecretKey(): ByteArray? {
        val encoded = securePrefs.getString(KEY_SECRET_KEY, null) ?: return null
        return try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save wallet state (vault, mesh state, nonce) to file
     */
    fun saveWalletState(state: ByteArray) {
        try {
            stateFile.writeBytes(state)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get wallet state from file
     */
    fun getWalletState(): ByteArray? {
        return try {
            if (stateFile.exists()) {
                stateFile.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Clear all wallet data (for reset/logout)
     */
    fun clearAll() {
        securePrefs.edit()
            .remove(KEY_SECRET_KEY)
            .putBoolean(KEY_HAS_WALLET, false)
            .apply()

        if (stateFile.exists()) {
            stateFile.delete()
        }
    }

    /**
     * Get storage info for debugging
     */
    fun getStorageInfo(): StorageInfo {
        return StorageInfo(
            hasWallet = hasWallet(),
            secretKeyExists = getSecretKey() != null,
            stateFileExists = stateFile.exists(),
            stateFileSize = if (stateFile.exists()) stateFile.length() else 0
        )
    }
}

/**
 * Info about current storage state
 */
data class StorageInfo(
    val hasWallet: Boolean,
    val secretKeyExists: Boolean,
    val stateFileExists: Boolean,
    val stateFileSize: Long
)
