package com.bitcoinwukong.robosats_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bitcoinwukong.robosats_android.ui.components.ScrollableTextBox
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel
@Composable
fun LogScreen(sharedViewModel: ISharedViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(if (isSystemInDarkTheme()) Color.DarkGray else Color.White)
    ) {
        val liveDataValue: String by sharedViewModel.torManagerEvents.observeAsState("")
        ScrollableTextBox(
            text = liveDataValue,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp),
            textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { sharedViewModel.restartTor() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(text = "Restart")
            }
            Button(
                onClick = { sharedViewModel.fetchOrders() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors() // Customize for your app theme
            ) {
                Text(text = "Connect")
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun TorScreenPreview() {
//    RobosatsAndroidTheme {
//        val mockSharedViewModel = MockSharedViewModel(MockTorManager())
//        LogScreen(mockSharedViewModel)
//    }
//}
