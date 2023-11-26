package com.bitcoinwukong.robosats_android.ui.order

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bitcoinwukong.robosats_android.model.OrderData

@Composable
fun TakeOrderDialog(orderData: OrderData?, onDismiss: () -> Unit, onTakeOrder: () -> Unit) {
    if (orderData != null) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("Take Order") },
            text = { /* Display order details here */ },
            confirmButton = {
                Button(onClick = { onTakeOrder() }) {
                    Text("Take Order")
                }
            }
        )
    }
}