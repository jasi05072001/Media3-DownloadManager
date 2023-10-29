@file:OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)

package com.example.myapplicationm3.screens

import android.net.Uri
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.example.myapplicationm3.R
import com.example.mylibrary.common.utils.DownloadUtil
import com.example.mylibrary.common.utils.MediaItemTag
import kotlinx.coroutines.ExperimentalCoroutinesApi

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

@ExperimentalCoroutinesApi
@OptIn(ExperimentalStdlibApi::class)
@UnstableApi
@Composable
fun SubTitlesPlayer() {

    // this url contains subtitles and it is MKV file so the mime type is "video/x-matroska"

    val url = "https://www.4sync.com/web/directDownload/ROGREMlQ/nAXet_ZV.c002de9de273dae17b829f1f7370ce1f"


//    val mediaItem= MediaItem.Builder()
//        .setUri(url)
//        .setMimeType("video/x-matroska")
//        .setMediaMetadata(
//            MediaMetadata.Builder().setTitle("Meta data").build()
//        )
//
//        .build()

    val uri = "https://storage.googleapis.com/wvmedia/cenc/hevc/tears/tears.mpd"
    val licenceUrl = "https://proxy.uat.widevine.com/proxy?video_id=2015_tears&provider=widevine_test"

    val mediaItem =   MediaItem.Builder()
        .setUri(uri)
        .setMimeType(MimeTypes.APPLICATION_MPD)
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle("Meta data").build()
        )
        .setDrmConfiguration(
            DrmConfiguration.Builder(C.WIDEVINE_UUID)
                .setLicenseUri(Uri.parse(licenceUrl))
                .setMultiSession(true)
                .build()
        )
        .setTag(MediaItemTag(-1, "Meta data"))
        .build()

    val isBtnEnabled = remember {
        mutableStateOf(false)
    }

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

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY){
                        isBtnEnabled.value = true
                    }
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
                enabled = isBtnEnabled.value,
                onClick = {
                    download(context, mediaItem, exoPlayer)
                }
            ) {
                Text(text = "Download")
            }
            Button(
                enabled = isBtnEnabled.value,
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
                enabled = isBtnEnabled.value,
                onClick = {
                    DownloadUtil.getDownloadTracker(context)
                        .resumeDownload(mediaItem.localConfiguration?.uri)
                }
            ) {
                Text(text = "Resume")
            }

            Button(
                enabled = isBtnEnabled.value,
                onClick = {
                    DownloadUtil.getDownloadTracker(context)
                        .removeDownload(mediaItem.localConfiguration?.uri)
                }
            ) {
                Text(text = "Remove")
            }
        }
    }
    Spacer(modifier = Modifier.height(15.dp))


    DisposableEffect(Unit){
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

}

