package com.sonarous.player.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sonarous.player.BackButtonRow
import com.sonarous.player.components.PlayerViewModel
import com.sonarous.player.R
import com.sonarous.player.ui.theme.shareTechFont

@Composable
fun InfoScreen(viewModel: PlayerViewModel, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(viewModel.backgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 5.dp)
    ) {
        BackButtonRow(viewModel, navController, "Info")
        OpenSourceInfo(viewModel)
        InfoText(
            text = "Help:",
            viewModel = viewModel,
            fontSize = 19.sp
        )
        InfoText(
            text = "Can't see your music?\nMake sure it's in your phone's \"Music\" folder",
            viewModel = viewModel
        )
    }
}

@Composable
fun OpenSourceInfo(viewModel: PlayerViewModel) {
    var expandedOpenSourceInfo by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expandedOpenSourceInfo = !expandedOpenSourceInfo
            },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        InfoText(
            "Licences for open source libraries used:\n",
            viewModel = viewModel,
            fontSize = 19.sp
        )
        if (expandedOpenSourceInfo) {
            Icon(
                painterResource(R.drawable.drop_up),
                "Drop up",
                tint = viewModel.iconColor
            )
        } else {
            Icon(
                painterResource(R.drawable.drop_down),
                "Drop down",
                tint = viewModel.iconColor
            )
        }
    }
    if (expandedOpenSourceInfo) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (expandedOpenSourceInfo) {
                item {
                    InfoText(
                        text = "AndroidX, kotlinx.serialization, color-picker compose, material-components-android\n\n" +
                                """
                        Licensed under the Apache License, Version 2.0 (the "License");
                        you may not use this file except in compliance with the License.
                        You may obtain a copy of the License at

                            http://www.apache.org/licenses/LICENSE-2.0

                        Unless required by applicable law or agreed to in writing, software
                        distributed under the License is distributed on an "AS IS" BASIS,
                        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                        See the License for the specific language governing permissions and
                        limitations under the License.
                        """.trimIndent(),
                        viewModel = viewModel,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                item {
                    InfoText(
                        """
                        JTransforms
                        Copyright (c) 2007 onward, Piotr Wendykier
                        All rights reserved.

                        Redistribution and use in source and binary forms, with or without
                        modification, are permitted provided that the following conditions are met:

                        1. Redistributions of source code must retain the above copyright notice, this
                           list of conditions and the following disclaimer. 
                        2. Redistributions in binary form must reproduce the above copyright notice,
                           this list of conditions and the following disclaimer in the documentation
                           and/or other materials provided with the distribution.

                        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
                        ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
                        WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
                        DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
                        ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
                        (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
                        LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
                        ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
                        (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
                        SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
                    """.trimIndent(),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun InfoText(
    text: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel,
    lineHeight: TextUnit = 17.sp,
    fontSize: TextUnit = 15.sp
) {
    Text(
        modifier = modifier,
        text = text,
        color = viewModel.textColor,
        fontSize = fontSize,
        fontFamily = shareTechFont,
        fontWeight = FontWeight.Normal,
        lineHeight = lineHeight
    )
}