package ru.landilf.ollink.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Native Compose annotated text; no Android Views or HTML bridge. */
@Composable
fun MarkdownText(source: String, modifier: Modifier = Modifier) {
    val text = remember(source) { formatMarkdown(source) }
    Text(text, modifier)
}

internal fun formatMarkdown(source: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    Regex("```[^\\n]*\\n([\\s\\S]*?)(?:```|$)").findAll(source).forEach { match ->
        appendPlain(source.substring(cursor, match.range.first))
        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(match.groupValues[1].trimEnd()) }
        append("\n")
        cursor = match.range.last + 1
    }
    appendPlain(source.substring(cursor))
}

private fun AnnotatedString.Builder.appendPlain(text: String) {
    text.split('\n').forEachIndexed { index, line ->
        if (index > 0) append("\n")
        val heading = Regex("^#{1,6} (.+)$").matchEntire(line)
        if (heading != null) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(heading.groupValues[1]) }
        else appendInline(line)
    }
}
private fun AnnotatedString.Builder.appendInline(text: String) {
    var cursor = 0
    Regex("`([^`\\n]+)`|\\*\\*(.+?)\\*\\*").findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val code = match.groupValues[1].isNotEmpty()
        withStyle(if (code) SpanStyle(fontFamily = FontFamily.Monospace) else SpanStyle(fontWeight = FontWeight.Bold)) {
            append(if (code) match.groupValues[1] else match.groupValues[2])
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}
