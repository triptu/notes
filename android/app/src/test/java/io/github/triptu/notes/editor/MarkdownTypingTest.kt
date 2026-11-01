package io.github.triptu.notes.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Notes are written with `|` for the cursor, or `<` and `>` around a selection. */
class MarkdownTypingTest {
    private fun note(marked: String): TextFieldValue {
        val caret = marked.indexOf('|')
        if (caret >= 0) return TextFieldValue(marked.removeRange(caret, caret + 1), TextRange(caret))
        val start = marked.indexOf('<')
        val end = marked.indexOf('>') - 1
        return TextFieldValue(marked.replace("<", "").replace(">", ""), TextRange(start, end))
    }

    private fun shown(value: TextFieldValue): String = value.text.replaceRange(value.selection.start, value.selection.start, "|")

    /** Types [keys] one at a time like a keyboard; `⌫` is backspace. */
    private fun type(start: String, keys: String): String {
        var value = note(start)
        for (key in keys) {
            val sel = value.selection
            val typed = if (key == '⌫') {
                if (sel.collapsed) TextFieldValue(value.text.removeRange(sel.start - 1, sel.start), TextRange(sel.start - 1))
                else TextFieldValue(value.text.removeRange(sel.min, sel.max), TextRange(sel.min))
            } else {
                TextFieldValue(value.text.replaceRange(sel.min, sel.max, key.toString()), TextRange(sel.min + 1))
            }
            value = MarkdownTyping.afterEdit(value, typed)
        }
        return shown(value)
    }

    @Test
    fun `enter carries bullets, checkboxes, numbers and quotes onto the next line`() {
        assertEquals("- milk\n- eggs|", type("- milk|", "\neggs"))
        assertEquals("* milk\n* |", type("* milk|", "\n"))
        assertEquals("- [x] milk\n- [ ] |", type("- [x] milk|", "\n"))
        assertEquals("\t- milk\n\t- |", type("\t- milk|", "\n"))
        assertEquals("1. milk\n2. eggs\n3. |", type("1. milk|", "\neggs\n"))
        assertEquals("> said\n> |", type("> said|", "\n"))
        assertEquals("plain\n|", type("plain|", "\n"))
    }

    @Test
    fun `enter in the middle of an item splits it into two items`() {
        assertEquals("- milk\n- |eggs", type("- milk|eggs", "\n"))
    }

    @Test
    fun `enter on an empty item ends the list, one level at a time`() {
        assertEquals("- milk\n|", type("- milk|", "\n\n"))
        assertEquals("- milk\n\t- eggs\n- |", type("- milk\n\t- eggs|", "\n\n"))
        assertEquals("1. milk\n\t1. eggs\n2. |", type("1. milk\n\t1. eggs|", "\n\n"))
    }

    @Test
    fun `a new number renumbers the items after it`() {
        assertEquals("1. a\n2. |\n3. b\n4. c", type("1. a|\n2. b\n3. c", "\n"))
    }

    @Test
    fun `backspace at the start of an item takes off one layer of its marker`() {
        assertEquals("- [ ] |milk", type("\t- [ ] |milk", "⌫"))
        assertEquals("- |milk", type("- [ ] |milk", "⌫"))
        assertEquals("|milk", type("- |milk", "⌫"))
        assertEquals("|milk", type("2. |milk", "⌫"))
        assertEquals("- mil|", type("- milk|", "⌫"))
    }

    @Test
    fun `markup pairs close themselves and step over their closers`() {
        assertEquals("a [[Ideas|]]", type("a |", "[[Ideas"))
        assertEquals("a [[Ideas]] |", type("a |", "[[Ideas]] "))
        assertEquals("**bold** |", type("|", "**bold** "))
        assertEquals("`code` |", type("|", "`code` "))
        assertEquals("```|", type("|", "```"))
        assertEquals("a |", type("a |", "[[⌫"))
    }

    @Test
    fun `typing a markup character over a selection wraps it`() {
        assertEquals("[[Ideas]]", type("<Ideas>", "[[").replace("|", ""))
        assertEquals("**big**", type("<big>", "**").replace("|", ""))
    }

    @Test
    fun `square brackets at the start of a line make a checkbox`() {
        assertEquals("- [ ] |", type("|", "[]"))
        assertEquals("- [ ] milk", type("|", "[]milk").replace("|", ""))
        assertEquals("\t* [ ] |", type("\t* |", "[]"))
        assertEquals("see []|", type("see |", "[]"))
    }

    @Test
    fun `pasting a link over words links them`() {
        val value = note("read <this> later")
        val pasted = TextFieldValue("read https://example.com later", TextRange(24))
        assertEquals("read [this](https://example.com)| later", shown(MarkdownTyping.afterEdit(value, pasted)))
    }

    @Test
    fun `indent and outdent move the cursor's line and keep numbers counting`() {
        assertEquals("1. a\n\t1. |b\n2. c", shown(MarkdownTyping.indent(note("1. a\n2. |b\n3. c"))))
        assertEquals("1. a\n2. |b\n3. c", shown(MarkdownTyping.outdent(note("1. a\n\t1. |b\n2. c"))))
        assertEquals("- a\n  - x\n  - |b", shown(MarkdownTyping.indent(note("- a\n  - x\n- |b"))))
    }

    @Test
    fun `the checklist button turns lines into checkboxes and back`() {
        assertEquals("- [ ] |milk", shown(MarkdownTyping.toggleChecklist(note("|milk"))))
        assertEquals("- [ ] mi|lk", shown(MarkdownTyping.toggleChecklist(note("- mi|lk"))))
        assertEquals("- [ ] milk", shown(MarkdownTyping.toggleChecklist(note("1. milk|"))).replace("|", ""))
        assertEquals("mi|lk", shown(MarkdownTyping.toggleChecklist(note("- [x] mi|lk"))))
    }

    @Test
    fun `edits leave other lists, frontmatter and code alone`() {
        assertEquals("- |a\n\n1. one\n9. nine", shown(MarkdownTyping.outdent(note("    - |a\n\n1. one\n9. nine"))))
        assertEquals("---\naliases: []|\n---\n", type("---\naliases: [|\n---\n", "]"))
        assertEquals("```\n- flag\n|\n```", type("```\n- flag|\n```", "\n"))
        assertEquals("\tone\ntwo", MarkdownTyping.indent(note("<one\n>two")).text)
    }

    @Test
    fun `a star before an existing star is typed, not skipped`() {
        assertEquals("*|*word*", type("|*word*", "*"))
    }

    @Test
    fun `a word the keyboard is still composing is left to the keyboard`() {
        val old = note("|")
        val composing = TextFieldValue("`", TextRange(1), composition = TextRange(0, 1))
        assertEquals(composing, MarkdownTyping.afterEdit(old, composing))
    }
}
