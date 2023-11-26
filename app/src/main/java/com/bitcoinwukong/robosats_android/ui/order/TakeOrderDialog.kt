package com.bitcoinwukong.robosats_android.ui.order

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod

@Composable
fun TakeOrderDialog(orderData: OrderData?, onDismiss: () -> Unit, onTakeOrder: () -> Unit) {
    if (orderData != null) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("Take Order") },
            text = {
                Column {
                    if (orderData.type == OrderType.BUY) {
                        Text("You're Selling Bitcoin")
                    } else {
                        Text("You're Buying Bitcoin")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Order ID: ${orderData.id ?: "N/A"}")
                    Text("Currency: ${orderData.currency.code}")
                    Text("Amount: ${orderData.formattedAmount()}")
                    if (orderData.paymentMethod == PaymentMethod.CUSTOM) {
                        Text("Payment: ${orderData.customPaymentMethod ?: "N/A"}")
                    } else {
                        Text("Payment: ${orderData.paymentMethod.methodName}")
                    }
                    Text("Price: ${orderData.price ?: "N/A"}")
                    Text("Premium: ${orderData.premium ?: "N/A"}%")
                }
            },
            confirmButton = {
                Button(onClick = { onTakeOrder() }) {
                    Text("Accept")
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