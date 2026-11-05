package io.github.triptu.notes.editor

import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionDragTest {
    @Test
    fun `the end that moved is the dragged one`() {
        assertEquals(DraggedEnd.End, draggedEnd(TextRange(2, 5), TextRange(2, 9)))
        assertEquals(DraggedEnd.Start, draggedEnd(TextRange(2, 5), TextRange(0, 5)))
        assertEquals(null, draggedEnd(TextRange(2, 5), TextRange(3, 9)))
        assertEquals(null, draggedEnd(TextRange(2, 5), TextRange(2, 5)))
        assertEquals(null, draggedEnd(TextRange(4), TextRange(7)))
    }

    @Test
    fun `an end moves past the other, keeping its role`() {
        assertEquals(TextRange(9, 5), TextRange(2, 5).withEnd(DraggedEnd.Start, 9))
        assertEquals(TextRange(2, 1), TextRange(2, 5).withEnd(DraggedEnd.End, 1))
        assertEquals(9, TextRange(9, 5).at(DraggedEnd.Start))
    }

    private fun speed(fingerY: Float) = edgeScrollSpeed(fingerY, height = 1000f, topMargin = 100f, bottomMargin = 200f, zone = 100f, maxSpeed = 600f)

    @Test
    fun `the middle of the screen is still`() {
        assertEquals(0f, speed(200f))
        assertEquals(0f, speed(500f))
        assertEquals(0f, speed(700f))
    }

    @Test
    fun `the edge zones scroll faster toward the edge, and no faster past the bars`() {
        assertEquals(-300f, speed(150f))
        assertEquals(-600f, speed(100f))
        assertEquals(-600f, speed(0f))
        assertEquals(300f, speed(750f))
        assertEquals(600f, speed(800f))
        assertEquals(600f, speed(1000f))
    }
}
