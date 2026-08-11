package com.soapjournal.app.ui.bible

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import com.soapjournal.app.data.bible.BibleVerse
import com.soapjournal.app.ui.theme.JesusRed

@Composable
fun RedLetterVerseText(
    verse: BibleVerse,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    narratorColor: Color = LocalContentColor.current
) {
    Text(
        text = buildAnnotatedString {
            verse.displaySpans().forEach { span ->
                withStyle(
                    SpanStyle(
                        color = if (span.wordsOfChrist) JesusRed else narratorColor,
                        fontStyle = if (span.wordsOfChrist) FontStyle.Normal else FontStyle.Normal
                    )
                ) {
                    append(span.text)
                }
            }
        },
        modifier = modifier,
        style = style
    )
}
