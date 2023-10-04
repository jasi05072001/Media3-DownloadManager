package com.example.myapplicationm3.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplicationm3.items.ColItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {

    val items =  listOf(
        ColItems(
            title = "Hls Video Player/Downloader",
            onclick = {
                navController.navigate(Screens.HlsPlayerScreen.route)
            }
        ),
        ColItems(
            title = " Mp4 Video with subtitles Player/Downloader",
            onclick = {
                navController.navigate(Screens.SubtitleVideoScreen.route)
            }
        ),
        ColItems(
            title = "Offline Video Player/Downloader",
            onclick = {
                navController.navigate(Screens.OfflinePlayerScreen.route)
            }
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Download manager"
                    )
                },
            )
        }
    ) {paddingValues->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = paddingValues.calculateTopPadding(), horizontal = 10.dp)
        ) {
            Text(
                text = "Select an option to explore -> ",
                fontSize = 20.sp,
                fontWeight = FontWeight.W500,
                modifier = Modifier.padding(vertical = 15.dp)
            )
            Divider()

            items.forEach {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            it.onclick.invoke()
                        }
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = it.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W400
                    )
                }
                Divider()
            }
        }


    }


}