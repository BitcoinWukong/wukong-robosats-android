package com.bitcoinwukong.robosats_android.model

import org.json.JSONObject

data class Robot(
    val token: String,
    val publicKey: String ?= null,
    val encryptedPrivateKey: String ?= null,
    val nickname: String ?= null,
    val earnedRewards: Int = 0,
    val wantsStealth: Boolean = true,
    val found: Boolean = false,
    val tgEnabled: Boolean = false,
    val tgToken: String ?= null,
    val tgBotName: String ?= null,
    val activeOrderId: Int? = null,
    val lastOrderId: Int? = null,
    // Error message for fetching robot info failure
    val errorMessage: String? = null
) {
    companion object {
        fun fromTokenAndJson(token: String, jsonObject: JSONObject): Robot {
            return Robot(
                token = token,
                nickname = jsonObject.getString("nickname"),
                publicKey = jsonObject.getString("public_key"),
                encryptedPrivateKey = jsonObject.getString("encrypted_private_key"),
                earnedRewards = jsonObject.getInt("earned_rewards"),
                wantsStealth = jsonObject.getBoolean("wants_stealth"),
                tgEnabled = jsonObject.getBoolean("tg_enabled"),
                tgToken = jsonObject.getString("tg_token"),
                tgBotName = jsonObject.getString("tg_bot_name"),

                found = jsonObject.optBoolean("found"),
                activeOrderId = jsonObject.optInt("active_order_id", -1).takeIf { it != -1 },
                lastOrderId = jsonObject.optInt("last_order_id", -1).takeIf { it != -1 }
            )
        }
    }
}
