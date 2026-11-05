package io.github.triptu.notes.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.sp
import io.github.triptu.notes.ui.theme.LightColors
import org.junit.Assert.assertEquals
import org.junit.Test

class LivePreviewTest {
    private val note = "## Big ideas\nEveryone quotes it.\n"

    /** The heading's `## ` is drawn 1sp tall when hidden; at full size when shown. */
    private fun markerShown(selection: TextRange): Boolean {
        val shown = LivePreview(MarkdownParser.parse(note), selection, focused = true, colors = LightColors, measure = { 0.sp })
            .filter(AnnotatedString(note))
        return shown.text.spanStyles.none { it.start == 0 && it.end == 3 && it.item.fontSize == 1.sp }
    }

    @Test
    fun `markup shows on the cursor's line`() {
        assertEquals(true, markerShown(TextRange(5)))
    }

    @Test
    fun `markup stays hidden away from the cursor`() {
        assertEquals(false, markerShown(TextRange(20)))
    }

    @Test
    fun `a selection leaves the markup hidden, so dragging a handle across it keeps the text still`() {
        assertEquals(false, markerShown(TextRange(0, 20)))
        assertEquals(false, markerShown(TextRange(20, 5)))
    }

    private fun drawn(text: String): String =
        LivePreview(MarkdownParser.parse(text), TextRange(text.length), focused = false, colors = LightColors, measure = { 0.sp })
            .filter(AnnotatedString(text)).text.text

    @Test
    fun `a list marker can't be wrapped apart from a long link after it`() {
        val link = "https://github.com/triptu/notes/blob/main/android/app/src/main/java/io/github/triptu/notes/editor/LivePreview.kt"
        for (marker in listOf("- ", "- [ ] ", "- [x] ", "1. ")) {
            val shown = drawn(marker + link)
            assertEquals(marker, false, shown.substring(0, marker.length).contains(' '))
            assertEquals(marker, link, shown.substring(marker.length))
        }
    }
}
