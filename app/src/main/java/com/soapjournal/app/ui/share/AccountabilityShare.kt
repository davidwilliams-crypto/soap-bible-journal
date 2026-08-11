package com.soapjournal.app.ui.share

import android.content.Context
import android.content.Intent
import com.soapjournal.app.data.SoapEntryEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AccountabilityShare {
    fun shareReflection(context: Context, entry: SoapEntryEntity) {
        val date = LocalDate.ofEpochDay(entry.entryDateEpochDay)
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        val body = buildString {
            appendLine("SOAP reflection — $date")
            appendLine()
            appendLine("S — Scripture: ${entry.scriptureReference.ifBlank { "(none)" }}")
            if (entry.scriptureText.isNotBlank()) {
                appendLine(entry.scriptureText.take(600))
                appendLine()
            }
            appendLine("O — Observation: (written in my journal)")
            appendLine("A — Application: ${if (entry.applicationFollowThrough) "I'm following through" else "still praying into this"}")
            appendLine("P — Prayer: ${if (entry.prayerFollowThrough) "prayed" else "continuing in prayer"}")
            if (entry.tags.isNotBlank()) {
                appendLine()
                appendLine("Tags: ${entry.tags}")
            }
            appendLine()
            append("Shared from SOAP Journal for accountability.")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SOAP: ${entry.scriptureReference}")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Share with accountability partner"))
    }
}
