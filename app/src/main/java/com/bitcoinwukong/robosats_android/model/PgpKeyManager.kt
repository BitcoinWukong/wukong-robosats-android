package com.bitcoinwukong.robosats_android.model

import android.util.Log
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bouncycastle.openpgp.PGPPrivateKey

object PgpKeyManager {
    private val decryptionMap = mutableMapOf<String, PGPPrivateKey?>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val decryptionInProgress = mutableSetOf<String>()

    fun getPgpPrivateKey(encryptedKey: String, token: String): PGPPrivateKey? {
        return decryptionMap[encryptedKey] ?: startDecryption(encryptedKey, token)
    }

    private fun startDecryption(encryptedKey: String, token: String): PGPPrivateKey? {
        if (decryptionInProgress.add(encryptedKey)) {
            coroutineScope.launch {
                Log.d("PgpKeyManager", "Start decrypting private key for token $token, $encryptedKey")
                val pgpKey = PgpKeyGenerator.decryptPrivateKey(encryptedKey, token)
                Log.d("PgpKeyManager", "Done decrypting private key for token $token, $encryptedKey")
                decryptionMap[encryptedKey] = pgpKey
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
