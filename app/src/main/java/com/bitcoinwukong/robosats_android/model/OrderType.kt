package com.bitcoinwukong.robosats_android.model

enum class OrderType(val value: Int) {
    BUY(0),
    SELL(1);

    companion object {
        fun fromInt(value: Int): OrderType {
            return values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid type value: $value")
        }
    }
}
