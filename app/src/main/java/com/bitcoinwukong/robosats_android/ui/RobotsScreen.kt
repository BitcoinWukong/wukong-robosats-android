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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import com.bitcoinwukong.robosats_android.ui.components.WKDropdownMenu
import com.bitcoinwukong.robosats_android.ui.order.CreateOrderDialog
import com.bitcoinwukong.robosats_android.ui.robot.RobotDetails
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel
import com.bitcoinwukong.robosats_android.viewmodel.RobotDropdownItem

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RobotsScreen(viewModel: ISharedViewModel = viewModel()) {
    var textFieldValue by remember { mutableStateOf("") }

    val robotTokens by viewModel.robotTokens.observeAsState(setOf())
    val robotsInfoMap by viewModel.robotsInfoMap.observeAsState(mapOf())

    val selectedToken by viewModel.selectedToken.observeAsState("")
    val selectedRobot by viewModel.selectedRobot.observeAsState(null)

    var showCreateOrderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager =
        LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Triggered when "Create Order" button is clicked
    if (showCreateOrderDialog) {
        CreateOrderDialog(
            onCreateOrder = { createOrderParams ->
                // Implement order creation logic
                viewModel.createOrder(createOrderParams)
                showCreateOrderDialog = false
            },
            onDismiss = { showCreateOrderDialog = false }
        )
    }
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this robot?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeRobot(selectedToken)
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmDialog = false }) {
                    Text("No")
                }
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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

        Button(
            modifier = Modifier
                .padding(8.dp),
            onClick = {
                if (textFieldValue.isNotBlank()) {
                    viewModel.addRobot(textFieldValue)
                    textFieldValue = "" // Clear the text field
                    keyboardController?.hide() // Dismiss the keyboard
                }
            }
        ) {
            Text("Recover")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Convert robotTokens to a list of RobotDropdownItem
        val dropdownItems = robotTokens.map { token ->
            RobotDropdownItem(token, robotsInfoMap[token])
        }.sortedBy { robotDropdownItem ->
            robotDropdownItem.token
        }
        val selectedDropdownItem = dropdownItems.find { it.token == selectedToken }

        WKDropdownMenu(
            label = "Active Robot",
            items = dropdownItems,
            selectedItem = selectedDropdownItem,
            onItemSelected = { robotDropdownItem ->
                viewModel.selectRobot(robotDropdownItem.token)
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
                onClick = { showDeleteConfirmDialog = true },
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
                viewModel, selectedToken, selectedRobot
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
fun RobotsScreenPreview_Loading() {
    RobosatsAndroidTheme {
        val robotTokens = setOf("token1", "token2", "token3")
        val viewModel = MockSharedViewModel(
            robotTokens = robotTokens,
            robotsInfoMap = emptyMap(),
            selectedToken = "token1"
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
