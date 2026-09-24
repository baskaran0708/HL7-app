package com.livemedica.helix.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing

/**
 * Renders the non-success states so every screen handles loading, empty, error and offline
 * identically. A screen only supplies its own success content and its own empty copy.
 */
@Composable
fun <T> HelixStateHost(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    loading: @Composable () -> Unit = { HelixListSkeleton() },
    emptyTitle: String = "Nothing here",
    emptyBody: String? = null,
    success: @Composable (T) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is UiState.Loading -> loading()

            is UiState.Success -> success(state.data)

            is UiState.Empty -> HelixEmptyState(
                icon = Icons.Outlined.Inbox,
                title = emptyTitle,
                body = state.message ?: emptyBody,
            )

            is UiState.Error -> HelixErrorState(
                title = "Something went wrong",
                // A repository that has nothing specific to add returns the generic message; echoing
                // it under an identical heading just says the same sentence twice.
                body = state.message.takeIf { it != GENERIC_ERROR_TITLE },
                onRetry = onRetry,
            )

            is UiState.Offline -> {
                val cached = state.cached
                if (cached != null) {
                    Column(Modifier.fillMaxSize()) {
                        HelixOfflineBanner()
                        success(cached)
                    }
                } else {
                    HelixErrorState(
                        icon = Icons.Outlined.CloudOff,
                        title = "You're offline",
                        body = "Showing your last synchronised data once a connection returns.",
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

const val GENERIC_ERROR_TITLE = "Something went wrong"

/** Persistent, non-blocking marker that the data on screen is stale. */
@Composable
fun HelixOfflineBanner(modifier: Modifier = Modifier) {
    val colors = HelixTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.warning.copy(alpha = 0.12f))
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Text(
            text = "You're offline · showing your last synchronised data",
            style = HelixTheme.typography.caption,
            color = colors.warning,
        )
    }
}

@Composable
fun HelixEmptyState(
    icon: ImageVector,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.textMute, modifier = Modifier.size(32.dp))
        Text(
            text = title,
            style = HelixTheme.typography.headline,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.md),
        )
        if (body != null) {
            Text(
                text = body,
                style = HelixTheme.typography.body,
                color = colors.textMute,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
fun HelixErrorState(
    title: String,
    body: String?,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.ErrorOutline,
) {
    val colors = HelixTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.critical, modifier = Modifier.size(32.dp))
        Text(
            text = title,
            style = HelixTheme.typography.headline,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.md),
        )
        if (body != null) {
            Text(
                text = body,
                style = HelixTheme.typography.body,
                color = colors.textMute,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
        if (onRetry != null) {
            TextButton(onClick = onRetry, modifier = Modifier.padding(top = Spacing.sm)) {
                Text("Try again", style = HelixTheme.typography.label, color = colors.primary)
            }
        }
    }
}

/**
 * Loading skeleton. A slow opacity pulse rather than a shimmer sweep — the design keeps motion
 * minimal, and a gentle pulse is far less distracting to read past on a dense clinical list.
 */
@Composable
fun HelixSkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    cornerRadius: Dp = Radius.sm,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .alpha(alpha)
            .background(HelixTheme.colors.fill),
    )
}

@Composable
fun HelixListSkeleton(modifier: Modifier = Modifier, rows: Int = 5) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        repeat(rows) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(vertical = Spacing.sm),
            ) {
                HelixSkeletonBlock(Modifier.fillMaxWidth(0.35f), height = 10.dp)
                HelixSkeletonBlock(Modifier.fillMaxWidth(0.75f), height = 16.dp)
                HelixSkeletonBlock(Modifier.fillMaxWidth(0.55f), height = 12.dp)
            }
        }
    }
}
