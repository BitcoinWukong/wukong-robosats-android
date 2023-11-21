package com.bitcoinwukong.robosats_android.ui.robot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
        // Display the first 8 characters and the last 4 characters of the token
        val displayToken =
            if (token.length > 12) token.take(8) + "..." + token.takeLast(4) else token

        Text(
            text = "${robot?.nickname ?: "Loading robot info..."} ($displayToken)",
            modifier = Modifier.weight(1f)
        )

        if (token == selectedToken) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = Color.Green,
                modifier = Modifier.size(24.dp) // Specifying the icon size
            )
        }
    }
    Divider()
}

@Preview(showBackground = true)
@Composable
fun RobotListItemPreviewOnlyToken() {
    val token = "XWdQIua1zwlK60rw00IVd64fvwbk0DyJC8ye"
    Row(modifier = Modifier.width(350.dp)) {
        RobotListItem(token, null, token) {}
    }
}

@Preview(showBackground = true)
@Composable
fun RobotListItemPreview() {
    val token = "XWdQIua1zwlK60rw00IVd64fvwbk0DyJC8ye"
    val robot1 = Robot(
        token,
        "pub_key",
        "enc_priv_key",
        nickname = "ContinuousSnowwhite345",
    )

    Row(modifier = Modifier.width(350.dp)) {
        RobotListItem(token, robot1, token) {}
    }
}
