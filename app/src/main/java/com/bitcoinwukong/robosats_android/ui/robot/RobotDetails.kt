package com.bitcoinwukong.robosats_android.ui.robot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitcoinwukong.robosats_android.mocks.MockSharedViewModel
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.ui.order.OrderDetailsDialog
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel

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
                    Text("Active Order ID: $activeOrderId")
                }
            } ?: run {
                Button(onClick = onClickCreateOrder, enabled = true) {
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

@Preview(showBackground = true)
@Composable
fun RobotDetailsPreviewNoActiveOrder() {
    val robot1 = Robot(
        "token1",
        "pub_key",
        "enc_priv_key",
        nickname = "robot1",
    )
    RobotDetails(MockSharedViewModel(), robot1) {}
}

@Preview(showBackground = true)
@Composable
fun RobotDetailsPreviewActiveOrder() {
    val robot1 = Robot(
        "token1",
        "pub_key",
        "enc_priv_key",
        nickname = "robot1",
        activeOrderId = 92998
    )
    RobotDetails(MockSharedViewModel(), robot1) {}
}