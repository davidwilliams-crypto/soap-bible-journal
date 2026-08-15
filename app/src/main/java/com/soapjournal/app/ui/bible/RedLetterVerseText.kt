package com.soapjournal.app.ui.bible

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import com.soapjournal.app.data.bible.BibleVerse
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
import com.soapjournal.app.ui.theme.ScriptureFamily

@Composable
fun RedLetterVerseText(
    verse: BibleVerse,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    narratorColor: Color = LocalContentColor.current
) {
    val christColor = LocalJournalSurfaces.current.jesusRed
    val annotated = remember(verse, christColor, narratorColor) {
        buildAnnotatedString {
            verse.displaySpans().forEach { span ->
                withStyle(
                    SpanStyle(
                        color = if (span.wordsOfChrist) christColor else narratorColor,
                        fontStyle = FontStyle.Normal
                    )
                ) {
                    append(span.text)
                }
            }
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style.copy(fontFamily = ScriptureFamily)
    )
}
