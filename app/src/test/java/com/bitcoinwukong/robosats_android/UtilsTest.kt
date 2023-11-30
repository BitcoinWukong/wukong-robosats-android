package com.bitcoinwukong.robosats_android

import com.bitcoinwukong.robosats_android.utils.convertExpirationTimeToExpirationSeconds
import com.bitcoinwukong.robosats_android.utils.parseDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class UtilsTest {
    @Test
    fun testParseDateTime() {
        // Example test for hashTokenAsBase91
        val dateTimeString = "2023-11-17T21:16:08.518446Z"
        val localTime = parseDateTime(dateTimeString)!!
        assertEquals(2023, localTime.year)
        assertEquals(11, localTime.month.value)
    }

    @Test
    fun `expiration time is later today`() {        // Arrange
        val now = LocalTime.now()
        val twoHoursLater = now.plusHours(2)

        // Act
        val expirationSeconds = convertExpirationTimeToExpirationSeconds(twoHoursLater)

        // Assert
        assertTrue("The expiration time should be positive", expirationSeconds > 0)
        assertTrue(
            "The expiration time should be close to 2 hours in seconds",
            expirationSeconds <= 2 * 60 * 60
        )
        assertTrue(
            "The expiration time should be close to 2 hours in seconds",
            expirationSeconds >= 2 * 60 * 60 - 10
        )
    }

    @Test
    fun `expiration time is earlier today and should be set for tomorrow`() {
        // Arrange
        val now = LocalTime.now()
        val twoHoursEarlier = now.minusHours(2)

        // Act
        val expirationSeconds = convertExpirationTimeToExpirationSeconds(twoHoursEarlier)

        // Assert
        val twentyTwoHoursInSeconds = 22 * 60 * 60
        assertTrue(
            "The expiration time should be set for tomorrow",
            expirationSeconds >= twentyTwoHoursInSeconds - 10
        )
        assertTrue(
            "The expiration time should be set for tomorrow",
            expirationSeconds <= twentyTwoHoursInSeconds
        )
    }

    @Test
    fun `expiration time is null and defaults to 24 hours`() {
        // Act
        val expirationSeconds = convertExpirationTimeToExpirationSeconds(null)

        // Assert
        assertEquals(
            "The expiration time should default to 24 hours in seconds",
            24 * 60 * 60,
            expirationSeconds
        )
    }
}