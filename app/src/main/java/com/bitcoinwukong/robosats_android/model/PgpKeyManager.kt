package com.bitcoinwukong.robosats_android.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.deserializePGPPrivateKey
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.serializePGPPrivateKey
import kotlinx.coroutines.*
import org.bouncycastle.openpgp.PGPPrivateKey
import org.json.JSONObject

object PgpKeyManager {
    private val decryptionMap = mutableMapOf<String, PGPPrivateKey?>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val decryptionInProgress = mutableSetOf<String>()
    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("PgpKeys", Context.MODE_PRIVATE)
        loadDecryptionMap()
    }

    private fun loadDecryptionMap() {
        val serializedMap = sharedPreferences.getString("decryptionMap", null)
        serializedMap?.let {
            val jsonObject = JSONObject(it)
            for (key in jsonObject.keys()) {
                val serializedKey = jsonObject.getString(key)
                decryptionMap[key] = deserializePGPPrivateKey(serializedKey)

                Log.d(
                    "PgpKeyManager",
                    "Loaded private key for $key from sharedPreferences"
                )
            }
        }
    }

    private fun saveDecryptionMap() {
        val serializedMap = JSONObject()
        decryptionMap.forEach { (key, privateKey) ->
            privateKey?.let {
                Log.d(
                    "PgpKeyManager",
                    "Saved private key for $key to sharedPreferences"
                )
                serializedMap.put(key, serializePGPPrivateKey(it))
            }
        }
        sharedPreferences.edit().putString("decryptionMap", serializedMap.toString()).apply()
    }

    fun getPgpPrivateKey(encryptedKey: String, token: String): PGPPrivateKey? {
        return decryptionMap[encryptedKey] ?: startDecryption(encryptedKey, token)
    }

    private fun startDecryption(encryptedKey: String, token: String): PGPPrivateKey? {
        if (decryptionInProgress.add(encryptedKey)) {
            coroutineScope.launch {
                Log.d(
                    "PgpKeyManager",
                    "Start decrypting private key for token $token, $encryptedKey"
                )
                val pgpKey = PgpKeyGenerator.decryptPrivateKeys(encryptedKey, token).second
                Log.d(
                    "PgpKeyManager",
                    "Done decrypting private key for token $token, $encryptedKey"
                )
                decryptionMap[encryptedKey] = pgpKey
                saveDecryptionMap()
                decryptionInProgress.remove(encryptedKey)
            }
        }
        return null
    }

    suspend fun waitForDecryption(encryptedKey: String): PGPPrivateKey? {
        while (encryptedKey in decryptionInProgress) {
            delay(100) // Or a suitable delay
        }
        return decryptionMap[encryptedKey]
    }
}
