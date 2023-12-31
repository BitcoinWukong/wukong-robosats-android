package com.bitcoinwukong.robosats_android.ui.order.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitcoinwukong.robosats_android.mocks.MockSharedViewModel
import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.MessageData
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderStatus
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.model.generateRobot
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.generateKeyPair
import com.bitcoinwukong.robosats_android.utils.parseDateTime
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessages(viewModel: ISharedViewModel, robot: Robot, order: OrderData) {
    val chatMessages by viewModel.chatMessages.observeAsState(emptyList())
    var currentMessage by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Display chat messages
        LazyColumn(
            state = scrollState,
            modifier = Modifier.weight(1f)
        ) {
            items(chatMessages) { message ->
                // Check if the message is from the sender (robot)
                val isFromSender = message.nick == robot.nickname
                ChatMessageBubble(message, isFromSender)
            }
        }

        // Auto-scroll to the latest message when the list changes
        LaunchedEffect(chatMessages) {
            if (chatMessages.isNotEmpty()) {
                scrollState.animateScrollToItem(chatMessages.size - 1)
            }
        }

        // Input field and Send button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentMessage,
                onValueChange = { currentMessage = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                placeholder = { Text("Type a message") },
                shape = MaterialTheme.shapes.small, // Rounded shape
                colors = OutlinedTextFieldDefaults.colors(
                ) // Custom colors if needed
            )
            FloatingActionButton(
                onClick = {
                    viewModel.sendChatMessage(robot, order.id!!, currentMessage)
                    currentMessage = ""
                },
                modifier = Modifier
                    .size(56.dp) // Typical size for a FAB
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(24.dp) // Typical size for icons within a FAB
                )
            }
        }

        // Conditional button based on order status and role
        if (order.status == OrderStatus.FIAT_SENT_IN_CHATROOM && order.isSeller) {
            Button(
                onClick = { showConfirmationDialog = true },
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Text("Confirm fiat received")
            }
        }
    }

    // Loading state
    if (chatMessages.isEmpty()) {
        Text("Loading messages...")
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Confirmation") },
            text = { Text("Are you sure you have received your fiat payment? This action is irreversible!") },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmOrderFiatReceived(order)
                    showConfirmationDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showConfirmationDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun ChatMessageBubble(messageData: MessageData, isFromSender: Boolean = false) {
    // Define message bubble alignment
    val bubbleAlignment = if (isFromSender) Alignment.TopEnd else Alignment.TopStart
    val bubbleColor =
        if (isFromSender) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val textColor =
        if (isFromSender) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val formattedTime = parseDateTime(messageData.time)?.format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    ) ?: "Unknown time"

    Box(
        contentAlignment = bubbleAlignment,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(all = 8.dp)) {
                Text(
                    text = messageData.nick, // Sender's nickname
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedTime, // Time of the message
                    color = textColor,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = messageData.message, // Message text
                    color = textColor
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ChatMessagesPreview_Seller() {
    val mockOrder = OrderData(
        id = 123, // Mock order ID
        type = OrderType.BUY, // Example order type
        currency = Currency.USD, // Example currency
        status = OrderStatus.FIAT_SENT_IN_CHATROOM, // Example order status
        isSeller = true,
    )

    val robot1 = generateRobot().copy(nickname = "robot1")
    val mockViewModel = MockSharedViewModel(chatMessages = listOf(
        MessageData(0, "2023-12-03T02:57:02.788041Z", "", "robot1", "hey"),
        MessageData(0, "2023-12-03T02:59:02.796032Z", "", "robot2", "hi, what's your email")))

    Box(modifier = Modifier.size(350.dp)) {
        ChatMessages(viewModel = mockViewModel, robot1, order = mockOrder)
    }
}
