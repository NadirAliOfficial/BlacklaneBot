package com.blacklanebot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object TestNotificationHelper {
    private const val CHANNEL_ID = "bl_test"
    private const val CHANNEL_NAME = "Blacklane Test"

    fun createChannel(ctx: Context) {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun sendMockNotification(ctx: Context) {
        MockBlacklaneActivity.currentOffers.clear()
        MockBlacklaneActivity.currentOffers.addAll(MockOfferGenerator.generate())

        val intent = Intent(ctx, MockBlacklaneActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New offer")
            .setContentText("Press to see details")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        NotificationManagerCompat.from(ctx).notify(1001, notif)

        // Also signal the accessibility service directly
        BlacklaneAccessibilityService.instance?.onOfferNotificationReceived()

        // Open mock screen
        ctx.startActivity(intent)
    }
}
