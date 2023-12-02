package com.bitcoinwukong.robosats_android.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun InvoiceDisplaySection(invoice: String) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Display Invoice
    val displayInvoice =
        if (invoice.length > 50) invoice.take(32) + "..." + invoice.takeLast(18) else invoice
    TextButton(onClick = {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("lightning:$invoice")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }) {
        Text(displayInvoice)
    }

    Button(onClick = {
        val clip = ClipData.newPlainText("invoice", invoice)
        clipboardManager.setPrimaryClip(clip)
    }) {
        Text("Copy")
    }
}
