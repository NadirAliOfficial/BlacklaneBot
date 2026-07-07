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
        Log.i(TAG, "Notification listener started")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != BotConfig.BLACKLANE_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""

        Log.i(TAG, "Blacklane notif: $title — $text")
        BotLogger.log(this, "NOTIF: $title")

        if (TrialManager.isExpired(this)) {
            Log.i(TAG, "Trial expired")
            return
        }

        if (title.contains("offer", ignoreCase = true) ||
            text.contains("offer", ignoreCase = true) ||
            title.contains("new ride", ignoreCase = true)
        ) {
            Log.i(TAG, "New offer — signaling service")
            BotLogger.log(this, "New offer detected — opening Blacklane...")
            BlacklaneAccessibilityService.instance?.onOfferNotificationReceived()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
