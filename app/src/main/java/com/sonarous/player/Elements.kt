package com.sonarous.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.sonarous.player.components.PlayerViewModel
import com.sonarous.player.ui.theme.shareTechFont
import kotlinx.coroutines.launch

@Composable
fun BackButtonRow(viewModel: PlayerViewModel, height: Dp = 45.dp, text: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(viewModel.backgroundColor),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            content = {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = "Back arrow"
                )
            },
            onClick = { onClick() },
            colors = IconButtonColors(
                contentColor = viewModel.iconColor,
                containerColor = Color.Transparent,
                disabledContentColor = viewModel.iconColor,
                disabledContainerColor = Color.Transparent
            )
        )
        if (text != null) {
            Spacer(modifier = Modifier.width(5.dp))
            LargeText(text, viewModel = viewModel)
        }
    }
}

@Composable
fun Text(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel, maxTextLength: Int = 20) {
    Text(
        modifier = modifier,
        text = if (text.length > maxTextLength) {
            "${text.removeRange(maxTextLength + 1 until text.length)}..."
        } else {
            text
        },
        color = viewModel.textColor,
        fontSize = 14.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        lineHeight = 4.sp,
    )
}

@Composable
fun LargeText(
    text: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel,
    lineHeight: TextUnit = 17.sp,
    maxLength: Int? = null
) {
    Text(
        modifier = modifier,
        text = if (maxLength == null) text else if (text.length > maxLength) "${text.substring(0, maxLength)}..." else text,
        color = viewModel.textColor,
        fontSize = 17.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        lineHeight = lineHeight
    )
}

@Composable
fun PlayerLargeText(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = if (text.length > 31) {
            "${text.removeRange(32 until text.length)}..."
        } else {
            text
        },
        color = viewModel.textColor,
        fontSize = 25.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun PlayerText(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = if (text.length > 31) {
            "${text.removeRange(32 until text.length)}..."
        } else {
            text
        },
        color = viewModel.textColor,
        fontSize = 20.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun AlbumScreenText(text: String, viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        color = viewModel.textColor,
        fontSize = 15.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        lineHeight = 15.sp,
        overflow = TextOverflow.Clip,
    )
}

@Composable
fun MiscText(text: String, fontSize: TextUnit, viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        color = viewModel.textColor,
        fontSize = fontSize,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun ScrollBar(columnState: LazyListState, viewModel: PlayerViewModel, lazyColumnSize: Float, itemsPerViewport: Float = 9f) {
    var scrollBarHeight by remember { mutableFloatStateOf(0f) }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .onGloballyPositioned { coordinates ->
                scrollBarHeight = coordinates.size.height.toFloat()
            },
    ) {
        val scope = rememberCoroutineScope()
        val tabOffset = remember {
            derivedStateOf {
                if (lazyColumnSize <= itemsPerViewport) {
                    0f
                } else {
                    // (Percentage of lazy list covered + percentage of offset) * scrollBarHeight
                    val overallPercentageScroll = columnState.firstVisibleItemIndex.toFloat() / columnState.layoutInfo.totalItemsCount.toFloat()
                    val offsetPercentageScroll = columnState.firstVisibleItemScrollOffset.dp.value / (columnState.layoutInfo.viewportSize.height.toFloat() * (columnState.layoutInfo.totalItemsCount.toFloat() / itemsPerViewport))
                    overallPercentageScroll * scrollBarHeight + offsetPercentageScroll * scrollBarHeight
                }
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { pointerChange, _ ->
                        val yDelta = pointerChange.position.y
                        scope.launch {
                            columnState.scrollBy(
                                // Proportion of change in position * total lazy column size in px
                                (yDelta - tabOffset.value) / scrollBarHeight * (columnState.layoutInfo.viewportSize.height.toFloat() * (columnState.layoutInfo.totalItemsCount.toFloat() / itemsPerViewport))
                            )
                        }
                    }
                }
        ) {
            val tabHeight = if (lazyColumnSize <= itemsPerViewport) {
                scrollBarHeight
            } else {
                itemsPerViewport / lazyColumnSize * scrollBarHeight
            }
            drawRoundRect(
                topLeft = Offset(0f,tabOffset.value.coerceIn(0f, scrollBarHeight - tabHeight)),
                color = viewModel.backgroundColor.increaseBrightness(0.1f),
                size = Size(30f, tabHeight - 10f),
                cornerRadius = CornerRadius(30f, 30f),
            )
        }
    }
}

fun Color.increaseBrightness(brightness: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[2] += brightness
    return Color(ColorUtils.HSLToColor(hsl))
}