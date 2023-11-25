package com.bitcoinwukong.robosats_android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.bitcoinwukong.robosats_android.model.OrderType


@Composable
fun <T> WKDropdownMenu(
    label: String, items: List<T>, selectedItem: T?, onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(selectedItem?.toString() ?: " ") }
    val isDropdownEnabled = items.isNotEmpty()

    Box {
        OutlinedTextField(
            value = selectedItem?.toString() ?: " ",
            onValueChange = { /* No action needed here */ },
            readOnly = true, // Makes the TextField not editable
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown Icon",
                    tint = if (isDropdownEnabled) MaterialTheme.colorScheme.onSurface
                    else Color.Gray
                )
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isDropdownEnabled,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = Color.Gray,
                backgroundColor = MaterialTheme.colorScheme.surface,
                focusedLabelColor = MaterialTheme.colorScheme.primary, // Adjust for better visibility in dark theme
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, // Adjust for better visibility in dark theme
                disabledLabelColor = Color.Gray,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = Color.Gray
            )
        )
        if (isDropdownEnabled) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { item ->
                    DropdownMenuItem(onClick = {
                        selectedText = item.toString()
                        onItemSelected(item)
                        expanded = false
                    }) {
                        Text(text = item.toString(), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = { expanded = true })
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WKDropdownMenuPreview() {
    var selectedOrderType by remember { mutableStateOf(OrderType.SELL) }

    WKDropdownMenu(
        label = "Order Type",
        items = OrderType.values().toList(),
        selectedItem = selectedOrderType,
        onItemSelected = { selectedOrderType = it }
    )
}

@Preview(showBackground = true)
@Composable
fun WKDropdownMenuPreview_EmptyList() {
    WKDropdownMenu(
        label = "Order Type",
        items = emptyList(),
        selectedItem = null,
        onItemSelected = { }
    )
}
