package io.github.triptu.notes.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The markdown niceties of typing in a note, as pure functions of the editor value: lists carry on
 * past Enter, markup pairs close themselves, `[]` makes a checkbox, a URL pasted over words links
 * them, and the editor's row of buttons indents lines and toggles checkboxes.
 */
object MarkdownTyping {
    /** Adjusts [new], the value after one edit from the keyboard, for the markdown it typed. */
    fun afterEdit(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        // A word being composed belongs to the keyboard; rewriting it would break the composition.
        // Frontmatter and code blocks aren't markdown prose, so they're typed as is.
        if (new.composition != null || !inProse(old.text, old.selection.min)) return new
        insertion(old, new)?.let { (at, replaced, typed) ->
            return when {
                typed == "\n" && replaced.isEmpty() -> continueList(old.text, at)
                typed.length == 1 && replaced.isNotEmpty() && typed[0] in Wrappers -> wrap(old, typed[0])
                typed.length == 1 && replaced.isEmpty() -> typeChar(old.text, at, typed[0])
                replaced.isNotEmpty() && !replaced.contains('\n') && Url.matches(typed.trim()) -> {
                    val link = "[$replaced](${typed.trim()})"
                    TextFieldValue(old.text.replaceRange(at, at + replaced.length, link), TextRange(at + link.length))
                }
                else -> null
            } ?: new
        }
        if (isBackspace(old, new)) return backspace(old.text, old.selection.start) ?: new
        return new
    }

    /** Moves the selected lines (or the cursor's line) one level in. */
    fun indent(value: TextFieldValue): TextFieldValue {
        val unit = indentUnit(value.text)
        val moved = selectedLines(value).map { line ->
            val renumber = if (line.kind == ListKind.Ordered) line.numberChange(1) else null
            listOfNotNull(Change(line.start, line.start, unit), renumber)
        }
        if (moved.isEmpty()) return value
        return renumber(value.apply(moved.flatten()))
    }

    /** Moves the selected lines (or the cursor's line) one level out. */
    fun outdent(value: TextFieldValue): TextFieldValue {
        val spaces = levelSpaces(value.text)
        val changes = selectedLines(value).mapNotNull { line -> line.outdentChange(spaces) }
        if (changes.isEmpty()) return value
        return renumber(value.apply(changes))
    }

    /** Makes the selected lines checkboxes, or plain lines again when they all are already. */
    fun toggleChecklist(value: TextFieldValue): TextFieldValue {
        val lines = selectedLines(value)
        val allTasks = lines.all { it.kind == ListKind.Task }
        val changes = lines.mapNotNull { line ->
            when {
                allTasks -> Change(line.indentEnd, line.markerEnd, "")
                line.kind == ListKind.Task -> null
                line.kind == ListKind.Bullet -> Change(line.markerEnd, line.markerEnd, "[ ] ")
                line.kind == ListKind.Quote -> Change(line.markerEnd, line.markerEnd, "- [ ] ")
                else -> Change(line.indentEnd, line.markerEnd, "- [ ] ")
            }
        }
        return value.apply(changes)
    }

    // Enter

    private fun continueList(text: String, at: Int): TextFieldValue? {
        val line = ListLine.at(text, at)
        if (line.kind == null || at < line.markerEnd) return null
        val content = text.substring(line.markerEnd, line.end)
        if (content.isBlank()) {
            // Enter on an empty item ends the list, a level at a time.
            val before = TextFieldValue(text, TextRange(at))
            line.outdentChange(levelSpaces(text))?.let { return renumber(before.apply(listOf(it))) }
            return TextFieldValue(text.replaceRange(line.start, line.end, ""), TextRange(line.start))
        }
        val next = "\n" + line.indent + line.nextMarker()
        val value = TextFieldValue(text.replaceRange(at, at, next), TextRange(at + next.length))
        return if (line.kind == ListKind.Ordered) renumber(value) else value
    }

    // One character

    private fun typeChar(text: String, at: Int, c: Char): TextFieldValue? {
        val before = text.getOrNull(at - 1)
        val after = text.getOrNull(at)
        val inserted = text.replaceRange(at, at, c.toString())
        fun caret(text: String, offset: Int) = TextFieldValue(text, TextRange(offset))
        return when {
            // Typing the closer of a pair steps over it, so the pair isn't doubled.
            after == c && isCloser(text, at, c) -> caret(text, at + 1)
            c == '[' && before == '[' && after != ']' -> caret(inserted.replaceRange(at + 1, at + 1, "]]"), at + 1)
            c == '*' && before == '*' && text.getOrNull(at - 2) != '*' && after != '*' -> caret(inserted.replaceRange(at + 1, at + 1, "**"), at + 1)
            c == '`' && before != '`' && (after == null || after.isWhitespace()) -> caret(inserted.replaceRange(at + 1, at + 1, "`"), at + 1)
            c == ']' -> checkboxShortcut(inserted, at + 1)
            else -> null
        }
    }

