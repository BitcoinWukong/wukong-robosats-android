package com.bitcoinwukong.robosats_android.utils

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun parseDateTime(dateTimeString: String?): LocalDateTime? {
    val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

    return try {
        if (dateTimeString.isNullOrBlank()) null
        else {
            val utcDateTime = ZonedDateTime.parse(dateTimeString, dateTimeFormatter)
            utcDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        }
    } catch (e: DateTimeParseException) {
        null
    }
}

fun convertExpirationTimeToExpirationDateTime(expirationTime: LocalTime?): LocalDateTime {
    val currentDateTime = LocalDateTime.now()

    // Check if expirationTime has been set by the user
    return if (expirationTime != null) {
        val currentTime = currentDateTime.toLocalTime()
        val currentDate = currentDateTime.toLocalDate()

        val expirationDate = if (expirationTime.isAfter(currentTime)) {
            currentDate // Use today's date if the selected time is ahead of the current time
        } else {
            currentDate.plusDays(1) // Use tomorrow's date otherwise
        }

        LocalDateTime.of(expirationDate, expirationTime)
    } else {
        // Handle the case where expirationTime is null
        currentDateTime.plusDays(1) // Default to 24 hours from now if no time is set
    }
}


fun convertExpirationTimeToExpirationSeconds(expirationTime: LocalTime?): Int {
    val currentDateTime = LocalDateTime.now()

    // Check if expirationTime has been set by the user
    val expirationDateTime = convertExpirationTimeToExpirationDateTime(expirationTime)

    // Calculate publicDuration as the difference in seconds between the current time and the expiration time
    return Duration.between(currentDateTime, expirationDateTime).seconds.toInt()
}
