package com.bitcoinwukong.robosats_android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitcoinwukong.robosats_android.mocks.MockSharedViewModel
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.ui.order.CreateOrderDialog
import com.bitcoinwukong.robosats_android.ui.robot.RobotDetails
import com.bitcoinwukong.robosats_android.ui.robot.RobotListItem
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
            onCreateOrder = { orderData ->
                // Implement order creation logic
                viewModel.createOrder(orderData)
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
                RobotListItem(token, robotsInfoMap[token], selectedToken) { robotToken ->
                    viewModel.selectRobot(robotToken)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    val clip = ClipData.newPlainText("robot token", selectedToken)
                    clipboardManager.setPrimaryClip(clip)
                },
                enabled = selectedToken.isNotEmpty()
            ) {
                Text("Copy Token")
            }

            Button(
                onClick = {
                    viewModel.removeRobot(selectedToken)
                },
                enabled = selectedToken.isNotEmpty()
            ) {
                Text("Delete")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(2f)
        ) {
            RobotDetails(
                viewModel, selectedRobot
            ) { showCreateOrderDialog = true }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RobotsScreenPreview() {
    RobosatsAndroidTheme {
        val robotTokens = setOf("token1", "token2", "token3")
        val robot1 = Robot(
            "token1",
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
