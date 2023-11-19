package com.bitcoinwukong.robosats_android

import com.bitcoinwukong.robosats_android.model.PaymentMethod
import org.junit.Assert
import org.junit.Test

class PaymentMethodTest {
    @Test
    fun testParsePaymentMethod() {
        Assert.assertEquals(PaymentMethod.AMAZON_USA_GIFTCARD, PaymentMethod.fromString("Amazon USA GiftCard"))
    }
}