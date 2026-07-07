package com.blacklanebot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat

class BlacklaneAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BLBot-A11y"
        var instance: BlacklaneAccessibilityService? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var pendingOfferCheck = false
    private var lastAcceptTime = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "Accessibility service started")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun onOfferNotificationReceived() {
        pendingOfferCheck = true
        Log.i(TAG, "Offer notification received — opening notification shade to tap it")
        // Open notification shade and tap the Blacklane notification (bypasses startActivity restrictions)
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            handler.postDelayed({ tapBlacklaneNotification() }, 1000)
        }, 500)
    }

    private fun tapBlacklaneNotification() {
        val root = rootInActiveWindow ?: return
        val rootPkg = root.packageName?.toString() ?: ""
        // Notification shade is hosted by systemui
        if (!rootPkg.contains("systemui", ignoreCase = true) && rootPkg != "android") {
            // Notification shade didn't open — fall back to direct scan
            Log.i(TAG, "Shade not open (pkg=$rootPkg), scheduling direct scan in 2s")
            handler.postDelayed({ scanOffersScreen() }, 2000)
            return
        }
        // Find the Blacklane notification and tap it
        val notifNode = findBlacklaneNotification(root)
        if (notifNode != null) {
            Log.i(TAG, "Tapping Blacklane notification in shade")
            notifNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            handler.postDelayed({ scanOffersScreen() }, 2000)
        } else {
            Log.i(TAG, "Blacklane notification not found in shade — direct scan")
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.postDelayed({ scanOffersScreen() }, 1500)
        }
    }

    private fun findBlacklaneNotification(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val text = (root.text?.toString() ?: "") + (root.contentDescription?.toString() ?: "")
        if (text.contains("blacklane", ignoreCase = true) || text.contains("New offer", ignoreCase = true)) {
            if (root.isClickable) return root
        }
        for (i in 0 until root.childCount) {
            val found = findBlacklaneNotification(root.getChild(i) ?: continue)
            if (found != null) return found
        }
        return null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg != BotConfig.BLACKLANE_PACKAGE) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (pendingOfferCheck) {
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({ scanOffersScreen() }, 800)
                }
            }
        }
    }

    private fun scanOffersScreen() {
        if (TrialManager.isExpired(this)) {
            Log.i(TAG, "Trial expired — bot disabled")
            BotLogger.log(this, "ERROR: Trial expired")
            pendingOfferCheck = false
            return
        }

        val root = rootInActiveWindow ?: run {
            Log.i(TAG, "No root window")
            return
        }

        val rootPkg = root.packageName?.toString() ?: ""
        if (rootPkg != BotConfig.BLACKLANE_PACKAGE) {
            Log.i(TAG, "Not on Blacklane screen: $rootPkg")
            return
        }

        Log.i(TAG, "Scanning offers screen")
        BotLogger.log(this, "SCANNING screen for offers...")
        val minH = BotConfig.getMinHours(this)
        val maxH = BotConfig.getMaxHours(this)

        // Collect all leaf text nodes in document order, split by accept buttons into "cards"
        val leaves = mutableListOf<AccessibilityNodeInfo>()
        collectLeafNodes(root, leaves)
        Log.i(TAG, "Total leaf nodes: ${leaves.size}")

        // Find positions of accept buttons (the "→" / "Accept offer" nodes)
        val acceptIndices = leaves.indices.filter { i ->
            val n = leaves[i]
            val t = n.text?.toString() ?: ""
            val d = n.contentDescription?.toString() ?: ""
            t == "→" || d.contains("accept offer", ignoreCase = true) ||
            t.contains("Accept offer") || t.contains("→")
        }

        Log.i(TAG, "Found ${acceptIndices.size} accept buttons in leaf scan")
        BotLogger.log(this, "FOUND ${acceptIndices.size} offer(s) on screen")

        if (acceptIndices.isEmpty()) {
            pendingOfferCheck = false
            BotLogger.log(this, "No offers visible")
            return
        }

        var accepted = false
        for ((cardNum, acceptIdx) in acceptIndices.withIndex()) {
            val acceptNode = leaves[acceptIdx]

            // Card text = leaf nodes from after previous accept+price up to this accept button
            val prevEnd = if (cardNum == 0) 0 else (acceptIndices[cardNum - 1] + 2)
            val cardText = leaves.subList(prevEnd, acceptIdx)
                .mapNotNull { it.text?.toString()?.takeIf { t -> t.isNotBlank() } }
                .joinToString(" ")

            Log.i(TAG, "Card $cardNum: $cardText")

            val isCanada = OfferParser.isCanadaRide(cardText)
            val inWindow = OfferParser.isWithinTimeWindow(cardText, minH, maxH)
            val snippet = cardText.take(70)

            when {
                !isCanada -> BotLogger.log(this, "SKIP (not Canada): $snippet")
                !inWindow -> BotLogger.log(this, "SKIP (outside ${minH}-${maxH}h): $snippet")
                else -> {
                    BotLogger.log(this, "MATCH — accepting: $snippet")
                    val now = System.currentTimeMillis()
                    if (now - lastAcceptTime < 5000) {
                        BotLogger.log(this, "SKIP — too soon after last accept")
                        continue
                    }
                    val r = android.graphics.Rect()
                    acceptNode.getBoundsInScreen(r)
                    if (!r.isEmpty) {
                        Log.i(TAG, "Tapping accept at (${r.centerX()},${r.centerY()})")
                        performTap(r.centerX().toFloat(), r.centerY().toFloat())
                        lastAcceptTime = now
                        pendingOfferCheck = false
                        BotLogger.log(this, "ACCEPTED")
                        notifyMainActivity("Accepted: $snippet")
                        showAcceptedPopup(snippet)
                        accepted = true
                        break
                    } else {
                        BotLogger.log(this, "ACCEPT FAILED — button off screen")
                    }
                }
            }
        }

        if (!accepted) {
            pendingOfferCheck = false
            BotLogger.log(this, "DONE — no matching offers")
        }
    }

    private fun collectLeafNodes(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.childCount == 0) {
            val t = node.text?.toString() ?: ""
            val d = node.contentDescription?.toString() ?: ""
            if (t.isNotBlank() || d.isNotBlank()) result.add(node)
            return
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectLeafNodes(it, result) }
        }
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun notifyMainActivity(message: String) {
        val intent = android.content.Intent("com.blacklanebot.BOT_ACTION")
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }

    fun showAcceptedPopup(detail: String) {
        val channelId = "blbot_accepted"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Offer Accepted", NotificationManager.IMPORTANCE_HIGH)
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Offer Accepted")
            .setContentText(detail)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    override fun onInterrupt() {
        Log.i(TAG, "Service interrupted")
    }
}