    private fun isCloser(text: String, at: Int, c: Char): Boolean {
        val before = text.getOrNull(at - 1)
        return when (c) {
            ']' -> text.startsWith("]]", at) || before == ']'
            '*' -> text.startsWith("**", at) || before == '*'
            '`' -> before != null && !before.isWhitespace()
            else -> false
        }
    }

    /** `[]` typed at the start of a line, or of a bullet, makes it a checkbox. */
    private fun checkboxShortcut(text: String, caret: Int): TextFieldValue? {
        val lineStart = text.lastIndexOf('\n', caret - 1) + 1
        val typed = EmptyBox.matchEntire(text.substring(lineStart, caret)) ?: return null
        val box = typed.groupValues[1] + (typed.groupValues[2].ifEmpty { "-" }) + " [ ] "
        return TextFieldValue(text.replaceRange(lineStart, caret, box), TextRange(lineStart + box.length))
    }

    private fun wrap(old: TextFieldValue, c: Char): TextFieldValue {
        val sel = old.selection
        val close = if (c == '[') ']' else c
        val text = old.text.substring(0, sel.min) + c + old.text.substring(sel.min, sel.max) + close + old.text.substring(sel.max)
        return TextFieldValue(text, TextRange(sel.min + 1, sel.max + 1))
    }

    // Backspace

    private fun backspace(text: String, at: Int): TextFieldValue? {
        // An empty pair goes as one.
        for ((open, close) in Pairs) {
            if (text.startsWith(open, at - open.length) && text.startsWith(close, at)) {
                val start = at - open.length
                return TextFieldValue(text.removeRange(start, at + close.length), TextRange(start))
            }
        }
        // At the start of an item's text, backspace takes off one layer of its marker.
        val line = ListLine.at(text, at)
        if (line.kind == null || at != line.markerEnd) return null
        val value = TextFieldValue(text, TextRange(at))
        line.outdentChange(levelSpaces(text))?.let { return renumber(value.apply(listOf(it))) }
        val change = if (line.kind == ListKind.Task) Change(line.taskBoxStart, line.markerEnd, "") else Change(line.start, line.markerEnd, "")
        return value.apply(listOf(change))
    }

    // Edits

    /** The text one keyboard edit put in at an offset, and what it replaced, or null for other edits. */
    private fun insertion(old: TextFieldValue, new: TextFieldValue): Triple<Int, String, String>? {
        if (!new.selection.collapsed) return null
        val start = old.selection.min
        val end = old.selection.max
        val caret = new.selection.start
        if (caret < start || new.text.length - caret != old.text.length - end) return null
        if (!new.text.regionMatches(0, old.text, 0, start) || !new.text.regionMatches(caret, old.text, end, old.text.length - end)) return null
        val typed = new.text.substring(start, caret)
        if (typed.isEmpty()) return null
        return Triple(start, old.text.substring(start, end), typed)
    }

    private fun isBackspace(old: TextFieldValue, new: TextFieldValue): Boolean {
        val at = old.selection.start
        return old.selection.collapsed && new.selection.collapsed && at > 0 && new.selection.start == at - 1 &&
            new.text.length == old.text.length - 1 && new.text.regionMatches(0, old.text, 0, at - 1) &&
            new.text.regionMatches(at - 1, old.text, at, old.text.length - at)
    }

    /** Fixes the numbers of the numbered list around the cursor, so each level counts up from its first item. */
    private fun renumber(value: TextFieldValue): TextFieldValue {
        val text = value.text
        var first = ListLine.at(text, value.selection.start.coerceIn(0, text.length))
        if (!first.inList) return value
        while (first.start > 0) {
            val previous = ListLine.at(text, first.start - 1)
            if (!previous.inList) break
            first = previous
        }
        val counters = HashMap<Int, Int>()
        val changes = ArrayList<Change>()
        var line = first
        while (line.inList) {
            val depth = line.depth
            counters.keys.removeAll { it > depth }
            when (line.kind) {
                ListKind.Ordered -> {
                    val n = counters[depth]?.plus(1) ?: line.number
                    line.numberChange(n)?.let(changes::add)
                    counters[depth] = n
                }
                ListKind.Bullet, ListKind.Task -> counters.remove(depth)
                else -> Unit
            }
            if (line.end >= text.length) break
            line = ListLine.at(text, line.end + 1)
        }
        return value.apply(changes)
    }

    private fun selectedLines(value: TextFieldValue): List<ListLine> {
        val text = value.text
        val lines = ArrayList<ListLine>()
        // A selection ending at the start of a line doesn't take that line.
        val last = if (value.selection.collapsed) value.selection.max else value.selection.max - 1
        var line = ListLine.at(text, value.selection.min)
        while (true) {
            lines += line
            if (line.end >= last || line.end >= text.length) break
            line = ListLine.at(text, line.end + 1)
        }
        return lines
    }

