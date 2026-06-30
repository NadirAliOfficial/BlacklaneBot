package com.blacklanebot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.app.Activity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var tvNotificationStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvBotStatus: TextView
    private lateinit var tvLastAction: TextView
    private lateinit var etMinHours: EditText
    private lateinit var etMaxHours: EditText

    private val botReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val msg = intent.getStringExtra("message") ?: return
            tvLastAction.text = "Last: $msg"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvNotificationStatus = findViewById(R.id.tvNotificationStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        tvBotStatus = findViewById(R.id.tvBotStatus)
        tvLastAction = findViewById(R.id.tvLastAction)
        etMinHours = findViewById(R.id.etMinHours)
        etMaxHours = findViewById(R.id.etMaxHours)

        etMinHours.setText(BotConfig.getMinHours(this).toString())
        etMaxHours.setText(BotConfig.getMaxHours(this).toString())

        findViewById<Button>(R.id.btnGrantNotification).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnGrantAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            val min = etMinHours.text.toString().toFloatOrNull() ?: 1f
            val max = etMaxHours.text.toString().toFloatOrNull() ?: 2f
            if (min >= max) {
                Toast.makeText(this, "Min must be less than Max", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            BotConfig.save(this, min, max)
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }

        registerReceiver(botReceiver, IntentFilter("com.blacklanebot.BOT_ACTION"))
    }

    override fun onResume() {
        super.onResume()
        updateStatusUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(botReceiver)
    }

    private fun updateStatusUI() {
        val notifGranted = isNotificationListenerEnabled()
        val a11yGranted = isAccessibilityServiceEnabled()

        tvNotificationStatus.text = "Notification Access: ${if (notifGranted) "GRANTED" else "NOT GRANTED"}"
        tvNotificationStatus.setTextColor(if (notifGranted) 0xFF388E3C.toInt() else 0xFFD32F2F.toInt())

        tvAccessibilityStatus.text = "Accessibility Service: ${if (a11yGranted) "GRANTED" else "NOT GRANTED"}"
        tvAccessibilityStatus.setTextColor(if (a11yGranted) 0xFF388E3C.toInt() else 0xFFD32F2F.toInt())

        tvBotStatus.text = when {
            notifGranted && a11yGranted -> "Bot Status: ACTIVE — monitoring Blacklane"
            else -> "Bot Status: Setup required above"
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(packageName)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${BlacklaneAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabled)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(service, ignoreCase = true)) return true
        }
        return false
    }
}
