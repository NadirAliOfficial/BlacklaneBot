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

    private fun firstLaunch(ctx: Context): Long =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_FIRST_LAUNCH, System.currentTimeMillis())
}
