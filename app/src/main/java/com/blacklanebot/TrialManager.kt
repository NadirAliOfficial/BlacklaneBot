package com.blacklanebot

import android.content.Context

object TrialManager {
    private const val PREFS = "trial"
    private const val KEY_FIRST_LAUNCH = "first_launch"
    private const val TRIAL_DAYS = 3L
    private const val TRIAL_MS = TRIAL_DAYS * 24 * 60 * 60 * 1000L

    fun init(ctx: Context) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains(KEY_FIRST_LAUNCH)) {
            p.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
    }

    fun isExpired(ctx: Context): Boolean {
        val first = firstLaunch(ctx)
        return System.currentTimeMillis() - first > TRIAL_MS
    }

    fun millisRemaining(ctx: Context): Long {
        val elapsed = System.currentTimeMillis() - firstLaunch(ctx)
        return (TRIAL_MS - elapsed).coerceAtLeast(0)
    }

    fun formattedRemaining(ctx: Context): String {
        val ms = millisRemaining(ctx)
        val hours = ms / (1000 * 60 * 60)
        val minutes = (ms % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (ms % (1000 * 60)) / 1000
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }

    private fun firstLaunch(ctx: Context): Long =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_FIRST_LAUNCH, System.currentTimeMillis())
}
