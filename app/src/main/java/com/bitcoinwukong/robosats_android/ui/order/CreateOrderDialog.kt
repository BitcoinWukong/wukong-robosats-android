package com.bitcoinwukong.robosats_android.ui.order

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod
import com.bitcoinwukong.robosats_android.ui.components.WKDropdownMenu
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme

@Composable
fun CreateOrderDialog(
    onCreateOrder: (OrderData) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Create Order") },
        text = {
            CreateOrderContent(onCreateOrder)
        },
        confirmButton = {
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateOrderContent(onCreateOrder: (OrderData) -> Unit) {
    var selectedOrderType by remember { mutableStateOf(OrderType.BUY) }
    var selectedCurrency by remember { mutableStateOf(Currency.USD) }
    var amount by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.AMAZON_USA_GIFTCARD) }
    var customPaymentMethod by remember {
        mutableStateOf("")
    }
    var premium by remember { mutableStateOf("") }


    Column {
        WKDropdownMenu(
            label = "Order Type",
            items = OrderType.values().toList(),
            selectedItem = selectedOrderType,
            onItemSelected = { selectedOrderType = it }
        )

        WKDropdownMenu(
            label = "Currency",
            items = Currency.values().toList(),
            selectedItem = selectedCurrency,
            onItemSelected = { selectedCurrency = it }
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        WKDropdownMenu(
            label = "Payment Method",
            items = PaymentMethod.values().toList(),
            selectedItem = selectedPaymentMethod,
            onItemSelected = { selectedPaymentMethod = it }
        )

        OutlinedTextField(
            value = premium,
            onValueChange = { premium = it },
            label = { Text("Premium") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        if (selectedPaymentMethod == PaymentMethod.CUSTOM) {
            OutlinedTextField(
                value = customPaymentMethod,
                onValueChange = { customPaymentMethod = it },
                label = { Text("Custom Payment Method") }
            )
        }
        Button(
            onClick = {
                val orderAmount = amount.toDoubleOrNull()
                val orderPremium = premium.toDoubleOrNull()

                if (orderAmount != null && orderPremium != null) {
                    val orderData = OrderData(
                        type = selectedOrderType,
                        currency = selectedCurrency,
                        amount = orderAmount,
                        paymentMethod = selectedPaymentMethod,
                        premium = orderPremium,
                    )
                    onCreateOrder(orderData)
                }
            },
            enabled = amount.isNotBlank()
        ) {
            Text("Create Order")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateOrderContentPreview() {
    RobosatsAndroidTheme {
        CreateOrderContent { orderData ->
            println(orderData)
        }
    }
}

