package com.soapjournal.app.ui.navigation

import com.soapjournal.app.data.prefs.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveStartDestinationTest {
    @Test
    fun settingsResumeOpensHomeSoBackIsNotADeadEnd() {
        val prefs = UserPreferences(resumeRoute = "settings")
        assertEquals(Routes.HOME, resolveStartDestination(prefs))
    }

    @Test
    fun editorResumeKeepsTheEntry() {
        val prefs = UserPreferences(resumeRoute = "editor", resumeEntryId = 42L)
        assertEquals(Routes.editor(42L), resolveStartDestination(prefs))
    }

    @Test
    fun editorWithoutIdOpensHome() {
        val prefs = UserPreferences(resumeRoute = "editor", resumeEntryId = -1L)
        assertEquals(Routes.HOME, resolveStartDestination(prefs))
    }

    @Test
    fun bibleResumeIsRestored() {
        val prefs = UserPreferences(resumeRoute = "bible")
        assertEquals(Routes.BIBLE, resolveStartDestination(prefs))
    }

    @Test
    fun blankResumeOpensHome() {
        assertEquals(Routes.HOME, resolveStartDestination(UserPreferences()))
    }
}
