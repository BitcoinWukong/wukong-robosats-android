package com.bitcoinwukong.robosats_android.model

data class ChatMessagesResponse(
    val messages: List<Message>,
    val peerConnected: Boolean,
    val peerPublicKey: String,
    val offset: Int?
)
