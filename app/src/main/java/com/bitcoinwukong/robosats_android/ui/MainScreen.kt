package com.bitcoinwukong.robosats_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitcoinwukong.robosats_android.mocks.MockSharedViewModel
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.ui.components.ScrollableTextBox
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel

enum class BottomTab(val title: String, val icon: ImageVector) {
    Robots("Robots", Icons.Outlined.SmartToy),
    Market("Market", Icons.Outlined.ShoppingCart),
    Log("Log", Icons.Outlined.Description)
}

@Composable
fun TorLoadingScreen(sharedViewModel: ISharedViewModel) {
    val liveDataValue: String by sharedViewModel.torManagerEvents.observeAsState("")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center, // Centers vertically
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Connecting to Tor...")
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        ScrollableTextBox(
            text = liveDataValue,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
        )
    }
}


@Composable
fun RobotsLoadingScreen(loadingRobots: Set<Robot>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colors.background),
    ) {
        Text(
            "Loading robots...",
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(loadingRobots.toList()) { robot ->
                Text(
                    text = robot.nickname ?: robot.token,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator()
    }
}

@Composable
fun MainScreen(sharedViewModel: ISharedViewModel) {
    val isTorReady by sharedViewModel.isTorReady.observeAsState(false)
    val loadingRobots by sharedViewModel.loadingRobots.observeAsState(emptySet())

    if (!isTorReady) {
        TorLoadingScreen(sharedViewModel = sharedViewModel)
        return
    } else if (loadingRobots.isNotEmpty()) {
        RobotsLoadingScreen(loadingRobots)
        return
    }

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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview_TorLoading() {
    RobosatsAndroidTheme {
        MainScreen(MockSharedViewModel(isTorReady = false))
    }
}
