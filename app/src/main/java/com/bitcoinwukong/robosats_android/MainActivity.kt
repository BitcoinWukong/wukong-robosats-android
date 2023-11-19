package com.bitcoinwukong.robosats_android

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.bitcoinwukong.robosats_android.repository.TorRepository
import com.bitcoinwukong.robosats_android.ui.MainScreen
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme
import com.bitcoinwukong.robosats_android.viewmodel.SharedViewModel
import com.bitcoinwukong.robosats_android.viewmodel.SharedViewModelFactory

class MainActivity : ComponentActivity() {
    private val app: RobosatsApp get() = application as RobosatsApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the view model
        val torRepository = TorRepository(app.torManager)
        val sharedPreferences = app.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val sharedViewModelFactory = SharedViewModelFactory(torRepository, sharedPreferences)
        val sharedViewModel = ViewModelProvider(this, sharedViewModelFactory)[SharedViewModel::class.java]

        setContent {
            RobosatsAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(sharedViewModel)
                }
            }
        }
        app.torManager.start()
    }
}
