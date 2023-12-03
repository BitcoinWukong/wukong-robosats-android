package com.bitcoinwukong.robosats_android.model

data class MessageData(
    val index: Int,
    val time: String,
    val encryptedMessage: String,
    val nick: String,
    var message: String = "",
)
