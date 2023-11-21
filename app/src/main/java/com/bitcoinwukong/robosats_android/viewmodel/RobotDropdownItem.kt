package com.bitcoinwukong.robosats_android.viewmodel

import com.bitcoinwukong.robosats_android.model.Robot

class RobotDropdownItem(
    val token: String,
    val robot: Robot?
) {
    // Display the first 8 characters and the last 4 characters of the token
    private val displayToken: String
        get() = if (token.length > 12) token.take(8) + "..." + token.takeLast(4) else token

    override fun toString(): String {
        return "${robot?.nickname ?: "Loading robot info..."} ($displayToken)"
    }
}
