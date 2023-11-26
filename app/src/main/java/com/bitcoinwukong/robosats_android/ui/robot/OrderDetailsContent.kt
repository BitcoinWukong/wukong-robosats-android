package com.bitcoinwukong.robosats_android.ui.robot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitcoinwukong.robosats_android.mocks.MockSharedViewModel
import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderStatus
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel

@Composable
fun OrderDetailsContent(
    viewModel: ISharedViewModel,
    robot: Robot,
) {
    val orderId = robot.activeOrderId ?: return
    val activeOrder by viewModel.activeOrder.observeAsState(null)

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
    ) {
        if (activeOrder == null) {
            LoadingContent(orderId)
        } else {
            val order = activeOrder ?: return
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                OrderStatusContent(order, viewModel, robot, orderId)
            }
            RefreshButton { viewModel.getOrderDetails(robot, orderId, true) }
        }
    }
}

@Composable
private fun RefreshButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Refresh")
        }
    }
}

@Composable
private fun OrderStatusContent(
    order: OrderData, viewModel: ISharedViewModel, robot: Robot, orderId: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(text = "Order ID: ${order.id}", modifier = Modifier.padding(bottom = 8.dp))
    }

    when {
        order.isWaitingForSellerCollateral() && order.isSeller() -> DisplayWaitingForSellerCollateralDetails(
            order
        )

        order.isChatting() -> {
            viewModel.getChatMessages(robot, orderId)
            ChatMessages(viewModel)
        }

        else -> when (order.status) {
            OrderStatus.PUBLIC -> DisplayPublicOrderDetails(viewModel, robot, order, orderId)
            OrderStatus.PAUSED -> DisplayPausedOrderDetails(viewModel, robot, orderId)
            OrderStatus.WAITING_FOR_MAKER_BOND, OrderStatus.WAITING_FOR_TAKER_BOND -> DisplayWaitingForBondDetails(
                order
            )
            OrderStatus.WAITING_ONLY_FOR_BUYER_INVOICE -> DisplayWaitingForBuyerInvoiceDetails(order)
            else -> DisplayUnknownStatus(order)
        }
    }
}

@Composable
private fun ChatMessages(viewModel: ISharedViewModel) {
    val chatMessages by viewModel.chatMessages.observeAsState(emptyList())

    if (chatMessages != null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(chatMessages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(message)
                }
            }
        }
    } else {
        Text("Loading messages...")
    }
}

@Composable
private fun LoadingContent(orderId: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Loading order of ID $orderId...")
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun DisplayPublicOrderDetails(
    viewModel: ISharedViewModel, robot: Robot, order: OrderData, orderId: Int
) {
    OrderDetailsSection(order)
    Button(onClick = { viewModel.pauseResumeOrder(robot, orderId) }) {
        Text("Pause")
    }
}

@Composable
private fun DisplayWaitingForBuyerInvoiceDetails(order: OrderData) {
    Text("We're now waiting for buyer to provide their invoice for receiving the Bitcoin.")
}

@Composable
private fun DisplayPausedOrderDetails(
    viewModel: ISharedViewModel, robot: Robot, orderId: Int
) {
    Text("Order is now paused")
    Button(onClick = { viewModel.pauseResumeOrder(robot, orderId) }) {
        Text("Resume")
    }
}

@Composable
private fun DisplayWaitingForBondDetails(order: OrderData) {
    order.bondInvoice?.let { bondInvoice ->
        Text("Waiting for your escrow bond:")
        Spacer(Modifier.height(16.dp))
        InvoiceDisplaySection(bondInvoice)
    }
}

@Composable
private fun DisplayWaitingForSellerCollateralDetails(order: OrderData) {
    order.escrowInvoice?.let { escrowInvoice ->
        Text("Waiting for collateral of ${order.escrowSats} sats:")
        Spacer(Modifier.height(16.dp))
        InvoiceDisplaySection(escrowInvoice)
    }
}

@Composable
private fun InvoiceDisplaySection(invoice: String) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Display Invoice
    val displayInvoice =
        if (invoice.length > 50) invoice.take(32) + "..." + invoice.takeLast(18) else invoice
    TextButton(onClick = {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("lightning:$invoice")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }) {
        Text(displayInvoice)
    }

    Button(onClick = {
        val clip = ClipData.newPlainText("invoice", invoice)
        clipboardManager.setPrimaryClip(clip)
    }) {
        Text("Copy")
    }
}

@Composable
private fun DisplayUnknownStatus(order: OrderData) {
    Text("Unknown order status")
    Log.d("Order", "Unknown order status, order data: $order")
}

@Composable
fun OrderDetailsSection(order: OrderData) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Text(text = "Type: ${order.type}")
        Text(text = "Currency: ${order.currency}")
        Text(text = "Amount: ${order.formattedAmount()}")
        Text(text = "Payment Method: ${order.paymentMethod}")
        Text(text = "Premium: ${order.premium}%")
    }
}


@Preview(showBackground = true)
@Composable
fun LoadingOrderDetailsPreview() {
    val robot1 = Robot(
        "token1",
        activeOrderId = 91593,
    )
    val mockSharedViewModel = MockSharedViewModel(emptyList(), false, activeOrder = null)
    Row(modifier = Modifier.width(350.dp)) {
        OrderDetailsContent(
            mockSharedViewModel, robot1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PauseOrderDetailsPreview() {
    val order = OrderData(
        id = 91593,
        type = OrderType.BUY,
        amount = 21.0,
        currency = Currency.USD,
        premium = 3.0,
        status = OrderStatus.PUBLIC
    )
    val robot1 = Robot(
        "token1",
        activeOrderId = 91593,
    )
    val mockSharedViewModel = MockSharedViewModel(listOf(order), false, activeOrder = order)
    Row(modifier = Modifier.width(350.dp)) {
        OrderDetailsContent(
            mockSharedViewModel, robot1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ResumeOrderDetailsPreview() {
    val order = OrderData(
        id = 91593, type = OrderType.BUY, currency = Currency.USD, status = OrderStatus.PAUSED
    )
    val robot1 = Robot(
        "token1",
        activeOrderId = 91593,
    )
    val mockSharedViewModel = MockSharedViewModel(listOf(order), false, activeOrder = order)
    Row(modifier = Modifier.width(350.dp)) {
        OrderDetailsContent(
            mockSharedViewModel, robot1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WaitingForMakerBondOrderDetailsPreview() {
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
    Row(modifier = Modifier.width(350.dp)) {
        OrderDetailsContent(
            mockSharedViewModel, robot1
        )
    }
}
