package com.bitcoinwukong.robosats_android.ui.robot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun RobotDetails(
    viewModel: ISharedViewModel,
    selectedToken: String,
    selectedRobot: Robot?,
    onClickCreateOrder: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center, // Center content
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            selectedToken.isEmpty() -> {
                Text("No active robot")
            }
            selectedRobot == null -> {
                CircularProgressIndicator()
            }
            selectedRobot.errorMessage != null -> {
                Text(text = selectedRobot.errorMessage)
            }
            selectedRobot.activeOrderId == null -> {
                Button(onClick = onClickCreateOrder, enabled = true) {
                    Text("Create Order")
                }
            }
            else -> {
                OrderDetailsContent(viewModel, selectedRobot)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RobotDetailsPreviewLoading() {
    Box(modifier = Modifier.size(300.dp)) {
        RobotDetails(MockSharedViewModel(), "",null) {}
    }
}

@Preview(showBackground = true)
@Composable
fun RobotDetailsPreviewErrorMessage() {
    Box(modifier = Modifier.size(300.dp)) {
        val robot = Robot(
            "token1",
            errorMessage = "Unable to load robot"
        )
        RobotDetails(MockSharedViewModel(), "token1", robot) {}
    }
}

@Preview(showBackground = true)
@Composable
fun RobotDetailsPreviewNoActiveOrder() {
    Box(modifier = Modifier.size(300.dp)) {
        val robot1 = Robot(
            "token1",
            nickname = "robot1",
        )
        RobotDetails(MockSharedViewModel(), "token1", robot1) {}
    }
}

@Preview(showBackground = true)
@Composable
fun RobotDetailsPreviewActiveOrder() {
    Box(modifier = Modifier.size(300.dp)) {
        val order = OrderData(
            id = 91593,
            type = OrderType.BUY,
            amount = 21.0,
            currency = Currency.USD,
            status = OrderStatus.PUBLIC
        )
        val robot1 = Robot(
            "token1",
            activeOrderId = 91593,
        )
        val mockSharedViewModel = MockSharedViewModel(listOf(order), false, activeOrder = order)
        RobotDetails(mockSharedViewModel, "token1", robot1) {}
    }
}