package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Elevation
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing

/**
 * The search input.
 *
 * Built from [BasicTextField] rather than an M3 `TextField` so it can match the design exactly — a
 * single filled row with no floating label and no indicator line — while keeping the platform's
 * text editing, selection and IME behaviour intact.
 *
 * [autoFocus] exists for the dedicated Search screen, which has nothing to look at until something
 * is typed and so should arrive with the keyboard already up; an inline search bar over a populated
 * list must not steal focus that way.
 */
@Composable
fun HelixSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = DEFAULT_PLACEHOLDER,
    onSearch: () -> Unit = {},
    onClear: () -> Unit = { onQueryChange("") },
    autoFocus: Boolean = false,
    textStyle: TextStyle = HelixTheme.typography.body,
    elevation: Dp = Elevation.sm,
) {
    val colors = HelixTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    val focusRequester = remember { FocusRequester() }

    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    Row(
        modifier = modifier
            .then(
                if (colors.isDark || elevation <= Elevation.none) Modifier
                else Modifier.shadow(elevation, shape, clip = false),
            )
            .clip(shape)
            .background(if (colors.isDark) colors.elevated else colors.surface)
            .border(Dimens.hairline, colors.hairline, shape)
            .heightIn(min = Dimens.touchTargetMin)
            .padding(start = Spacing.md + 1.dp, end = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 1.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.textMute,
            modifier = Modifier.size(Dimens.iconSm),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                // The placeholder is the field's only label, so it has to be its description too.
                .semantics { contentDescription = placeholder },
            textStyle = textStyle.copy(color = colors.text),
            singleLine = true,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(text = placeholder, style = textStyle, color = colors.textMute)
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = onClear, modifier = Modifier.size(Dimens.touchTargetMin - Spacing.sm)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear search",
                    tint = colors.textMute,
                    modifier = Modifier.size(Dimens.iconSm),
                )
            }
        }
    }
}

private const val DEFAULT_PLACEHOLDER = "Search patient, MRN, accession…"
