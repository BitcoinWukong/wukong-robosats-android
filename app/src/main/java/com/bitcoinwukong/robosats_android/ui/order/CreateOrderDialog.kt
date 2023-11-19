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
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod
import com.bitcoinwukong.robosats_android.ui.components.WKDropdownMenu
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme

@Composable
fun CreateOrderDialog(
    onOrderCreated: (OrderType, Currency, Double, PaymentMethod) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Create Order") },
        text = {
            CreateOrderContent(onOrderCreated)
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
fun CreateOrderContent(onOrderCreated: (OrderType, Currency, Double, PaymentMethod) -> Unit) {
    var selectedOrderType by remember { mutableStateOf(OrderType.BUY) }
    var selectedCurrency by remember { mutableStateOf(Currency.USD) }
    var amount by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.AMAZON_USA_GIFTCARD) }
    var customPaymentMethod by remember {
        mutableStateOf("")
    }

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
                if (orderAmount != null) {
                    onOrderCreated(
                        selectedOrderType,
                        selectedCurrency,
                        orderAmount,
                        selectedPaymentMethod,
                    )
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
        // Provide a sample implementation for the onOrderCreated lambda
        CreateOrderContent { orderType, currency, amount, paymentMethod ->
            // For preview, we just print the inputs. In actual app, this will create the order.
            println("Order Type: $orderType, Currency: $currency, Amount: $amount, Payment Method: $paymentMethod")
        }
    }
}

