package com.bitcoinwukong.robosats_android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitcoinwukong.robosats_android.mocks.MockSharedViewModel
import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.ui.order.TakeOrderDialog
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MarketScreen(viewModel: ISharedViewModel = viewModel()) {
    val orders by viewModel.orders.observeAsState(emptyList())
    val isUpdating by viewModel.isUpdating.observeAsState(false)
    val isTorReady by viewModel.isTorReady.observeAsState(false)
    val lastUpdated by viewModel.lastUpdated.observeAsState()
    val selectedRobot: Robot? by viewModel.selectedRobot.observeAsState(null)
    var showAlert by remember { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Sell", "Buy")

    var selectedOrder: OrderData? by remember { mutableStateOf(null) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
    ) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = index == selectedTabIndex,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn {
                items(orders.filter { order ->
                    (selectedTabIndex == 0 && order.type == OrderType.BUY) ||
                            (selectedTabIndex == 1 && order.type == OrderType.SELL)
                }) { order ->
                    OrderRow(order) {
                        selectedOrder = order
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getTorStatusText(isTorReady, lastUpdated),
                style = MaterialTheme.typography.bodyMedium
            )

            if (!isTorReady || isUpdating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Button(
                    onClick = { viewModel.fetchOrders() },
                    modifier = Modifier
                ) {
                    Text(text = "Refresh")
                }
            }
        }
    }

    // Show dialog when an order is selected
    TakeOrderDialog(
        orderData = selectedOrder,
        onDismiss = { selectedOrder = null },
        onTakeOrder = {
            val currentOrder = selectedOrder ?: return@TakeOrderDialog

            if (selectedRobot?.pgpPrivateKey == null) {
                showAlert = true
            } else {
                viewModel.takeOrder(currentOrder)
            }
            selectedOrder = null
        }
    )

    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = { Text("Alert") },
            text = { Text("No active robot available, please create a robot or wait until its loading is completed") },
            confirmButton = {
                Button(onClick = { showAlert = false }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun getTorStatusText(isTorReady: Boolean, lastUpdated: LocalDateTime?): String {
    return if (isTorReady) {
        if (lastUpdated != null) {
            "Last updated: \n${lastUpdated.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}"
        } else {
            "Updating..."
        }
    } else {
        "Starting tor..."
    }
}

@Composable
fun OrderRow(orderData: OrderData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = orderData.id.toString(),
            modifier = Modifier.width(55.dp),
            textAlign = TextAlign.Left
        )
        Text(
            text = orderData.currency.code,
            modifier = Modifier.width(35.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = orderData.formattedAmount(),
            modifier = Modifier.width(100.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = if (orderData.paymentMethod == PaymentMethod.CUSTOM) {
                orderData.customPaymentMethod!!
            } else {
                orderData.paymentMethod.methodName
            },
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "${orderData.premium}%",
            modifier = Modifier.width(55.dp),
            textAlign = TextAlign.Right
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MarketScreenPreview() {
    RobosatsAndroidTheme {
        val orders = listOf(
            OrderData(
                id = 91593,
                type = OrderType.BUY,
                currency = Currency.USD,
                minAmount = 50.0,
                maxAmount = 175.0,
                paymentMethod = PaymentMethod.USDT,
                price = 105.0,
                premium = 5.0
            ),
            OrderData(
                id = 2,
                type = OrderType.SELL,
                currency = Currency.EUR,
                amount = 200.0,
                paymentMethod = PaymentMethod.CUSTOM,
                customPaymentMethod = "PaymentMethod2",
                price = 210.0,
                premium = -2.0
            ),
            OrderData(
                id = 91595,
                type = OrderType.BUY,
                currency = Currency.BTC,
                minAmount = 150.0,
                maxAmount = 2000.0,
                paymentMethod = PaymentMethod.CUSTOM,
                customPaymentMethod = "PaymentMethod3",
                price = 315.0,
                premium = -4.91
            )
        )
        val mockSharedViewModel = MockSharedViewModel(orders, false)
        MarketScreen(mockSharedViewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun MarketScreenEmptyPreview() {
    RobosatsAndroidTheme {
        val mockSharedViewModel = MockSharedViewModel(emptyList(), true)
        MarketScreen(mockSharedViewModel)
    }
}