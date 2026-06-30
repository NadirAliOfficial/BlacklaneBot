package com.blacklanebot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BLBot", "Boot completed — services will auto-start if permissions are still granted")
            // Accessibility + Notification listener services restart automatically if enabled in system settings
        }
    }
}
