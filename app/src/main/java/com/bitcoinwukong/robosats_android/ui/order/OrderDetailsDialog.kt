package com.bitcoinwukong.robosats_android.ui.order

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import com.bitcoinwukong.robosats_android.mocks.MockSharedViewModel
import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderStatus
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel

@Composable
fun OrderDetailsDialog(
    viewModel: ISharedViewModel,
    robot: Robot,
    onDismiss: () -> Unit
) {
    val orderId = robot.activeOrderId ?: return
    viewModel.getOrderDetails(robot, orderId)
    val activeOrder by viewModel.activeOrder.observeAsState(null)

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(text = "Order Action") },
        text = {
            if (activeOrder == null) {
                Text("Loading...")
            } else {
                val order = activeOrder!!
                Column {
                    if (order.status == OrderStatus.PUBLIC) {
                        Text("Order is now public")

                        Button(onClick = {
                            viewModel.pauseResumeOrder(robot, orderId)
                        }) {
                            Text("Pause Order")
                        }
                    } else if (order.status == OrderStatus.PAUSED) {
                        Text("Order is now paused")

                        Button(onClick = {
                            viewModel.pauseResumeOrder(robot, orderId)
                        }) {
                            Text("Resume Order")
                        }
                    } else {
                        Text("Unknown order status")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onDismiss()
            }) {
                Text("Cancel")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun PauseOrderDialogPreview() {
    val order = OrderData(
        id = 91593,
        type = OrderType.BUY,
        currency = Currency.USD,
        minAmount = 50.0,
        maxAmount = 175.0,
        paymentMethod = PaymentMethod.AMAZON_USA_GIFTCARD,
        price = 105.0,
        premium = 5.0,
        status = OrderStatus.PUBLIC
    )
    val robot1 = Robot(
        "token1",
        "pub_key",
        "enc_priv_key",
        nickname = "robot1",
        activeOrderId = 91593,
    )
    val mockSharedViewModel = MockSharedViewModel(listOf(order), false, activeOrder = order)
    RobosatsAndroidTheme {
        OrderDetailsDialog(
            mockSharedViewModel,
            robot1,
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ResumeOrderDialogPreview() {
    val order = OrderData(
        id = 91593,
        type = OrderType.BUY,
        currency = Currency.USD,
        minAmount = 50.0,
        maxAmount = 175.0,
        paymentMethod = PaymentMethod.AMAZON_USA_GIFTCARD,
        price = 105.0,
        premium = 5.0,
        status = OrderStatus.PAUSED
    )
    val robot1 = Robot(
        "token1",
        "pub_key",
        "enc_priv_key",
        nickname = "robot1",
        activeOrderId = 91593,
    )
    val mockSharedViewModel = MockSharedViewModel(listOf(order), false, activeOrder = order)
    RobosatsAndroidTheme {
        OrderDetailsDialog(
            mockSharedViewModel,
            robot1,
            onDismiss = { },
        )
    }
}
