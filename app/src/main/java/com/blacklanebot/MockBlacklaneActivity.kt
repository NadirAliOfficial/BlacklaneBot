package com.blacklanebot

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class MockBlacklaneActivity : Activity() {

    companion object {
        const val ACTION_ACCEPT = "com.blacklanebot.MOCK_ACCEPT"
        const val EXTRA_OFFER_ID = "offer_id"
        var currentOffers = mutableListOf<MockOffer>()
    }

    private lateinit var offersContainer: LinearLayout
    private val offerViews = mutableMapOf<Int, View>()

    private val acceptReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id = intent.getIntExtra(EXTRA_OFFER_ID, -1)
            if (id != -1) markAccepted(id)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mock)
        offersContainer = findViewById(R.id.offersContainer)

        if (currentOffers.isEmpty()) currentOffers.addAll(MockOfferGenerator.generate())
        renderOffers()

        registerReceiver(acceptReceiver, IntentFilter(ACTION_ACCEPT))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(acceptReceiver)
    }

    private fun renderOffers() {
        offersContainer.removeAllViews()
        for (offer in currentOffers) {
            val v = LayoutInflater.from(this).inflate(R.layout.item_offer, offersContainer, false)
            v.tag = offer.id

            v.findViewById<TextView>(R.id.tvCategory).text = offer.category
            v.findViewById<TextView>(R.id.tvPickupTime).text = offer.pickupLabel
            v.findViewById<TextView>(R.id.tvAirport).text = offer.pickupAirport
            v.findViewById<TextView>(R.id.tvDestination).text = offer.destination
            v.findViewById<TextView>(R.id.tvCity).text = "${offer.city}, ${offer.province}"
            v.findViewById<TextView>(R.id.tvPrice).text = offer.price

            val acceptBtn = v.findViewById<TextView>(R.id.btnAccept)
            acceptBtn.contentDescription = "Accept offer"
            acceptBtn.setOnClickListener { markAccepted(offer.id) }

            offerViews[offer.id] = v
            offersContainer.addView(v)
        }
    }

    fun markAccepted(offerId: Int) {
        val view = offerViews[offerId] ?: return
        view.findViewById<TextView>(R.id.tvAcceptedBadge).visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.btnAccept).apply {
            text = "✓"
            setBackgroundColor(0xFF388E3C.toInt())
            isEnabled = false
        }
    }
}
