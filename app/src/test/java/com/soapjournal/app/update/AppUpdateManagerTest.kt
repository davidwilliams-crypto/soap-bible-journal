package com.soapjournal.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateManagerTest {
    @Test
    fun parsesVersionCodeFromReleaseBody() {
        val body = """
            SOAP Journal 1.1.0
            versionCode=2

            - Online CSB/NLT
        """.trimIndent()
        assertEquals(2L, AppUpdateManager.parseVersionCode(body, "app.apk", "v1.1.0"))
    }

    @Test
    fun parsesVersionCodeFromAssetName() {
        assertEquals(
            3L,
            AppUpdateManager.parseVersionCode(null, "soap-journal-v3.apk", "v1.2.0")
        )
    }

    @Test
    fun returnsNullWhenMissing() {
        assertNull(AppUpdateManager.parseVersionCode("notes only", "soap-journal.apk", "v1.1.0"))
    }
}
