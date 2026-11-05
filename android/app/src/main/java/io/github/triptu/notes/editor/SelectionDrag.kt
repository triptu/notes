package io.github.triptu.notes.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange

/** Which end of a selection a finger holds while dragging a handle. */
enum class DraggedEnd {
    Start, End;

    val other: DraggedEnd get() = if (this == Start) End else Start
}

/** The one end that moved from [before] to [after], or null when neither or both did. */
fun draggedEnd(before: TextRange, after: TextRange): DraggedEnd? = when {
    before.start == after.start && before.end != after.end -> DraggedEnd.End
    before.end == after.end && before.start != after.start -> DraggedEnd.Start
    else -> null
}

fun TextRange.at(end: DraggedEnd): Int = if (end == DraggedEnd.Start) start else this.end

fun TextRange.withEnd(end: DraggedEnd, offset: Int): TextRange =
    if (end == DraggedEnd.Start) TextRange(offset, this.end) else TextRange(start, offset)

/**
 * A finger holding a selection handle. The field moves the handle by the finger's travel alone,
 * so whatever the page scrolls while the finger is down has to be added back to land on the text
 * under it. [scrollAtStart] is the page's scroll when the finger took the handle.
 */
class HandleDrag(val end: DraggedEnd, val scrollAtStart: Int) {
    /** Where the finger is in the viewport, from the handle's last move. */
    var finger: Offset = Offset.Zero
}

/**
 * How fast the page scrolls, in px per second, for a finger at [fingerY] in a viewport [height]
 * px tall during a handle drag: nothing in the middle, up to [maxSpeed] at an edge, negative
 * upwards. The edge zones are [zone] px deep and start inside the [topMargin] and
 * [bottomMargin] kept clear for the bars.
 */
fun edgeScrollSpeed(fingerY: Float, height: Float, topMargin: Float, bottomMargin: Float, zone: Float, maxSpeed: Float): Float {
    val top = topMargin + zone
    val bottom = height - bottomMargin - zone
    return when {
        fingerY < top -> -((top - fingerY) / zone).coerceAtMost(1f) * maxSpeed
        fingerY > bottom -> ((fingerY - bottom) / zone).coerceAtMost(1f) * maxSpeed
        else -> 0f
    }
}
