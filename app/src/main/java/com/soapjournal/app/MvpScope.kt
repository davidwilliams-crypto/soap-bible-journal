package com.soapjournal.app

/**
 * Product scope notes:
 * - Offline Bible text: public-domain KJV corpus (VOTD + sample chapters).
 * - Modern versions (ESV/NIV/etc.): selectable in UI; licensed feeds not bundled.
 * - Sync: still local-only; accountability uses Android share intents.
 */
object MvpScope {
    const val EMBEDDED_KJV = true
    const val LICENSED_VERSION_FEEDS = false
    const val CLOUD_SYNC = false
}
