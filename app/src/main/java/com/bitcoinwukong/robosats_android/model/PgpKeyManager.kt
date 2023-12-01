package com.bitcoinwukong.robosats_android.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.deserializePGPPrivateKey
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.readPublicKey
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.serializePGPPrivateKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

object PgpKeyManager {
    private val decryptionMap = mutableMapOf<String, PGPPrivateKeyBundle?>()
    private val publicKeyMap = mutableMapOf<String, PGPPublicKeyBundle>()
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
                val serializedBundle = jsonObject.getJSONObject(key)
                val serializedSigningKey = serializedBundle.getString("signingKey")
                val serializedEncryptionKey = serializedBundle.getString("encryptionKey")

                val signingKey = deserializePGPPrivateKey(serializedSigningKey)
                val encryptionKey = deserializePGPPrivateKey(serializedEncryptionKey)

                decryptionMap[key] = PGPPrivateKeyBundle(signingKey, encryptionKey)

                Log.d("PgpKeyManager", "Loaded private key bundle for $key from sharedPreferences")
            }
        }
    }

    private fun saveDecryptionMap() {
        val serializedMap = JSONObject()
        decryptionMap.forEach { (key, privateKeyBundle) ->
            privateKeyBundle?.let {
                val bundleObject = JSONObject().apply {
                    put("signingKey", serializePGPPrivateKey(it.signingKey))
                    put("encryptionKey", serializePGPPrivateKey(it.encryptionKey))
                }

                Log.d("PgpKeyManager", "Saved private key bundle for $key to sharedPreferences")
                serializedMap.put(key, bundleObject)
            }
        }
        sharedPreferences.edit().putString("decryptionMap", serializedMap.toString()).apply()
    }

    fun getPgpPrivateKey(encryptedKey: String, token: String): PGPPrivateKeyBundle? {
        return decryptionMap[encryptedKey] ?: startDecryption(encryptedKey, token)
    }

    fun getPgpPublicKey(publicKey: String): PGPPublicKeyBundle {
        return publicKeyMap.getOrPut(publicKey) { readPublicKey(publicKey) }
    }

    private fun startDecryption(encryptedKey: String, token: String): PGPPrivateKeyBundle? {
        if (decryptionInProgress.add(encryptedKey)) {
            coroutineScope.launch {
                Log.d(
                    "PgpKeyManager",
                    "Start decrypting private key for token $token, $encryptedKey"
                )
                val pgpKey = PgpKeyGenerator.decryptPrivateKeys(encryptedKey, token)
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

    suspend fun waitForDecryption(encryptedKey: String): PGPPrivateKeyBundle? {
        while (encryptedKey in decryptionInProgress) {
            delay(100) // Or a suitable delay
        }
        return decryptionMap[encryptedKey]
    }
}
