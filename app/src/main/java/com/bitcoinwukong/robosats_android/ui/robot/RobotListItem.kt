package com.bitcoinwukong.robosats_android.ui.robot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitcoinwukong.robosats_android.model.Robot

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
fun RobotListItemPreviewOnlyToken() {
    RobotListItem("token1", null, "token1") { t ->
    }
}

@Preview(showBackground = true)
@Composable
fun RobotListItemPreview() {
    val robot1 = Robot(
        "token1",
        "pub_key",
        "enc_priv_key",
        nickname = "robot1",
    )
    RobotListItem("token1", robot1, "token1") { t ->
    }
}
