package com.example.sleepwellapp.datalayer

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_prefs")

class UserPreferences(context: Context, private val cryptoManager: CryptoManager) {
    private val dataStore = context.dataStore

    companion object {
        val USERNAME_KEY = stringPreferencesKey("username")
        val UID_KEY = stringPreferencesKey("password")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    val usernameFlow: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[USERNAME_KEY]?.let { decryptString(it) }
        }

    val passwordFlow: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[UID_KEY]?.let { decryptString(it) }
        }

    val darkModeFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    suspend fun saveDarkModePreference(isDarkMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }

    suspend fun saveCredentials(username: String, uid: String) {
        val encryptedUsername = encryptString(username)
        val encryptedUID = encryptString(uid)
        dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = encryptedUsername
            preferences[UID_KEY] = encryptedUID
        }
    }

    suspend fun clearCredentials() {
        dataStore.edit { preferences ->
            preferences.remove(USERNAME_KEY)
            preferences.remove(UID_KEY)
        }
    }

    suspend fun clearAllData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun encryptString(value: String): String {
        val (iv, encryptedData) = cryptoManager.encrypt(value.toByteArray())
        return "${iv.toBase64()}:${encryptedData.toBase64()}"
    }

    private fun decryptString(value: String): String {
        val (iv, encryptedData) = value.split(":").let {
            it[0].fromBase64() to it[1].fromBase64()
        }
        return String(cryptoManager.decrypt(iv, encryptedData))
    }

    private fun ByteArray.toBase64(): String = android.util.Base64.encodeToString(this, android.util.Base64.DEFAULT)
    private fun String.fromBase64(): ByteArray = android.util.Base64.decode(this, android.util.Base64.DEFAULT)
}
