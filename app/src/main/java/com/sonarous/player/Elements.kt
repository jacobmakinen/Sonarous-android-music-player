package com.sonarous.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonarous.player.components.PlayerViewModel
import com.sonarous.player.ui.theme.shareTechFont

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
fun Text(text: String, modifier: Modifier = Modifier, viewModel: PlayerViewModel) {
    Text(
        modifier = modifier,
        text = if (text.length > 25) {
            "${text.removeRange(26 until text.length)}..."
        } else {
            text
        },
        color = viewModel.textColor,
        fontSize = 15.sp,
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
    lineHeight: TextUnit = 17.sp
) {
    Text(
        modifier = modifier,
        text = text,
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
fun AlbumScreenText(
    text: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel
) {
    Text(
        modifier = modifier,
        text = text,
        color = viewModel.textColor,
        fontSize = 15.sp,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        lineHeight = 15.sp
    )
}

@Composable
fun MyTextField(value: MutableState<String>) {
    BasicTextField(
        value = value.value,
        onValueChange = {
            value.value = it
        },
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