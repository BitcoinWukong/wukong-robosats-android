package com.bitcoinwukong.robosats_android.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun parseDateTime(dateTimeString: String?): LocalDateTime? {
    val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

    return try {
        if (dateTimeString.isNullOrBlank()) null else LocalDateTime.parse(dateTimeString, dateTimeFormatter)
    } catch (e: DateTimeParseException) {
        null
    }
}