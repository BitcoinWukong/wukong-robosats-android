package com.bitcoinwukong.robosats_android.model

data class ChatMessagesResponse(
    val messageData: List<MessageData>,
    val peerConnected: Boolean,
    val peerPublicKey: String,
    val offset: Int?
)
