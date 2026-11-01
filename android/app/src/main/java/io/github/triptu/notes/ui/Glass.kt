package io.github.triptu.notes.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import io.github.triptu.notes.ui.theme.NotesTheme

/**
 * Frosted glass for controls floating over content: what's behind [haze] shows through blurred and
 * mostly covered by the raised colour, with a faint rim in dark mode. The search pill and the
 * editor's typing buttons share it.
 */
@Composable
fun Modifier.glass(haze: HazeState, shape: Shape): Modifier {
    val colors = NotesTheme.colors
    val dark = isSystemInDarkTheme()
    val blur = remember(colors) {
        HazeBlurStyle {
            blurRadius(24.dp)
            backgroundColor(colors.bg)
            colorEffects(listOf(HazeColorEffect.tint(colors.raised.copy(alpha = 0.82f))))
            noiseFactor(0f)
        }
    }
    return this
        .clip(shape)
        .hazeBlur(HazeInput.Sources(haze), blur)
        .then(if (dark) Modifier.border(1.dp, Color.White.copy(alpha = 0.07f), shape) else Modifier)
}
