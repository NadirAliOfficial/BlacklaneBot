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
        if (sbn.packageName != BotConfig.BLACKLANE_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""

        Log.d(TAG, "Blacklane notification: title=$title text=$text")

        // Trigger on any new offer notification
        if (title.contains("offer", ignoreCase = true) ||
            text.contains("offer", ignoreCase = true) ||
            title.contains("new ride", ignoreCase = true)
        ) {
            Log.d(TAG, "New offer detected, signaling accessibility service")
            BlacklaneAccessibilityService.instance?.onOfferNotificationReceived()

            // Open Blacklane app so accessibility service can read the screen
            val launch = packageManager.getLaunchIntentForPackage(BotConfig.BLACKLANE_PACKAGE)
            launch?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            launch?.let { startActivity(it) }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
