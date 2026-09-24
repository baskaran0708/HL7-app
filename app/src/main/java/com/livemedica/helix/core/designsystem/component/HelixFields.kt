package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing

/**
 * Labelled facts — the 1-up and 2-up grids every detail screen uses for identifiers, times and
 * codes.
 */

/**
 * A labelled fact — `REPORTED / 09:48`.
 *
 * Values default to the mono face because almost every fact on these screens is an identifier, a
 * time or a code, and tabular figures keep two of them in adjacent columns visually aligned.
 */
@Composable
fun HelixMiniField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    mono: Boolean = true,
) {
    val colors = HelixTheme.colors
    Column(modifier = modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        HelixOverline(label)
        Text(
            text = value,
            style = if (mono) HelixTheme.typography.mono else HelixTheme.typography.label,
            color = colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xs + 1.dp),
        )
    }
}

/** Two [HelixMiniField]s in a row, split by a vertical hairline — the design's 2-up fact grid. */
@Composable
fun HelixMiniFieldPair(
    startLabel: String,
    startValue: String,
    endLabel: String,
    endValue: String,
    modifier: Modifier = Modifier,
    startMono: Boolean = true,
    endMono: Boolean = true,
) {
    val colors = HelixTheme.colors
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        HelixMiniField(startLabel, startValue, Modifier.weight(1f), startMono)
        Box(
            Modifier
                .width(Dimens.hairline)
                .fillMaxHeight()
                .background(colors.hairline),
        )
        HelixMiniField(endLabel, endValue, Modifier.weight(1f), endMono)
    }
}
