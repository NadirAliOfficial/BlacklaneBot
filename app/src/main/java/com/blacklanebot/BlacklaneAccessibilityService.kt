package com.blacklanebot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

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
        Log.d(TAG, "Accessibility service started")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun onOfferNotificationReceived() {
        pendingOfferCheck = true
        Log.d(TAG, "Offer notification received, will process on next screen event")
        // Schedule a scan after app opens (give it 2 seconds to load)
        handler.postDelayed({ scanOffersScreen() }, 2000)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName != BotConfig.BLACKLANE_PACKAGE) return

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
        val root = rootInActiveWindow ?: run {
            Log.d(TAG, "No root window")
            return
        }

        if (root.packageName != BotConfig.BLACKLANE_PACKAGE) {
            Log.d(TAG, "Not on Blacklane screen")
            return
        }

        Log.d(TAG, "Scanning offers screen")
        val minH = BotConfig.getMinHours(this)
        val maxH = BotConfig.getMaxHours(this)

        // Collect all text from screen and find offer cards
        val offerCards = findOfferCards(root)
        Log.d(TAG, "Found ${offerCards.size} offer cards")

        for (card in offerCards) {
            val text = extractFullText(card)
            Log.d(TAG, "Card text: $text")

            if (OfferParser.isCanadaRide(text) && OfferParser.isWithinTimeWindow(text, minH, maxH)) {
                Log.d(TAG, "MATCH FOUND - accepting offer")
                val now = System.currentTimeMillis()
                if (now - lastAcceptTime < 5000) {
                    Log.d(TAG, "Too soon since last accept, skipping")
                    continue
                }
                if (acceptOffer(card)) {
                    lastAcceptTime = now
                    pendingOfferCheck = false
                    notifyMainActivity("Accepted: ${text.take(50)}")
                    return
                }
            }
        }

        // If no matching card found, reset flag (no point rescanning without new notification)
        pendingOfferCheck = false
    }

    private fun findOfferCards(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val cards = mutableListOf<AccessibilityNodeInfo>()
        collectCards(root, cards)
        return cards
    }

    private fun collectCards(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        val text = collectNodeText(node)
        // An offer card contains a time pattern and a price ($)
        if (text.contains(Regex("\\d{1,2}:\\d{2}")) && text.contains("$")) {
            result.add(node)
            return // don't recurse into children of a found card
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectCards(it, result) }
        }
    }

    private fun extractFullText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        collectText(node, sb)
        return sb.toString()
    }

    private fun collectText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        node.text?.let { sb.append(it).append(" ") }
        node.contentDescription?.let { sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, sb) }
        }
    }

    private fun collectNodeText(node: AccessibilityNodeInfo): String {
        return buildString {
            fun recurse(n: AccessibilityNodeInfo) {
                n.text?.let { append(it).append(" ") }
                n.contentDescription?.let { append(it).append(" ") }
                for (i in 0 until n.childCount) n.getChild(i)?.let { recurse(it) }
            }
            recurse(node)
        }
    }

    private fun acceptOffer(card: AccessibilityNodeInfo): Boolean {
        // Strategy 1: find a clickable button within the card that looks like Accept
        val btn = findAcceptButton(card)
        if (btn != null) {
            Log.d(TAG, "Clicking accept button via ACTION_CLICK")
            btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }

        // Strategy 2: click the card itself to open detail, then find accept on detail screen
        if (card.isClickable) {
            Log.d(TAG, "Clicking card to open detail view")
            card.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            handler.postDelayed({ clickAcceptOnDetailScreen() }, 1500)
            return true
        }

        // Strategy 3: tap the center of the card's bounding rect
        val rect = android.graphics.Rect()
        card.getBoundsInScreen(rect)
        if (!rect.isEmpty) {
            Log.d(TAG, "Tapping card center via gesture: ${rect.centerX()},${rect.centerY()}")
            performTap(rect.centerX().toFloat(), rect.centerY().toFloat())
            handler.postDelayed({ clickAcceptOnDetailScreen() }, 1500)
            return true
        }

        return false
    }

    private fun findAcceptButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for the blue arrow/accept button — it's usually a circular ImageButton
        val desc = (node.contentDescription?.toString() ?: "").lowercase()
        val className = (node.className?.toString() ?: "").lowercase()
        if (node.isClickable &&
            (desc.contains("accept") || desc.contains("confirm") || desc.contains("arrow") ||
             className.contains("imagebutton") || className.contains("imageview"))
        ) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findAcceptButton(node.getChild(i) ?: continue)
            if (found != null) return found
        }
        return null
    }

    private fun clickAcceptOnDetailScreen() {
        val root = rootInActiveWindow ?: return
        // On detail screen look for a slide-to-accept or accept button
        val btn = findNodeByText(root, "accept", "slide", "confirm", "book")
        if (btn != null) {
            btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Accepted on detail screen")
        } else {
            // Try swipe gesture for slide-to-accept
            performSlideGesture()
        }
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, vararg keywords: String): AccessibilityNodeInfo? {
        val text = (root.text?.toString() ?: "").lowercase() +
                   (root.contentDescription?.toString() ?: "").lowercase()
        if (root.isClickable && keywords.any { text.contains(it) }) return root
        for (i in 0 until root.childCount) {
            val found = findNodeByText(root.getChild(i) ?: continue, *keywords)
            if (found != null) return found
        }
        return null
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun performSlideGesture() {
        val display = resources.displayMetrics
        val w = display.widthPixels.toFloat()
        val h = display.heightPixels.toFloat()
        // Slide from left-center to right-center of lower third of screen
        val y = h * 0.75f
        val path = Path().apply {
            moveTo(w * 0.1f, y)
            lineTo(w * 0.85f, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 500)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Performed slide gesture")
    }

    private fun notifyMainActivity(message: String) {
        val intent = android.content.Intent("com.blacklanebot.BOT_ACTION")
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
}
