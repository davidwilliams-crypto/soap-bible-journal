package com.soapjournal.app

/**
 * Product scope notes:
 * - Preferred online version: CSB (fetched remotely when online; not bundled).
 * - Offline Bible text: public-domain KJV corpus (VOTD + fallback chapters).
 * - Other modern versions (ESV/NIV/etc.): selectable; licensed feeds not wired yet.
 * - Sync: still local-only; accountability uses Android share intents.
 */
object MvpScope {
    const val EMBEDDED_KJV = true
    const val ONLINE_CSB = true
    const val LICENSED_VERSION_FEEDS = false
    const val CLOUD_SYNC = false
}
