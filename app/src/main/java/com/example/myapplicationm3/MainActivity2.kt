package com.example.myapplicationm3

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.myapplicationm3.media3.common.DownloadUtil
import com.example.myapplicationm3.media3.common.MediaItemTag
import com.example.myapplicationm3.ui.theme.MyApplicationM3Theme

@UnstableApi
class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationM3Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnlineScreen()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationM3Theme {
        Greeting("Android")
    }
}

@UnstableApi
@Composable
fun OnlineScreen() {
    val url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"

    val showLoading = remember {
        mutableStateOf(false)
    }

    val mediaItem= MediaItem.Builder()
        .setUri(url)
        .setMimeType(MimeTypes.APPLICATION_M3U8)
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

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(10.dp))

        AndroidView(
            modifier = Modifier
                .height(LocalConfiguration.current.screenHeightDp.dp * 0.75f)
                .fillMaxWidth(),
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            }
        )



        Button(
            onClick = {
                extracted(showLoading, context, mediaItem, exoPlayer)


            }
        ) {
            Text(text = "Download")
        }

    }
    if (showLoading.value){
        CircularProgressIndicator()
    }


    DisposableEffect(Unit){
        onDispose { exoPlayer.release() }
    }

}


private fun extracted(
    showLoading: MutableState<Boolean>,
    context: Context,
    mediaItem: MediaItem,
    exoPlayer: ExoPlayer
) {
    showLoading.value = true

    try {


        if (DownloadUtil.getDownloadTracker(context).isDownloaded(mediaItem)) {
            Toast.makeText(context, "Already Downloaded", Toast.LENGTH_SHORT).show()
            showLoading.value = false
        } else {
            val item = mediaItem.buildUpon()
                .setTag(
                    (mediaItem.localConfiguration?.tag as MediaItemTag)
                        .copy(duration = exoPlayer.duration)
                )
                .build()

            showLoading.value = false

            if (!DownloadUtil.getDownloadTracker(context)
                    .hasDownload(item.localConfiguration?.uri)
            ) {
                showLoading.value = false
                DownloadUtil.getDownloadTracker(context)
                    .toggleDownloadDialogHelper(context, item)
                Toast.makeText(context, "Downloading....", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, e.message.toString(), Toast.LENGTH_SHORT).show()
    }
    showLoading.value = false
}