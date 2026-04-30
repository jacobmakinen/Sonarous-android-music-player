package com.sonarous.player

import androidx.annotation.OptIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.sonarous.player.components.PlayerService
import com.sonarous.player.components.PlayerViewModel
import com.sonarous.player.ui.theme.dotoFamily
import com.sonarous.player.ui.theme.orbitronFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun SpectrumAnalyzer(
    spectrumAnalyzer: PlayerService.SpectrumAnalyzer,
    viewModel: PlayerViewModel
) {
    val scope = rememberCoroutineScope()
    var eqList by remember { mutableStateOf(doubleArrayOf()) }
    var volume by remember { mutableDoubleStateOf(0.0) }

    // Collecting flow data from spectrum analyzer
    val stateFlowData = spectrumAnalyzer.eqStateFlow.collectAsState()
    remember(stateFlowData.value) {
        scope.launch {
            delay(1850)
            eqList = stateFlowData.value.visualiserList
            volume = stateFlowData.value.volume
        }
    }
    Row(
        modifier = Modifier
            .size(340.dp, 140.dp)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val fieldName = listOf("63", "16O", "4OO", "1k", "2.5k", "6.3k", "16k")
        VolumeLevelAxis(viewModel)
        //======================== Volume level ========================//
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(35.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy((-2).dp)
            ) {
                val tick = viewModel.currentSongPosition
                if (viewModel.isPlaying) {
                    VolumeLevelText(tick, viewModel, volume)
                    VolumeLevelText(tick, viewModel, volume)
                }
            }
        }
        Spacer(
            modifier = Modifier
                .width(15.dp)
        )
        //======================== Equaliser ========================//
        EQLevelAxis(viewModel)
        for (i in 0..6) { // 7 band EQ
            EQLevelColumn(fieldName[i], viewModel, eqList)
        }
    }
}

@Composable
fun VolumeLevelAxis(viewModel: PlayerViewModel) {
    val colorAlpha = 0.65f
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(11.5.dp)
            .offset(y = 9.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 1.dp),
            text = "2O",
            fontWeight = FontWeight.W300,
            fontFamily = orbitronFamily,
            fontSize = 5.sp,
            color = viewModel.eqTextColor,
            lineHeight = 2.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = viewModel.eqTextColor.copy(alpha = colorAlpha),
                    offset = Offset(0f, 0f),
                    blurRadius = 20f
                )
            )
        )
        for (i in 1..4) {
            VolumeLevelTick(viewModel)
        }
        Text(
            modifier = Modifier
                .offset(y = 2.dp)
                .padding(horizontal = 1.dp),
            text = "1O",
            fontWeight = FontWeight.W300,
            fontFamily = orbitronFamily,
            fontSize = 5.sp,
            color = viewModel.eqTextColor,
            lineHeight = 2.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = viewModel.eqTextColor.copy(alpha = 0.6f),
                    offset = Offset(0f, 0f),
                    blurRadius = 20f
                )
            )
        )
        for (i in 1..4) {
            VolumeLevelTick(viewModel)
        }
        Text(
            modifier = Modifier
                .offset(y = 2.dp)
                .padding(horizontal = 1.dp),
            text = "O",
            fontFamily = orbitronFamily,
            fontWeight = FontWeight.W300,
            fontSize = 5.sp,
            color = viewModel.eqTextColor,
            lineHeight = 10.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = viewModel.eqTextColor.copy(alpha = colorAlpha),
                    offset = Offset(0f, 0f),
                    blurRadius = 20f
                )
            )
        )
    }
    Canvas(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .offset(y = 15.dp)
            .shadow(
                shape = RectangleShape,
                elevation = 2.dp,
                ambientColor = viewModel.eqTextColor.copy(alpha = 0.8f)
            )
    ) {
        drawRect(
            color = viewModel.eqTextColor,
            size = Size(width = 1f, height = 131.dp.toPx()),
        )
    }
}

@Composable
fun EQLevelAxis(viewModel: PlayerViewModel) {
    Column( // Arbitrary measure dashes
        modifier = Modifier
            .fillMaxHeight()
            .width(7.dp)
            .offset(y = 9.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 1..8) {
            VolumeLevelTick(viewModel)
        }
    }
    Canvas(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .offset(y = 16.dp)
            .shadow(
                shape = RectangleShape,
                elevation = 2.dp,
                ambientColor = viewModel.eqTextColor.copy(alpha = 0.8f)
            )
    ) {
        drawRect(
            color = viewModel.eqTextColor,
            size = Size(width = 1f, height = 130.65.dp.toPx()),
        )
    }
}

@Composable
fun VolumeLevelTick(viewModel: PlayerViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy((-2).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..3) {
                Text(
                    modifier = Modifier,
                    text = "_",
                    fontWeight = FontWeight.W300,
                    fontSize = 7.sp,
                    color = viewModel.eqTextColor,
                    lineHeight = 10.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = viewModel.eqTextColor.copy(alpha = 0.8f),
                            offset = Offset(0f, 0f),
                            blurRadius = 8f
                        )
                    )
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun EQLevelColumn(fieldName: String, viewModel: PlayerViewModel, eqList: DoubleArray) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(35.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OrbitronText(
            fieldName,
            Modifier,
            viewModel
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy((-2).dp)
        ) {
            val tick = viewModel.currentSongPosition
            if (viewModel.isPlaying) {
                EQLevelText(fieldName, tick, viewModel, eqList)
                EQLevelText(fieldName, tick, viewModel, eqList)
            }
        }
    }
}

