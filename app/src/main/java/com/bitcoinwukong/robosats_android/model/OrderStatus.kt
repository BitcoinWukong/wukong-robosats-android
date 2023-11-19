package com.bitcoinwukong.robosats_android.model

enum class OrderStatus(val code: Int, val description: String) {
    WAITING_FOR_MAKER_BOND(0, "Waiting for maker bond"),
    PUBLIC(1, "Public"),
    PAUSED(2, "Paused"),
    WAITING_FOR_TAKER_BOND(3, "Waiting for taker bond"),
    CANCELLED(4, "Cancelled"),
    EXPIRED(5, "Expired"),
    WAITING_FOR_TRADE_COLLATERAL_AND_BUYER_INVOICE(6, "Waiting for trade collateral and buyer invoice"),
    WAITING_ONLY_FOR_SELLER_TRADE_COLLATERAL(7, "Waiting only for seller trade collateral"),
    WAITING_ONLY_FOR_BUYER_INVOICE(8, "Waiting only for buyer invoice"),
    SENDING_FIAT_IN_CHATROOM(9, "Sending fiat - In chatroom"),
    FIAT_SENT_IN_CHATROOM(10, "Fiat sent - In chatroom"),
    IN_DISPUTE(11, "In dispute"),
    COLLABORATIVELY_CANCELLED(12, "Collaboratively cancelled"),
    SENDING_SATOSHIS_TO_BUYER(13, "Sending satoshis to buyer"),
    SUCCESSFUL_TRADE(14, "Successful trade"),
    FAILED_LIGHTNING_NETWORK_ROUTING(15, "Failed lightning network routing"),
    WAIT_FOR_DISPUTE_RESOLUTION(16, "Wait for dispute resolution"),
    MAKER_LOST_DISPUTE(17, "Maker lost dispute"),
    TAKER_LOST_DISPUTE(18, "Taker lost dispute");

    companion object {
        fun fromCode(code: Int): OrderStatus? {
            return values().find { it.code == code }
        }
    }
}
