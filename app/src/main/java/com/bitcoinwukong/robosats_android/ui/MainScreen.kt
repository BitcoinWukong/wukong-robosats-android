package com.bitcoinwukong.robosats_android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.bitcoinwukong.robosats_android.mocks.MockSharedViewModel
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel

enum class BottomTab(val title: String, val icon: ImageVector) {
    Robots("Robots", Icons.Outlined.SmartToy),
    Market("Market", Icons.Outlined.ShoppingCart),
    Log("Log", Icons.Outlined.Description)
}

@Composable
fun MainScreen(sharedViewModel: ISharedViewModel) {
    val bottomTabs = listOf(BottomTab.Robots, BottomTab.Market, BottomTab.Log)
    var selectedTab by remember { mutableStateOf(BottomTab.Robots) }

    Scaffold(
        bottomBar = {
            BottomNavigation {
                bottomTabs.forEach { tab ->
                    BottomNavigationItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.title) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null
                            )
                        } // Use appropriate icons
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                BottomTab.Robots -> RobotsScreen(sharedViewModel)
                BottomTab.Market -> MarketScreen(sharedViewModel)
                BottomTab.Log -> LogScreen(sharedViewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    RobosatsAndroidTheme {
        MainScreen(MockSharedViewModel())
    }
}
