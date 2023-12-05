package com.bitcoinwukong.robosats_android

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.bitcoinwukong.robosats_android.model.PgpKeyManager
import com.bitcoinwukong.robosats_android.repository.TorRepository
import com.bitcoinwukong.robosats_android.ui.MainScreen
import com.bitcoinwukong.robosats_android.ui.theme.RobosatsAndroidTheme
import com.bitcoinwukong.robosats_android.viewmodel.SharedViewModel
import com.bitcoinwukong.robosats_android.viewmodel.SharedViewModelFactory

class MainActivity : ComponentActivity() {
    private val app: RobosatsApp get() = application as RobosatsApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PgpKeyManager.initialize(app)

        // Create the view model
        val torRepository = TorRepository(app.torManager)
        val sharedPreferences = app.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val sharedViewModelFactory = SharedViewModelFactory(torRepository, sharedPreferences)
        val sharedViewModel =
            ViewModelProvider(this, sharedViewModelFactory)[SharedViewModel::class.java]

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

        // Start the TorForegroundService
        val serviceIntent = Intent(this, TorForegroundService::class.java)
        startForegroundService(serviceIntent)

        // Check Notification Settings
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.areNotificationsEnabled()) {
            showNotificationPermissionDialog()
        }
    }

    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Notifications")
            .setMessage("Notifications are disabled. Please enable them to keep the app running in the background to check order status.")
            .setPositiveButton("Settings") { dialog, _ ->
                dialog.dismiss()
                // Direct the user to the app's notification settings
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
