package com.blacklanebot

import android.util.Log
import java.util.Calendar
import java.util.regex.Pattern

object OfferParser {
    private const val TAG = "BLBot-Parser"

    // Matches patterns like "14 Aug · 12:30" or "Thu 14 Aug · 12:30" or "12:30"
    private val TIME_PATTERN = Pattern.compile(
        "(?:(\\d{1,2})\\s+([A-Za-z]{3})\\s*[·•\\-]\\s*)?(\\d{1,2}):(\\d{2})"
    )

    private val MONTH_MAP = mapOf(
        "jan" to 0, "feb" to 1, "mar" to 2, "apr" to 3,
        "may" to 4, "jun" to 5, "jul" to 6, "aug" to 7,
        "sep" to 8, "oct" to 9, "nov" to 10, "dec" to 11
    )

    fun isCanadaRide(text: String): Boolean {
        val hasCanada = BotConfig.CANADA_KEYWORDS.any { kw ->
            text.contains(kw, ignoreCase = kw.trimStart().length == kw.length)
        }
        val hasUSA = BotConfig.USA_KEYWORDS.any { kw -> text.contains(kw, ignoreCase = true) }
        val result = hasCanada && !hasUSA
        Log.d(TAG, "isCanada=$result hasCanada=$hasCanada hasUSA=$hasUSA | text snippet: ${text.take(80)}")
        return result
    }

    fun isWithinTimeWindow(text: String, minHours: Float, maxHours: Float): Boolean {
        val matcher = TIME_PATTERN.matcher(text)
        if (!matcher.find()) {
            Log.d(TAG, "No time found in: ${text.take(80)}")
            return false
        }

        val day = matcher.group(1)?.toIntOrNull()
        val monthStr = matcher.group(2)?.lowercase()
        val hour = matcher.group(3)?.toIntOrNull() ?: return false
        val minute = matcher.group(4)?.toIntOrNull() ?: return false

        val now = Calendar.getInstance()
        val ride = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (day != null && monthStr != null) {
                MONTH_MAP[monthStr]?.let { set(Calendar.MONTH, it) }
                set(Calendar.DAY_OF_MONTH, day)
                // If parsed date is in the past, it's probably next year
                if (before(now)) add(Calendar.YEAR, 1)
            } else {
                // Time only — if time is earlier than now, assume tomorrow
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val diffMins = (ride.timeInMillis - now.timeInMillis) / 60_000.0
        val inWindow = diffMins >= (minHours * 60) && diffMins <= (maxHours * 60)
        Log.d(TAG, "Time check: ride=$hour:$minute diffMins=$diffMins window=${minHours*60}-${maxHours*60} inWindow=$inWindow")
        return inWindow
    }
}
