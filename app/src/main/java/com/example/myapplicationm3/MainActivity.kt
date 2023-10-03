package com.example.myapplicationm3

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.myapplicationm3.databinding.ActivityMainBinding
import com.example.mylibrary.common.utils.DownloadUtil
import com.example.mylibrary.common.utils.MediaItemTag


@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : AppCompatActivity() {

    val url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"

    private lateinit var binding: ActivityMainBinding


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

    private val mediaItem =
        MediaItem.Builder()
            .setUri(url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle("Meta data").build()
            )
            .setTag(MediaItemTag(-1, "Meta data"))
            .build()




    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun  onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContentView(binding.root)

        val  exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(DownloadUtil.getReadOnlyDataSourceFactory(this))
            ).build()
            .apply {
                playWhenReady = true
                setMediaItem(mediaItem)
                prepare()
            }

        binding.player.player = exoPlayer

        binding.downloadState. setOnClickListener {
            if(DownloadUtil.getDownloadTracker(this).isDownloaded(mediaItem)) {
                Toast.makeText(
                    this,
                    "You've already downloaded the video",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val item = mediaItem.buildUpon()
                    .setTag((mediaItem.localConfiguration?.tag as MediaItemTag)
                        .copy(duration = exoPlayer.duration))
                    .build()
                Log.d("Duration", exoPlayer.duration.toString())

                if(!DownloadUtil.getDownloadTracker(this@MainActivity)
                        .hasDownload(item.localConfiguration?.uri)
                ) {
                    DownloadUtil.getDownloadTracker(this@MainActivity)
                        .toggleDownloadDialogHelper(this@MainActivity, item)
                }
            }
        }
    }
}