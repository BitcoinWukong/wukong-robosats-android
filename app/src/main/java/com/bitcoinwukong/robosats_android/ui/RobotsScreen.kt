package com.bitcoinwukong.robosats_android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitcoinwukong.robosats_android.mocks.MockSharedViewModel
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.ui.order.CreateOrderDialog
import com.bitcoinwukong.robosats_android.ui.order.OrderDetailsDialog
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel

//@Composable
//fun RobotsScreen(viewModel: ISharedViewModel = viewModel()) {
//    CreateOrderContent({ orderType, currency, amount, paymentMethod ->
//        viewModel.createOrder(orderType, currency, amount, paymentMethod)
//    })
//}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RobotsScreen(viewModel: ISharedViewModel = viewModel()) {
    var textFieldValue by remember { mutableStateOf("") }

    val robotTokens by viewModel.robotTokens.observeAsState(setOf())
    val robotsInfoMap by viewModel.robotsInfoMap.observeAsState(mapOf())

    val selectedToken by viewModel.selectedToken.observeAsState("")
    val selectedRobot by viewModel.selectedRobot.observeAsState(null)

    var showCreateOrderDialog by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager =
        LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Triggered when "Create Order" button is clicked
    if (showCreateOrderDialog) {
        CreateOrderDialog(
            onOrderCreated = { orderType, currency, amount, paymentMethod ->
                // Implement order creation logic
                viewModel.createOrder(orderType, currency, amount, paymentMethod)
                showCreateOrderDialog = false
            },
            onDismiss = { showCreateOrderDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // TextField to add new token
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Robot Token") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (textFieldValue.isNotBlank()) {
                        viewModel.addRobot(textFieldValue)
                        textFieldValue = ""
                        keyboardController?.hide()
                    }
                }
            )
        )

        // Add Button
        Button(
            onClick = {
                if (textFieldValue.isNotBlank()) {
                    viewModel.addRobot(textFieldValue)
                    textFieldValue = "" // Clear the text field
                    keyboardController?.hide() // Dismiss the keyboard
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Recover")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn for the list of tokens
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // make the LazyColumn take up all space above the buttons
                .padding(vertical = 4.dp)
        ) {
            // Inside the LazyColumn in RobotsScreen
            items(robotTokens.toList()) { token ->
                RobotListItem(token, robotsInfoMap[token], selectedToken) { t ->
                    viewModel.selectRobot(t)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    // Copy the selected token to the clipboard
                    val clip = ClipData.newPlainText("simple text", selectedToken)
                    clipboardManager.setPrimaryClip(clip)
                },
                enabled = selectedToken.isNotEmpty() // Enable button only when a token is selected
            ) {
                Text("Copy Token")
            }

            Button(
                onClick = {
                    viewModel.removeRobot(selectedToken)
                },
                enabled = selectedToken.isNotEmpty() // Enable button only when a token is selected
            ) {
                Text("Delete")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(2f)
        ) {
            selectedRobot?.let { robot ->
                RobotDetails(viewModel, robot,
                    onClickCreateOrder = { showCreateOrderDialog = true })
            }
        }
    }
}

@Composable
fun RobotDetails(
    viewModel: ISharedViewModel,
    robot: Robot,
    onClickCreateOrder: () -> Unit
) {
    var showPopup by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center, // Centers the content horizontally
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active Order ID Button
            robot.activeOrderId?.let { activeOrderId ->
                Button(onClick = { showPopup = true }) {
                    Text("Active Order ID: ${activeOrderId}")
                }
            } ?: run {
                // TODO: Enable order creation after fixing everything
                Button(onClick = onClickCreateOrder, enabled = false) {
                    Text("Create Order")
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        // Popup
        if (showPopup) {
            OrderDetailsDialog(
                onDismiss = { showPopup = false },
                viewModel = viewModel,
                robot = robot
            )
        }
    }
}

@Composable
fun RobotListItem(
    token: String,
    robot: Robot?,
    selectedToken: String,
    onTokenSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTokenSelected(token) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = robot?.nickname ?: "Loading...",
        )

        Spacer(modifier = Modifier.weight(1f))

        if (token == selectedToken) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = Color.Green
            )
        }
    }
    Divider()
}

@Preview(showBackground = true)
@Composable
fun RobotsScreenPreview() {
    RobosatsAndroidTheme {
        val robotTokens = setOf("token1", "token2", "token3")
        val robot1 = Robot(
            "token1",
            "pub_key",
            "enc_priv_key",
            nickname = "robot1",
        )
        val robotsInfoMap = mapOf(
            "token1" to robot1
        )
        val viewModel = MockSharedViewModel(
            robotTokens = robotTokens,
            robotsInfoMap = robotsInfoMap,
            selectedRobot = robot1
        )
        RobotsScreen(viewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun RobotsScreenPreviewEmpty() {
    RobosatsAndroidTheme {
        RobotsScreen(MockSharedViewModel())
    }
}
