package com.bitcoinwukong.robosats_android.model

import android.util.Log
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bouncycastle.openpgp.PGPPrivateKey
import org.json.JSONObject

data class Robot(
    val token: String,
    val publicKey: String? = null,
    val encryptedPrivateKey: String? = null,
    val nickname: String? = null,
    val earnedRewards: Int = 0,
    val wantsStealth: Boolean = true,
    val found: Boolean = false,
    val tgEnabled: Boolean = false,
    val tgToken: String? = null,
    val tgBotName: String? = null,
    val activeOrderId: Int? = null,
    val lastOrderId: Int? = null,
    // Error message for fetching robot info failure
    val errorMessage: String? = null,
    var pgpPrivateKey: PGPPrivateKey? = null,
) {
    companion object {
        fun fromTokenAndJson(token: String, jsonObject: JSONObject): Robot {
            val encryptedKey = jsonObject.getString("encrypted_private_key")
            val robot = Robot(
                token = token,
                nickname = jsonObject.getString("nickname"),
                publicKey = jsonObject.getString("public_key"),
                encryptedPrivateKey = encryptedKey,
                earnedRewards = jsonObject.getInt("earned_rewards"),
                wantsStealth = jsonObject.getBoolean("wants_stealth"),
                tgEnabled = jsonObject.getBoolean("tg_enabled"),
                tgToken = jsonObject.getString("tg_token"),
                tgBotName = jsonObject.getString("tg_bot_name"),
                found = jsonObject.optBoolean("found"),
                activeOrderId = jsonObject.optInt("active_order_id", -1).takeIf { it != -1 },
                lastOrderId = jsonObject.optInt("last_order_id", -1).takeIf { it != -1 },
                pgpPrivateKey = PgpKeyManager.getPgpPrivateKey(encryptedKey, token)
            )

            if (robot.pgpPrivateKey == null) {
                // If decryption is in progress, wait for it
                CoroutineScope(Dispatchers.IO).launch {
                    robot.pgpPrivateKey = PgpKeyManager.waitForDecryption(encryptedKey)
                }
            }

            return robot
        }
    }

    suspend fun decryptMessage(
        encryptedMessage: String,
    ): String {
        if (pgpPrivateKey == null) {
            Log.d(
                "Robot",
                "Robot $token pgpPrivateKey is null, continue the private key decryption"
            )
            pgpPrivateKey = PgpKeyManager.waitForDecryption(encryptedPrivateKey!!)
        }

        return if (pgpPrivateKey != null) {
            Log.d("Robot", "Robot $token pgpPrivateKey decrypted, start message decryption")
            // Decrypt the message
            PgpKeyGenerator.decryptMessage(
                encryptedMessage.replace("\\", "\n"),
                pgpPrivateKey!!
            )
        } else {
            // Return empty string or an error message if decryption is not possible
            "Decryption Error: Private key not available."
        }
    }
}
