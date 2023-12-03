package com.bitcoinwukong.robosats_android.model

import android.util.Log
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

fun errorRobot(token: String, errorMessage: String): Robot {
    return Robot(
        token,
        publicKey = "errorRobotWithInvalidPublicKey",
        errorMessage = errorMessage
    )
}

data class Robot(
    val token: String,
    val publicKey: String,
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
    // PGP keys
    var privateKeyBundle: PGPPrivateKeyBundle? = null,
    var publicKeyBundle: PGPPublicKeyBundle? = null,
) {
    init {
        if (publicKeyBundle == null && errorMessage == null) {
            this.publicKeyBundle = PgpKeyManager.getPgpPublicKey(publicKey)
        }
    }
    companion object {
        fun fromTokenAndJson(token: String, jsonObject: JSONObject): Robot {
            val publicKey = jsonObject.getString("public_key")
            val encryptedKey = jsonObject.getString("encrypted_private_key")

            val robot = Robot(
                token = token,
                nickname = jsonObject.getString("nickname"),
                publicKey = publicKey,
                encryptedPrivateKey = encryptedKey,
                earnedRewards = jsonObject.getInt("earned_rewards"),
                wantsStealth = jsonObject.getBoolean("wants_stealth"),
                tgEnabled = jsonObject.getBoolean("tg_enabled"),
                tgToken = jsonObject.getString("tg_token"),
                tgBotName = jsonObject.getString("tg_bot_name"),
                found = jsonObject.optBoolean("found"),
                activeOrderId = jsonObject.optInt("active_order_id", -1).takeIf { it != -1 },
                lastOrderId = jsonObject.optInt("last_order_id", -1).takeIf { it != -1 },
                privateKeyBundle = PgpKeyManager.getPgpPrivateKey(encryptedKey, token),
                publicKeyBundle = PgpKeyManager.getPgpPublicKey(publicKey),
            )

            if (robot.privateKeyBundle == null) {
                // If decryption is in progress, wait for it
                CoroutineScope(Dispatchers.IO).launch {
                    robot.privateKeyBundle = PgpKeyManager.waitForDecryption(encryptedKey)
                    if (robot.privateKeyBundle != null) {
                        onPrivateKeyDecrypted(robot)
                    }
                }
            }
            return robot
        }

        var onPrivateKeyDecrypted: (Robot) -> Unit = {}
    }

    suspend fun decryptMessage(
        messageData: MessageData,
    ): MessageData {
        if (privateKeyBundle == null) {
            Log.d(
                "Robot",
                "Robot $token pgpPrivateKey is null, continue the private key decryption"
            )
            privateKeyBundle = PgpKeyManager.waitForDecryption(encryptedPrivateKey!!)
        }

        messageData.message = PgpKeyGenerator.decryptMessage(
            messageData.message.replace("\\", "\n"),
            privateKeyBundle!!.encryptionKey
        )

        return messageData
    }

    suspend fun encryptMessage(
        message: String,
        peerPublicKey: String,
    ): String {
        val peerPublicKeyBundle = PgpKeyManager.getPgpPublicKey(peerPublicKey)
        if (privateKeyBundle == null) {
            Log.d(
                "Robot",
                "Robot $token privateKeyBundle is null, continue the private key decryption"
            )
            privateKeyBundle = PgpKeyManager.waitForDecryption(encryptedPrivateKey!!)
        }

        return if (privateKeyBundle != null) {
            Log.d("Robot", "Robot $token privateKeyBundle decrypted, start message encryption")
            // Encrypt the message
            PgpKeyGenerator.encryptMessage(
                message,
                privateKeyBundle!!.signingKey,
                publicKeyBundle!!.encryptionKey,
                peerPublicKeyBundle.encryptionKey,
            )
        } else {
            // Return empty string or an error message if decryption is not possible
            "Decryption Error: Private key not available."
        }
    }
}
