package com.blacklanebot

import java.util.Calendar

data class MockOffer(
    val id: Int,
    val category: String,
    val pickupAirport: String,
    val destination: String,
    val city: String,
    val province: String,
    val pickupTimeMs: Long,
    val price: String,
    val isCanada: Boolean
) {
    val pickupLabel: String get() {
        val cal = Calendar.getInstance().apply { timeInMillis = pickupTimeMs }
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                             "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val day = days[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val date = cal.get(Calendar.DAY_OF_MONTH)
        val month = months[cal.get(Calendar.MONTH)]
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return "$day $date $month · ${String.format("%02d:%02d", h, m)}"
    }
}

object MockOfferGenerator {
    fun generate(): List<MockOffer> {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L

        return listOf(
            // SHOULD ACCEPT — Canada, 1.5 hrs from now
            MockOffer(1, "Business", "YYZ", "123 King St W", "Toronto", "ON",
                now + (hour + 30 * 60 * 1000), "\$95.00", true),

            // SHOULD ACCEPT — Canada, exactly 1 hr from now
            MockOffer(2, "Business", "YVR", "800 Robson St", "Vancouver", "BC",
                now + hour, "\$110.00", true),

            // SHOULD NOT — USA ride
            MockOffer(3, "Business", "JFK", "88 E Broadway", "New York", "NY",
                now + hour, "\$85.45", false),

            // SHOULD NOT — Canada but too far (4 hrs)
            MockOffer(4, "First Class", "YUL", "1000 De La Gauchetière", "Montreal", "QC",
                now + 4 * hour, "\$145.00", true),

            // SHOULD NOT — Canada but too soon (20 mins)
            MockOffer(5, "Business", "YYC", "350 7th Ave SW", "Calgary", "AB",
                now + 20 * 60 * 1000, "\$78.00", true),
        )
    }
}
