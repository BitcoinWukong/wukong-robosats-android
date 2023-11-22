package com.bitcoinwukong.robosats_android.model

import com.bitcoinwukong.robosats_android.utils.parseDateTime
import org.json.JSONObject
import java.time.LocalDateTime

data class OrderData(
    val id: Int? = null,
    val type: OrderType,
    val currency: Currency,
    val amount: Double? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CUSTOM,
    val customPaymentMethod: String? = null,
    val price: Double? = null,
    val premium: Double? = null,
    // Additional fields for detailed view
    val status: OrderStatus? = null,
    val createdAt: LocalDateTime? = null,
    val expiresAt: LocalDateTime? = null,
    val hasRange: Boolean = false,
    val isExplicit: Boolean = false,
    val satoshis: Double? = null,
    val maker: Int? = null,
    val taker: Int? = null,
    val escrowDuration: Int? = null,
    val bondSize: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val bondInvoice: String? = null,
    val bondSats: Int? = null,
    val escrowInvoice: String? = null,
    val escrowSats: Int? = null,
    val isMaker: Boolean = false,
    val isTaker: Boolean = false,
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): OrderData {
            // Using null for missing or non-parseable values
            val amount = jsonObject.optString("amount").toDoubleOrNull()
            val minAmount = jsonObject.optString("min_amount").toDoubleOrNull()
            val maxAmount = jsonObject.optString("max_amount").toDoubleOrNull()
            val premium = jsonObject.optString("premium").toDoubleOrNull()

            val paymentMethod = PaymentMethod.fromString(jsonObject.getString("payment_method"))
            val customMethod =
                if (paymentMethod == PaymentMethod.CUSTOM) jsonObject.getString("payment_method") else null

            val status = jsonObject.optInt("status", -1).takeIf { it != -1 }
                ?.let { OrderStatus.fromCode(it) }

            return OrderData(
                id = jsonObject.getInt("id"),
                type = OrderType.fromInt(jsonObject.getInt("type")),
                currency = Currency.fromId(jsonObject.getInt("currency")),
                amount = amount,
                minAmount = minAmount,
                maxAmount = maxAmount,
                paymentMethod = paymentMethod,
                customPaymentMethod = customMethod,
                price = jsonObject.optDouble("price", 0.0),
                premium = premium,
                // Parse additional fields with defaults
                status = status,
                createdAt = parseDateTime(jsonObject.optString("created_at", "")),
                expiresAt = parseDateTime(jsonObject.optString("expires_at", "")),
                hasRange = jsonObject.optBoolean("has_range", false),
                isExplicit = jsonObject.optBoolean("is_explicit", false),
                satoshis = jsonObject.optDouble("satoshis", 0.0),
                maker = jsonObject.optInt("maker", -1).takeIf { it != -1 },
                taker = jsonObject.optInt("taker", -1).takeIf { it != -1 },
                escrowDuration = jsonObject.optInt("escrow_duration", -1).takeIf { it != -1 },
                bondSize = jsonObject.optDouble("bond_size", 0.0),
                latitude = jsonObject.optString("latitude").toDoubleOrNull(),
                longitude = jsonObject.optString("longitude").toDoubleOrNull(),
                bondInvoice = jsonObject.optString("bond_invoice", "").takeIf { it.isNotEmpty() },
                bondSats = jsonObject.optInt("bond_satoshis", -1).takeIf { it != -1 },
                escrowInvoice = jsonObject.optString("escrow_invoice", "")
                    .takeIf { it.isNotEmpty() },
                escrowSats = jsonObject.optInt("escrow_satoshis", -1).takeIf { it != -1 },
                isMaker = jsonObject.optBoolean("is_maker", false),
                isTaker = jsonObject.optBoolean("is_taker", false),
            )
        }
    }

    fun isWaitingForSellerCollateral(): Boolean {
        return (status == OrderStatus.WAITING_FOR_TRADE_COLLATERAL_AND_BUYER_INVOICE
                || status == OrderStatus.WAITING_ONLY_FOR_SELLER_TRADE_COLLATERAL)
    }

    fun isSeller(): Boolean {
        return (type == OrderType.SELL && isMaker) || (type == OrderType.BUY && isTaker)
    }

    fun formattedAmount(): String {
        return formatAmount(amount, minAmount, maxAmount)
    }

    override fun toString(): String {
        val orderType = type.toString()

        val formattedAmount = formatAmount(amount, minAmount, maxAmount)
        val formattedPrice = String.format("%.0f", price)
        val formattedPremium = premium?.let { String.format("%.1f%%", it) } ?: "N/A"

        return "#$id, $orderType $currency $formattedAmount, $paymentMethod, $formattedPrice, $formattedPremium"
    }

    private fun formatAmount(amount: Double?, minAmount: Double?, maxAmount: Double?): String {
        return when {
            amount != null -> String.format("%.0f", amount)
            minAmount != null && maxAmount != null -> "${
                String.format(
                    "%.0f",
                    minAmount
                )
            } ~ ${String.format("%.0f", maxAmount)}"

            else -> "N/A"
        }
    }
}
