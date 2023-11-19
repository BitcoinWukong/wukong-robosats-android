package com.bitcoinwukong.robosats_android

import com.bitcoinwukong.robosats_android.utils.parseDateTime
import org.junit.Assert
import org.junit.Test

class UtilsTest {
    @Test
    fun testParseDateTime() {
        // Example test for hashTokenAsBase91
        val dateTimeString = "2023-11-17T21:16:08.518446Z"
        val localTime = parseDateTime(dateTimeString)!!
        Assert.assertEquals(2023, localTime.year)
        Assert.assertEquals(11, localTime.month.value)
    }
}