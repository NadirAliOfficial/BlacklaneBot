package com.blacklanebot

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var tvNotificationStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvBotStatus: TextView
    private lateinit var tvLastAction: TextView
    private lateinit var etMinHours: EditText
    private lateinit var etMaxHours: EditText
    private lateinit var expiredOverlay: LinearLayout
    private lateinit var mainContent: ScrollView
    private lateinit var tvLog: TextView
    private lateinit var logScroll: ScrollView

    private val handler = Handler(Looper.getMainLooper())

    private val botReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val msg = intent.getStringExtra("message") ?: return
            when (intent.action) {
                "com.blacklanebot.BOT_ACTION" -> tvLastAction.text = "Last: $msg"
                "com.blacklanebot.BOT_LOG" -> appendLog(msg)
            }
        }
    }

    private fun appendLog(msg: String) {
        BotLogger.append(msg)
        tvLog.text = BotLogger.getLog()
        logScroll.post { logScroll.fullScroll(android.view.View.FOCUS_DOWN) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TrialManager.init(this)
        setContentView(R.layout.activity_main)

        tvNotificationStatus = findViewById(R.id.tvNotificationStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        tvBotStatus = findViewById(R.id.tvBotStatus)
        tvLastAction = findViewById(R.id.tvLastAction)
        expiredOverlay = findViewById(R.id.expiredOverlay)
        tvLog = findViewById(R.id.tvLog)
        logScroll = findViewById(R.id.logScroll)
        mainContent = findViewById(R.id.mainContent)
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

        findViewById<Button>(R.id.btnClearLog).setOnClickListener {
            tvLog.text = ""
            BotLogger.clear()
        }

        registerReceiver(botReceiver, IntentFilter("com.blacklanebot.BOT_ACTION"))
        registerReceiver(botReceiver, IntentFilter("com.blacklanebot.BOT_LOG"))
    }

    override fun onResume() {
        super.onResume()
        updateStatusUI()
        updateTrialUI()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(botReceiver)
    }

    private fun updateTrialUI() {
        if (TrialManager.isExpired(this)) {
            expiredOverlay.visibility = View.VISIBLE
            mainContent.visibility = View.INVISIBLE
        } else {
            expiredOverlay.visibility = View.GONE
            mainContent.visibility = View.VISIBLE
        }
    }

    private fun updateStatusUI() {
        val notifGranted = isNotificationListenerEnabled()
        val a11yGranted = isAccessibilityServiceEnabled()

        tvNotificationStatus.text = "Notification Access: ${if (notifGranted) "GRANTED" else "NOT GRANTED"}"
        tvNotificationStatus.setTextColor(if (notifGranted) 0xFF388E3C.toInt() else 0xFFD32F2F.toInt())

        tvAccessibilityStatus.text = "Accessibility Service: ${if (a11yGranted) "GRANTED" else "NOT GRANTED"}"
        tvAccessibilityStatus.setTextColor(if (a11yGranted) 0xFF388E3C.toInt() else 0xFFD32F2F.toInt())

        tvBotStatus.text = when {
            TrialManager.isExpired(this) -> "Bot Status: TRIAL EXPIRED"
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
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(service, ignoreCase = true)) return true
        }
        return false
    }
}
