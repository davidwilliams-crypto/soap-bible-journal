package com.soapjournal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soapjournal.app.ui.navigation.SoapNavHost
import com.soapjournal.app.ui.theme.SOAPBibleJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SoapJournalApplication
        setContent {
            val prefs by app.container.prefs.preferences.collectAsStateWithLifecycle(
                initialValue = com.soapjournal.app.data.prefs.UserPreferences()
            )
            SOAPBibleJournalTheme(darkTheme = prefs.darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SoapNavHost(container = app.container)
                }
            }
        }
    }
}
