package com.sonarous.player.screens

import androidx.compose.animation.core.EaseOutSine
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.sonarous.player.components.PlayerViewModel
import com.sonarous.player.ui.theme.dotoFamily
import kotlinx.coroutines.delay

@Composable
fun BasicLoadingScreen(viewModel: PlayerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(viewModel.backgroundColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var loadingProgressRange by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            loadingProgressRange = 60
        }
        val loadingProgress by animateIntAsState(
            targetValue = loadingProgressRange,
            TweenSpec(
                durationMillis = 800,
                easing = EaseOutSine
            )
        )
        Row(
            modifier = Modifier
                .height(250.dp)
                .width(50.dp)
                .graphicsLayer(
                    rotationZ = 90f
                ),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start // Start
        ) {
            DotoText(
                text = loadingTextLevelBuilder(0..loadingProgress),
                modifier = Modifier,
                viewModel = viewModel
            )
        }
        LaunchedEffect(Unit) {
            delay(600)
            viewModel.loadingFinished = true
        }
    }
}
fun loadingTextLevelBuilder(n: IntRange): String {
    var tempText = ""
    for (i in n) {
        tempText += "________\n"
    }
    return tempText
}
@Composable
fun DotoText(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = text,
        fontFamily = dotoFamily,
        fontWeight = FontWeight.W600,
        fontSize = 8.sp,
        color = viewModel.eqTextColor,
        style = TextStyle(
            shadow = Shadow(
                color = viewModel.eqTextColor.copy(alpha = 0.8f),
                offset = Offset(0f,0f),
                blurRadius = 20f
            )
        ),
        lineHeight = 4.sp
    )
}

fun changeBrightness(color: Int, factor: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color, hsl)
    hsl[2] *= factor
    return Color(ColorUtils.HSLToColor(hsl))
}