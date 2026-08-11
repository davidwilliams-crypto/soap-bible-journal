package com.soapjournal.app.data

enum class SoapSection(val key: String, val title: String, val prompt: String) {
    SCRIPTURE("scripture", "Scripture", "What passage are you studying?"),
    OBSERVATION("observation", "Observation", "What does it say?"),
    APPLICATION("application", "Application", "How does this apply to me?"),
    PRAYER("prayer", "Prayer", "Talk to God about it");

    companion object {
        fun fromKey(key: String): SoapSection =
            entries.firstOrNull { it.key == key } ?: OBSERVATION
    }
}
