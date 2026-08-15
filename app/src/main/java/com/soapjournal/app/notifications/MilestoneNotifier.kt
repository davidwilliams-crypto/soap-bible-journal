package com.soapjournal.app.notifications

import android.content.Context

/** Celebrates a streak milestone (7/30/100/365 days) with a one-off notification. */
object MilestoneNotifier {
    const val CHANNEL_MILESTONE = "soap_milestone"

    fun celebrate(context: Context, days: Int) {
        Notifications.post(
            context,
            CHANNEL_MILESTONE,
            "$days days in the Word",
            "You've kept a $days-day SOAP rhythm. Well done, good and faithful servant."
        )
    }
}
