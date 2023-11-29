package com.bitcoinwukong.robosats_android.viewmodel

import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod
import java.time.LocalTime

data class OrderParams(
    var orderType: OrderType = OrderType.BUY,
    var currency: Currency = Currency.USD,
    var amount: String = "",
    var paymentMethod: PaymentMethod = PaymentMethod.USDT,
    var customPaymentMethod: String = "",
    var premium: String = "",
    // Target expiration time
    var expirationTime: LocalTime? = null,
)
