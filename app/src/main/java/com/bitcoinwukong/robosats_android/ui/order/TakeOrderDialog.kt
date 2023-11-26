package com.bitcoinwukong.robosats_android.ui.order

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.PaymentMethod

@Composable
fun TakeOrderDialog(orderData: OrderData?, onDismiss: () -> Unit, onTakeOrder: () -> Unit) {
    if (orderData != null) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("Take Order") },
            text = {
                Column {
                    Text("Order ID: ${orderData.id ?: "N/A"}")
                    Text("Type: ${orderData.type.name}")
                    Text("Currency: ${orderData.currency.code}")
                    Text("Amount: ${orderData.amount ?: "N/A"}")
                    Text("Min Amount: ${orderData.minAmount ?: "N/A"}")
                    Text("Max Amount: ${orderData.maxAmount ?: "N/A"}")
                    if (orderData.paymentMethod == PaymentMethod.CUSTOM) {
                        Text("Payment Method: ${orderData.customPaymentMethod ?: "N/A"}")
                    } else {
                        Text("Payment Method: ${orderData.paymentMethod.name}")
                    }
                    Text("Price: ${orderData.price ?: "N/A"}")
                    Text("Premium: ${orderData.premium ?: "N/A"}%")
                }
            },
            confirmButton = {
                Button(onClick = { onTakeOrder() }) {
                    Text("Take Order")
                }
            },
            dismissButton = {
                Button(onClick = { onDismiss() }) {
                    Text("Cancel")
                }
            }
        )
    }
}