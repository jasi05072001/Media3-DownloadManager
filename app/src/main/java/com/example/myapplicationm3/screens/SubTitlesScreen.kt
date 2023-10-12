package com.example.myapplicationm3.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.example.myapplicationm3.R
import com.example.mylibrary.common.utils.DownloadUtil
import com.example.mylibrary.common.utils.MediaItemTag

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun SubTitlesScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = " Video with Subtitles Player/Downloader"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(
                    top = paddingValues.calculateTopPadding()
                )
                .fillMaxSize()
        ) {
            SubTitlesPlayer()

        }
    }

}

@UnstableApi
@Composable
fun SubTitlesPlayer() {

    // this url contains subtitles and it is MKV file so the mime type is "video/x-matroska"

    val url = "https://www.4sync.com/web/directDownload/ROGREMlQ/nAXet_ZV.c002de9de273dae17b829f1f7370ce1f"


    val mediaItem= MediaItem.Builder()
        .setUri(url)
        .setMimeType("video/x-matroska")
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle("Meta data").build()
        )
        .setTag(MediaItemTag(-1, "Meta data"))
        .build()


    val context = LocalContext.current


    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            setMediaItem(mediaItem)
            prepare()
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                }
                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    Toast.makeText(context, mediaMetadata.title, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

        AndroidView(
            modifier = Modifier
                .height(LocalConfiguration.current.screenHeightDp.dp * 0.40f)
                .fillMaxWidth(),
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setShowSubtitleButton(true)
                    setBackgroundColor(context.getColor(R.color.transparent))

                }
            }
        )

        Spacer(modifier = Modifier.height(25.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Button(
                onClick = {
                    download(context, mediaItem, exoPlayer)
                }
            ) {
                Text(text = "Download")
            }
            Button(
                onClick = {
                    DownloadUtil.getDownloadTracker(context)
                        .pauseDownload(mediaItem.localConfiguration?.uri)
                }
            ) {
                Text(text = "Pause")
            }
        }
        Spacer(modifier = Modifier.height(15.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    DownloadUtil.getDownloadTracker(context)
                        .resumeDownload(mediaItem.localConfiguration?.uri)
                }
            ) {
                Text(text = "Resume")
            }

            Button(
                onClick = {
                    DownloadUtil.getDownloadTracker(context)
                        .removeDownload(mediaItem.localConfiguration?.uri)
                }
            ) {
                Text(text = "Remove")
            }
        }
//            Spacer(modifier = Modifier.height(10.dp))
//            Button(modifier = Modifier
//                .padding(horizontal = 15.dp)
//                .fillMaxWidth(),
//                onClick = {
//
//                    val list = listOf(240, 480, 720, 1080)
//                    val a = 620
//
//                    // Find the closest element in the list to 'a'
//                    val closestElement = list.minByOrNull { abs(it - a) }
//
//                    if (closestElement != null) {
//                        val result = "Closest element to $a is $closestElement"
//                        println(result)
//                    } else {
//                        println("The list is empty")
//                    }
//
//                }) {
//                Text(text = "Closest")
//            }
    }
    Spacer(modifier = Modifier.height(15.dp))


    DisposableEffect(Unit){
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

}


private fun download(
    context: Context,
    mediaItem: MediaItem,
    exoPlayer: ExoPlayer
) {
    try {
        if (DownloadUtil.getDownloadTracker(context).isDownloaded(mediaItem)) {
            Toast.makeText(context, "Already Downloaded", Toast.LENGTH_SHORT).show()
        } else {
            val item = mediaItem.buildUpon()
                .setTag(
                    (mediaItem.localConfiguration?.tag as MediaItemTag)
                        .copy(duration = exoPlayer.duration)
                )
                .build()

            if (!DownloadUtil.getDownloadTracker(context)
                    .hasDownload(item.localConfiguration?.uri)
            ) {
                DownloadUtil.getDownloadTracker(context)
                    .toggleDownloadDialogHelper(context, item)
                Toast.makeText(context, "Downloading....", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, e.message.toString(), Toast.LENGTH_SHORT).show()
    }
}