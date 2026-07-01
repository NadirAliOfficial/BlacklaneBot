package com.blacklanebot

import android.content.Context
import android.content.SharedPreferences

object BotConfig {
    private const val PREFS = "bot_config"
    private const val KEY_MIN_HOURS = "min_hours"
    private const val KEY_MAX_HOURS = "max_hours"

    const val BLACKLANE_PACKAGE = "com.blacklane.passenger"

    // Canadian provinces and major cities used to identify Canada rides
    val CANADA_KEYWORDS = setOf(
        // Provinces & territories
        " ON", " BC", " QC", " AB", " MB", " SK", " NS", " NB", " NL", " PE", " NT", " NU", " YT",
        ", ON", ", BC", ", QC", ", AB", ", MB", ", SK", ", NS", ", NB", ", NL",
        "Ontario", "British Columbia", "Quebec", "Alberta", "Manitoba", "Saskatchewan",
        "Nova Scotia", "New Brunswick", "Newfoundland", "Labrador", "Prince Edward Island",
        "Northwest Territories", "Nunavut", "Yukon",
        // Major airports
        "YYZ", "YVR", "YUL", "YYC", "YOW", "YEG", "YHZ", "YWG", "YQB", "YXE",
        // Major cities
        "Toronto", "Vancouver", "Montreal", "Calgary", "Edmonton", "Ottawa",
        "Winnipeg", "Halifax", "Quebec City", "Saskatoon", "Regina", "Hamilton",
        "Kitchener", "London", "Windsor", "Mississauga", "Brampton", "Markham",
        "Vaughan", "Burnaby", "Surrey", "Richmond", "Kelowna"
    )

    // Keywords that confirm USA — if found, skip the ride regardless
    val USA_KEYWORDS = setOf(
        ", NY", ", CA", ", TX", ", FL", ", IL", ", WA", ", MA", ", GA", ", NV",
        "New York", "Los Angeles", "Chicago", "Houston", "Miami", "Seattle",
        "Boston", "Atlanta", "Las Vegas", "San Francisco", "JFK", "LAX", "ORD",
        "MIA", "ATL", "SFO", "BOS", "EWR", "DFW", "LGA"
    )

    fun getMinHours(ctx: Context): Float = prefs(ctx).getFloat(KEY_MIN_HOURS, 1.0f)
    fun getMaxHours(ctx: Context): Float = prefs(ctx).getFloat(KEY_MAX_HOURS, 2.0f)

    fun save(ctx: Context, minHours: Float, maxHours: Float) {
        prefs(ctx).edit().putFloat(KEY_MIN_HOURS, minHours).putFloat(KEY_MAX_HOURS, maxHours).apply()
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
