package com.blacklanebot

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class BlacklaneNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "BLBot-NL"
        var instance: BlacklaneNotificationListener? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Notification listener started")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""

        // Log every notification package so we can identify Blacklane's real package name
        Log.d(TAG, "NOTIF pkg=${sbn.packageName} title=$title text=$text")

        val isBlacklane = sbn.packageName == BotConfig.BLACKLANE_PACKAGE ||
            sbn.packageName.contains("blacklane", ignoreCase = true) ||
            title.contains("blacklane", ignoreCase = true) ||
            title.contains("offer", ignoreCase = true) && text.contains("chauffeur", ignoreCase = true)

        if (!isBlacklane) return

        Log.d(TAG, "Blacklane notification matched: pkg=${sbn.packageName} title=$title text=$text")

        if (TrialManager.isExpired(this)) {
            Log.d(TAG, "Trial expired — ignoring notification")
            return
        }

        if (title.contains("offer", ignoreCase = true) ||
            text.contains("offer", ignoreCase = true) ||
            title.contains("new ride", ignoreCase = true)
        ) {
            Log.d(TAG, "New offer detected, signaling accessibility service")
            BlacklaneAccessibilityService.instance?.onOfferNotificationReceived()

            // Try known package first, fall back to matched package
            val pkg = if (sbn.packageName.contains("blacklane", ignoreCase = true))
                sbn.packageName else BotConfig.BLACKLANE_PACKAGE
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            launch?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            launch?.let { startActivity(it) }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