    /** Tabs, as Obsidian indents, unless the note already indents its lists with spaces. */
    private fun indentUnit(text: String): String {
        var spaces = Int.MAX_VALUE
        val body = if (text.startsWith("---\n")) text.substringAfter("\n---", "") else text
        for (line in body.lineSequence()) {
            if (line.startsWith('\t')) return "\t"
            val n = line.length - line.trimStart(' ').length
            if (n > 0 && ListLine.Marker.find(line) != null) spaces = minOf(spaces, n)
        }
        return if (spaces == Int.MAX_VALUE) "\t" else " ".repeat(spaces.coerceAtLeast(2))
    }

    /** False inside frontmatter or a code fence. */
    private fun inProse(text: String, at: Int): Boolean {
        if (text.startsWith("---\n")) {
            val close = text.indexOf("\n---", 3)
            if (close < 0 || at <= close + 4) return false
        }
        var fenced = false
        var lineStart = 0
        while (true) {
            val lineEnd = text.indexOf('\n', lineStart)
            if (lineEnd < 0 || lineEnd >= at) break
            if (text.substring(lineStart, lineEnd).trimStart().startsWith("```")) fenced = !fenced
            lineStart = lineEnd + 1
        }
        return !fenced
    }

    private fun levelSpaces(text: String): Int = indentUnit(text).let { if (it == "\t") 4 else it.length }

    private val Url = Regex("""https?://\S+""")
    private val EmptyBox = Regex("""([ \t]*)(?:([-*+]) )?\[]""")
    private const val Wrappers = "*_`[~="
    private val Pairs = listOf("[[" to "]]", "**" to "**", "`" to "`")
}

/** A replacement of [start]..[end] with [text]. */
internal class Change(val start: Int, val end: Int, val text: String)

/** Applies non-overlapping [changes], keeping the selection on the same text. */
internal fun TextFieldValue.apply(changes: List<Change>): TextFieldValue {
    if (changes.isEmpty()) return this
    val sorted = changes.sortedBy { it.start }
    val out = StringBuilder(text.length + 16)
    var at = 0
    for (c in sorted) {
        out.append(text, at, c.start).append(c.text)
        at = c.end
    }
    out.append(text, at, text.length)
    fun map(offset: Int): Int {
        var shift = 0
        for (c in sorted) {
            when {
                c.end <= offset -> shift += c.text.length - (c.end - c.start)
                c.start < offset -> return c.start + shift + c.text.length
                else -> break
            }
        }
        return offset + shift
    }
    return TextFieldValue(out.toString(), TextRange(map(selection.start), map(selection.end)))
}

internal enum class ListKind { Bullet, Task, Ordered, Quote }

/** One line of the note and its list or quote marker, if it has one. */
internal class ListLine(
    val start: Int,
    val end: Int,
    val indent: String,
    val kind: ListKind?,
    /** Just past the marker and its space; the end of the indent for a plain line. */
    val markerEnd: Int,
    private val marker: String,
) {
    val indentEnd: Int get() = start + indent.length
    val number: Int get() = marker.takeWhile(Char::isDigit).toIntOrNull() ?: 0
    /** Indent width with tabs as four columns, to compare levels. */
    val depth: Int get() = indent.sumOf { if (it == '\t') 4 else 1 }
    /** Part of a list: an item, or a line indented under one. */
    val inList: Boolean get() = (kind != null && kind != ListKind.Quote) || (indent.isNotEmpty() && end > indentEnd)
    /** Where a task's `[ ] ` starts. */
    val taskBoxStart: Int get() = indentEnd + 2

    fun nextMarker(): String = when (kind) {
        ListKind.Task -> marker.substring(0, 2) + "[ ] "
        ListKind.Ordered -> "${number + 1}${marker.trimEnd().last()} "
        ListKind.Quote -> "> "
        else -> marker
    }

    fun numberChange(n: Int): Change? {
        val digits = marker.takeWhile(Char::isDigit).length
        if (n == number) return null
        return Change(indentEnd, indentEnd + digits, n.toString())
    }

    /** Takes one level of indent off, or null when the line has none. [spaces] is a level in spaces. */
    fun outdentChange(spaces: Int): Change? {
        if (indent.isEmpty()) return null
        val remove = if (indent.startsWith('\t')) 1 else minOf(indent.takeWhile { it == ' ' }.length, spaces).coerceAtLeast(1)
        return Change(start, start + remove, "")
    }

    companion object {
        val Marker = Regex("""^[ \t]*(?:[-*+] \[[ xX]](?: |$)|[-*+] |\d{1,9}[.)] |>(?: |$))""")

        fun at(text: String, offset: Int): ListLine {
            val start = text.lastIndexOf('\n', offset - 1) + 1
            val end = text.indexOf('\n', offset).let { if (it < 0) text.length else it }
            val line = text.substring(start, end)
            val indent = line.takeWhile { it == ' ' || it == '\t' }
            val match = Marker.find(line)
            val marker = match?.value?.substring(indent.length).orEmpty()
            val kind = when {
                match == null -> null
                marker.startsWith(">") -> ListKind.Quote
                marker.first().isDigit() -> ListKind.Ordered
                marker.length > 2 && marker[2] == '[' -> ListKind.Task
                else -> ListKind.Bullet
            }
            return ListLine(start, end, indent, kind, start + indent.length + marker.length, marker)
        }
    }
}
