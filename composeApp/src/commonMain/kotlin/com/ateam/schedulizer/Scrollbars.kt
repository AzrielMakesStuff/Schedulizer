package com.ateam.schedulizer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Drag handle for a [LazyListState]. Only shown when the list can actually scroll. */
@Suppress("FrequentlyChangingValue")
@Composable
fun LazyListScrollbar(state: LazyListState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val info = state.layoutInfo
    val visible = info.visibleItemsInfo
    val total = info.totalItemsCount
    val viewport = info.viewportSize.height.toFloat()

    // Estimate the full content height in pixels from the items currently on screen.
    // Works even when one item (the week grid) is far taller than the rest.
    val visibleSum = visible.sumOf { it.size }.toFloat()
    val avg = if (visible.isNotEmpty()) visibleSum / visible.size else 0f
    val estTotal = visibleSum + (total - visible.size).coerceAtLeast(0) * avg
    val scrollable = (estTotal - viewport).coerceAtLeast(1f)
    val scrolled = state.firstVisibleItemIndex * avg + state.firstVisibleItemScrollOffset

    val progress = when {
        !state.canScrollBackward -> 0f
        !state.canScrollForward -> 1f
        else -> (scrolled / scrollable).coerceIn(0f, 1f)
    }

    ScrollbarTrack(
        visible = (state.canScrollForward || state.canScrollBackward) && viewport > 0f && estTotal > 0f,
        progress = progress,
        thumbFraction = { if (estTotal > 0f) viewport / estTotal else 1f },
        onDragFraction = { f -> scope.launch { state.scrollBy(f * scrollable) } },
        modifier = modifier
    )
}

/** Drag handle for a [ScrollState] (verticalScroll). Only shown when content overflows. */
@Suppress("FrequentlyChangingValue")
@Composable
fun ScrollStateScrollbar(state: ScrollState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val max = state.maxValue
    val overflow = max > 0 && max != Int.MAX_VALUE

    ScrollbarTrack(
        visible = overflow,
        progress = if (overflow) state.value / max.toFloat() else 0f,
        thumbFraction = { trackPx -> trackPx / (trackPx + max) },
        onDragFraction = { f -> scope.launch { state.scrollBy(f * max) } },
        modifier = modifier
    )
}

@Composable
private fun ScrollbarTrack(
    visible: Boolean,
    progress: Float,
    thumbFraction: (trackPx: Float) -> Float,
    onDragFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.primary
    var dragging by remember { mutableStateOf(false) }
    val currentOnDrag by rememberUpdatedState(onDragFraction)

    BoxWithConstraints(modifier = modifier) {
        val trackPx = with(density) { maxHeight.toPx() }
        val minThumbPx = with(density) { 48.dp.toPx() }
        val thumbPx = (trackPx * thumbFraction(trackPx)).coerceIn(minThumbPx, trackPx)
        val rangePx = (trackPx - thumbPx).coerceAtLeast(1f)

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, (progress * rangePx).roundToInt()) }
                .width(28.dp)
                .height(with(density) { thumbPx.toDp() })
                .pointerInput(rangePx) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            currentOnDrag(amount / rangePx)
                        }
                    )
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 2.dp)
                    .width(if (dragging) 10.dp else 4.dp)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = if (dragging) 0.9f else 0.5f))
            )
        }
    }
}