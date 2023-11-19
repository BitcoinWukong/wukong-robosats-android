package com.bitcoinwukong.robosats_android

import com.bitcoinwukong.robosats_android.utils.generateSecureToken
import com.bitcoinwukong.robosats_android.utils.hashTokenAsBase91
import com.bitcoinwukong.robosats_android.utils.hexStringToByteArray
import com.bitcoinwukong.robosats_android.utils.hexToBase91
import com.bitcoinwukong.robosats_android.utils.tokenSha256Hash
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

class HashAndEncodeTest {
    @Test
    fun testGenereteToken() {
        // Example test for hashTokenAsBase91
        val tokenLength = 36
        val expectedToken = generateSecureToken()
        assertEquals(tokenLength, expectedToken.length)
    }

    @Test
    fun testHashTokenSha256() {
        val token = "oCl67qpTBVFtcnsjSUaX7E9aDVFY7r7Wjwep"
        val expectedHash = "47f8d2ef23e77c33281d9eea9e2966a8e31da9764e02240f7df34e7a99de1970"

        assertEquals(expectedHash, tokenSha256Hash(token))
    }

    @Test
    fun testHexStringToByteArray() {
        val token = "oCl67qpTBVFtcnsjSUaX7E9aDVFY7r7Wjwep"
        val digest = MessageDigest.getInstance("SHA-256")
        val expectedByteArray = digest.digest(token.toByteArray(Charsets.UTF_8))

        val hexString = "47f8d2ef23e77c33281d9eea9e2966a8e31da9764e02240f7df34e7a99de1970"
        assertArrayEquals(expectedByteArray, hexStringToByteArray(hexString))
    }
    @Test
    fun testHexToBase91() {
        val hexString = "47f8d2ef23e77c33281d9eea9e2966a8e31da9764e02240f7df34e7a99de1970"
        val expectedOutput = "b)F{YZ2.EJBppd@3eRd]BVrl.N7y_44`A:t6RSqC"

        assertEquals(expectedOutput, hexToBase91(hexString))
    }

    @Test
    fun testHashTokenAsBase91() {
        val token = "oCl67qpTBVFtcnsjSUaX7E9aDVFY7r7Wjwep"
        val expectedOutput = "b)F{YZ2.EJBppd@3eRd]BVrl.N7y_44`A:t6RSqC"

        assertEquals(expectedOutput, hashTokenAsBase91(token))
    }
}