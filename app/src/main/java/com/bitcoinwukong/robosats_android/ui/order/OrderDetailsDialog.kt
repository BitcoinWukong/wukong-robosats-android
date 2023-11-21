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
        title = { Text(text = "Order Details") },
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
                    } else if (order.status == OrderStatus.WAITING_FOR_MAKER_BOND) {
                        order.bondInvoice?.let { Text(it) }
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
        status = OrderStatus.PUBLIC
    )
    val robot1 = Robot(
        "token1",
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
        status = OrderStatus.PAUSED
    )
    val robot1 = Robot(
        "token1",
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
fun WaitingForMakerBondOrderDialogPreview() {
    val order = OrderData(
        id = 91593,
        type = OrderType.BUY,
        currency = Currency.USD,
        status = OrderStatus.WAITING_FOR_MAKER_BOND,
        bondInvoice = "lnbc72700n1pj4h7p4pp5856nrs0caqe5l2rlprsu4dtyvqszhf0xqyshyt539cwy93vwu8jqd2j2pshjmt9de6zqun9vejhyetwvdjn5gr9xe3rjvp4vgcj6enrvg6j6dr9xdnz6c3n8psj6wpsxsunwcesxpnrqcnx9cs9g6rfwvs8qcted4jkuapq2ay5cnpqgefy2326g5syjn3qt984253q2aq5cnz92skzqcmgv43kkgr0dcs9ymmzdafkzarnyp5kvgr5dpjjqmr0vd4jqampwvs8xatrvdjhxumxw4kzugzfwss8w6tvdssxyefqw4hxcmmrddjkggpgveskjmpfyp6kumr9wdejq7t0w5sxx6r9v96zqmmjyp3kzmnrv4kzqatwd9kxzar9wfskcmre9ccqzjxxqzuysp5pdhd9nrhef8teamunl99lxprfw5krzeyydwh2mrwar6nrvp4295s9qyyssq3eeha7zeu9c53n3hvj06ldupwqaafw8dspgfc5tq6fj42m0trvp84lfwmkpz9ajfjsf9x3w7qh0smy7zrmd5hseu3kd6edkmx7g7gtcp4gny35"
    )
    val robot1 = Robot(
        "token1",
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
