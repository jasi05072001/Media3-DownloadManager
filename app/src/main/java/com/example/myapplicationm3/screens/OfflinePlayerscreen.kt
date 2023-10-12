package com.example.myapplicationm3.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.myapplicationm3.R
import com.example.mylibrary.common.utils.DownloadUtil


@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun  OfflinePlayerScreen() {
    Column {
        OfflinePlayer()
    }

}

@UnstableApi
@Composable
fun OfflinePlayer() {
    val context = LocalContext.current


    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            setMediaSource(DownloadUtil.returnCachedMediaSource(context))
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
    }
    Spacer(modifier = Modifier.height(15.dp))


    DisposableEffect(Unit){
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }
}