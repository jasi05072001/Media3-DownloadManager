package com.example.myapplicationm3

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.myapplicationm3.ui.theme.MyApplicationM3Theme
import com.example.mylibrary.common.utils.DownloadUtil
import com.example.mylibrary.common.utils.MediaItemTag
import kotlin.math.abs

@UnstableApi
class MainActivity2 : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    private val requestPermissionLauncher =
        this.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(
                    this,
                    "Permission granted!",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                Toast.makeText(
                    this,
                    "Permission not granted!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MyApplicationM3Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnlineScreen()
//                    SortedMap()
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
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShowSubtitleButton(true)

                }
            }
        )


        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Button(
                onClick = {
                    extracted( context, mediaItem, exoPlayer)
                }
            ) {
                Text(text = "Download")
            }
            Button(
                onClick = {
                    DownloadUtil.getDownloadTracker(context).pauseDownload(mediaItem.localConfiguration?.uri)
                }
            ) {
                Text(text = "Pause")
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    DownloadUtil.getDownloadTracker(context).resumeDownload(mediaItem.localConfiguration?.uri)
                }
            ) {
                Text(text = "Resume")
            }

            Button(
                onClick = {
                    DownloadUtil.getDownloadTracker(context).removeDownload(mediaItem.localConfiguration?.uri)
                }
            ) {
                Text(text = "Remove")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(modifier = Modifier
            .padding(horizontal = 15.dp)
            .fillMaxWidth(),
            onClick = {

                val list = listOf(240, 480, 720, 1080)
                val a = 620

// Find the closest element in the list to 'a'
                val closestElement = list.minByOrNull { abs(it - a) }

                if (closestElement != null) {
                    val result = "Closest element to $a is $closestElement"
                    println(result)
                } else {
                    println("The list is empty")
                }

            }) {
            Text(text= "Closest")
        }
    }

    DisposableEffect(Unit){
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

}


private fun extracted(
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
