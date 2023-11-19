package com.bitcoinwukong.robosats_android.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64


fun generateSecureToken(): String {
    val tokenLength = 36
    val randomBytes = ByteArray(tokenLength * 2)
    SecureRandom().nextBytes(randomBytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
        .take(tokenLength)
        .replace('-', 'a') // Replace with a valid Base62 character
        .replace('_', 'b') // Replace with a valid Base62 character
}


fun tokenSha256Hash(token: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
    return bytesToHex(hashBytes)
}

fun bytesToHex(bytes: ByteArray): String {
    return bytes.joinToString("") { "%02x".format(it) }
}

//An authenticated request (has the token's sha256 hash encoded as base 91 in the Authorization header)
fun hashTokenAsBase91(token: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
    return encodeBase91(hashBytes) // Implement encodeBase91 for base 91 encoding
}

fun hexStringToByteArray(hexString: String): ByteArray {
    val len = hexString.length
    val data = ByteArray(len / 2)
    for (i in 0 until len step 2) {
        data[i / 2] = ((Character.digit(hexString[i], 16) shl 4) + Character.digit(hexString[i + 1], 16)).toByte()
    }
    return data
}

fun hexToBase91(hex: String): String {
    val byteArray = hexStringToByteArray(hex)
    return encodeBase91(byteArray)
}

fun encodeBase91(input: ByteArray): String {
    val base91Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&()*+,./:;<=>?@[]^_`{|}~\""
    var buffer = 0
    var bufferLength = 0
    var value: Int
    val output = StringBuilder()

    input.forEach { byte ->
        buffer = buffer or (byte.toInt() and 255 shl bufferLength)
        bufferLength += 8
        if (bufferLength > 13) {
            value = buffer and 8191
            if (value > 88) {
                buffer = buffer shr 13
                bufferLength -= 13
            } else {
                value = buffer and 16383
                buffer = buffer shr 14
                bufferLength -= 14
            }
            output.append(base91Alphabet[value % 91])
            output.append(base91Alphabet[value / 91])
        }
    }

    if (bufferLength != 0) {
        output.append(base91Alphabet[buffer % 91])
        if (bufferLength > 7 || buffer > 90) {
            output.append(base91Alphabet[buffer / 91])
        }
    }

    return output.toString()
}
