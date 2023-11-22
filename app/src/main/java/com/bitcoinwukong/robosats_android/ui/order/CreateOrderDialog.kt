package com.bitcoinwukong.robosats_android.ui.order

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod
import com.bitcoinwukong.robosats_android.ui.components.WKDropdownMenu
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme
import com.bitcoinwukong.robosats_android.viewmodel.OrderParams

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
    val context = LocalContext.current

    var currentParams by remember {
        mutableStateOf(loadOrderParams(context))
    }

    Column {
        WKDropdownMenu(
            label = "Order Type",
            items = OrderType.values().toList(),
            selectedItem = currentParams.orderType,
            onItemSelected = {
                currentParams = currentParams.copy(orderType = it)
                saveOrderParams(context, currentParams)
            }
        )

        WKDropdownMenu(
            label = "Currency",
            items = Currency.values().toList(),
            selectedItem = currentParams.currency,
            onItemSelected = {
                currentParams = currentParams.copy(currency = it)
                saveOrderParams(context, currentParams)
            }
        )

        OutlinedTextField(
            value = currentParams.amount,
            onValueChange = {
                currentParams = currentParams.copy(amount = it)
                saveOrderParams(context, currentParams)
            },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        WKDropdownMenu(
            label = "Payment Method",
            items = PaymentMethod.values().toList(),
            selectedItem = currentParams.paymentMethod,
            onItemSelected = {
                currentParams = currentParams.copy(paymentMethod = it)
                saveOrderParams(context, currentParams)
            }
        )

        OutlinedTextField(
            value = currentParams.premium,
            onValueChange = {
                currentParams = currentParams.copy(premium = it)
                saveOrderParams(context, currentParams)
            },
            label = { Text("Premium") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        if (currentParams.paymentMethod == PaymentMethod.CUSTOM) {
            OutlinedTextField(
                value = currentParams.customPaymentMethod,
                onValueChange = {
                    currentParams = currentParams.copy(customPaymentMethod = it)
                    saveOrderParams(context, currentParams)
                },
                label = { Text("Custom Payment Method") }
            )
        }
        Button(
            onClick = {
                val orderAmount = currentParams.amount.toDoubleOrNull()
                val orderPremium = currentParams.premium.toDoubleOrNull()

                if (orderAmount != null && orderPremium != null) {
                    val orderData = OrderData(
                        type = currentParams.orderType,
                        currency = currentParams.currency,
                        amount = orderAmount,
                        paymentMethod = currentParams.paymentMethod,
                        premium = orderPremium,
                    )
                    onCreateOrder(orderData)
                }
            },
            enabled = currentParams.amount.isNotBlank() && currentParams.premium.isNotBlank()
        ) {
            Text("Create Order")
        }
    }
}

private fun saveOrderParams(context: Context, orderParams: OrderParams) {
    val sharedPrefs = context.getSharedPreferences("OrderParams", Context.MODE_PRIVATE)
    sharedPrefs.edit().apply {
        putString("orderType", orderParams.orderType.name)
        putString("currency", orderParams.currency.name)
        putString("amount", orderParams.amount)
        putString("paymentMethod", orderParams.paymentMethod.name)
        putString("premium", orderParams.premium)
        apply()
    }
}

private fun loadOrderParams(context: Context): OrderParams {
    val sharedPrefs = context.getSharedPreferences("OrderParams", Context.MODE_PRIVATE)
    return OrderParams(
        orderType = OrderType.valueOf(
            sharedPrefs.getString("orderType", OrderType.BUY.name) ?: OrderType.BUY.name
        ),
        currency = Currency.valueOf(
            sharedPrefs.getString("currency", Currency.USD.name) ?: Currency.USD.name
        ),
        amount = sharedPrefs.getString("amount", "") ?: "",
        paymentMethod = PaymentMethod.valueOf(
            sharedPrefs.getString(
                "paymentMethod",
                PaymentMethod.USDT.name
            ) ?: PaymentMethod.USDT.name
        ),
        premium = sharedPrefs.getString("premium", "") ?: ""
    )
}

@Preview(showBackground = true)
@Composable
fun CreateOrderContentPreview() {
    RobosatsAndroidTheme {
        CreateOrderContent({})
    }
}