//============================== EQ level ==============================//
@OptIn(UnstableApi::class)
@Composable
fun EQLevelText(fieldName: String, tick: Float, viewModel: PlayerViewModel, eqList: DoubleArray) {
    val eqTransition = rememberInfiniteTransition()

    val target = remember(tick) {
        if (eqList.count() != 0) {
            when (fieldName) {
                "63" -> level63(eqList[0])
                "16O" -> level160(eqList[1])
                "4OO" -> level400(eqList[2])
                "1k" -> level1k(eqList[3])
                "2.5k" -> level2500k(eqList[4])
                "6.3k" -> level6300k(eqList[5])
                "16k" -> level16k(eqList[6])
                else -> 0f
            }
        } else {
            0f
        }
    }
    val levels by eqTransition.animateFloat(
        initialValue = 0f,
        targetValue = target,
        animationSpec = infiniteRepeatable(
            tween(
                10,
                0,
                EaseOut
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    Text(
        modifier = Modifier,
        text = textLevelBuilder(1..levels.toInt()),
        fontFamily = dotoFamily,
        fontWeight = FontWeight.W100,
        fontSize = 23.sp,
        color = viewModel.eqLevelColor,
        letterSpacing = 0.sp,
        lineHeight = 3.sp,
        textAlign = TextAlign.Center,
    )
}

//============================== Volume level ==============================//
@OptIn(UnstableApi::class)
@Composable
fun VolumeLevelText(
    tick: Float,
    viewModel: PlayerViewModel,
    volumeLevel: Double
) {
    val eqTransition = rememberInfiniteTransition()
    val target = remember(tick) {
        volumeLevel(volumeLevel)
    }
    val levels by eqTransition.animateFloat(
        initialValue = 0f,
        targetValue = target,
        animationSpec = infiniteRepeatable(
            tween(
                10,
                0,
                EaseOut
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    Text(
        modifier = Modifier,
        text = textLevelBuilder(1..levels.toInt()),
        fontFamily = dotoFamily,
        fontWeight = FontWeight.W100,
        fontSize = 23.sp,
        color = viewModel.eqLevelColor,
        letterSpacing = 0.sp,
        lineHeight = 3.sp,
        textAlign = TextAlign.Center
    )
}

@OptIn(UnstableApi::class)
fun volumeLevel(volume: Double): Float {
    var tmpSound = volume

    if (tmpSound > 20000.0) {
        tmpSound = 20000.0
    }
    return when {
        tmpSound <= 2244 -> 2f
        tmpSound <= 2512 -> 4f
        tmpSound <= 2818 -> 6f
        tmpSound <= 3162 -> 8f
        tmpSound <= 3548 -> 10f
        tmpSound <= 3981 -> 12f
        tmpSound <= 4467 -> 14f
        tmpSound <= 5012 -> 16f
        tmpSound <= 5623 -> 18f
        tmpSound <= 6325 -> 20f
        tmpSound <= 7096 -> 22f
        tmpSound <= 7943 -> 24f
        tmpSound <= 8913 -> 26f
        tmpSound <= 10000 -> 28f
        tmpSound <= 11220 -> 30f
        tmpSound <= 12589 -> 32f
        tmpSound <= 14125 -> 34f
        tmpSound <= 15849 -> 36f
        tmpSound <= 17783 -> 38f
        tmpSound <= 20000 -> 40f
        else -> 0f
    }
}

@OptIn(UnstableApi::class)
fun level63(data: Double): Float {
    var tempValue = data
    if (tempValue > 110.0) {
        tempValue = 110.0
    }
    tempValue = tempValue / 5.5 * 2
    return tempValue.toFloat()
}

@OptIn(UnstableApi::class)
fun level160(magnitude: Double): Float {
    var tempValue = magnitude
    if (tempValue > 55.0) {
        tempValue = 55.0
    }
    tempValue = tempValue / 2.75 * 2
    return tempValue.toFloat()
}

@OptIn(UnstableApi::class)
fun level400(magnitude: Double): Float {
    var tempValue = magnitude
    if (tempValue > 40.0) {
        tempValue = 40.0
    }
    tempValue = tempValue / 2.0 * 2
    return tempValue.toFloat()
}

@OptIn(UnstableApi::class)
fun level1k(magnitude: Double): Float {
    var tempValue = magnitude
    if (tempValue > 13.0) {
        tempValue = 13.0
    }
    tempValue = tempValue / 0.65 * 2
    return tempValue.toFloat()
}

@OptIn(UnstableApi::class)
fun level2500k(magnitude: Double): Float {
    var tempValue = magnitude
    if (tempValue > 5.0) {
        tempValue = 5.0
    }
    tempValue = tempValue / 0.25 * 2
    return tempValue.toFloat()
}

@OptIn(UnstableApi::class)
fun level6300k(magnitude: Double): Float {
    var tempValue = magnitude
    if (tempValue > 1.5) {
        tempValue = 1.5
    }
    tempValue = tempValue / 0.075 * 2
    return tempValue.toFloat()
}

@OptIn(UnstableApi::class)
fun level16k(magnitude: Double): Float {
    var tempValue = magnitude
    if (tempValue > 1.5) {
        tempValue = 1.5
    }
    tempValue = tempValue / 0.075 * 2
    return tempValue.toFloat()
}

//============================== Text presets ==============================//
@Composable
fun OrbitronText(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = text,
        fontFamily = orbitronFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        color = viewModel.eqTextColor,
        style = TextStyle(
            shadow = Shadow(
                color = viewModel.eqTextColor.copy(alpha = 0.8f),
                offset = Offset(0f, 0f),
                blurRadius = 20f
            )
        )
    )
}

fun textLevelBuilder(n: IntRange): String {
    var tempText = ""
    for (i in n) {
        tempText += "_\n"
    }
    return tempText
}