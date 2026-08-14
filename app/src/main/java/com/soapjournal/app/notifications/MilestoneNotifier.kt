package com.soapjournal.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.soapjournal.app.MainActivity
import com.soapjournal.app.R

/** Celebrates a streak milestone (7/30/100/365 days) with a one-off notification. */
object MilestoneNotifier {
    const val CHANNEL_MILESTONE = "soap_milestone"

    fun celebrate(context: Context, days: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val launch = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context,
            CHANNEL_MILESTONE.hashCode(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_MILESTONE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$days days in the Word")
            .setContentText("You've kept a $days-day SOAP rhythm. Well done, good and faithful servant.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(CHANNEL_MILESTONE.hashCode(), notification)
    }
}
