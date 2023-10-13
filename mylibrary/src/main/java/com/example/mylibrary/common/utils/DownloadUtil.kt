package com.example.mylibrary.common.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.mylibrary.R
import com.example.mylibrary.common.downloadTracker.DownloadTracker
import org.chromium.net.CronetEngine
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
object DownloadUtil {
    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"

    private const val TAG = "DownloadUtil"
    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

    private lateinit var databaseProvider: DatabaseProvider
    private lateinit var downloadCache: Cache
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var httpDataSourceFactory: HttpDataSource.Factory
    private lateinit var downloadNotificationHelper: DownloadNotificationHelper
    private lateinit var downloadDirectory: File
    private lateinit var downloadManager: DownloadManager
    private lateinit var downloadTracker: DownloadTracker


    @Synchronized
    fun getHttpDataSourceFactory(context: Context):HttpDataSource.Factory{
        if (!DownloadUtil::httpDataSourceFactory.isInitialized){
            httpDataSourceFactory = CronetDataSource.Factory(
                CronetEngine.Builder(context).build(),
                Executors.newSingleThreadExecutor()
            )
        }
        return httpDataSourceFactory
    }

    @Synchronized
    fun getReadOnlyDataSourceFactory(context:Context):DataSource.Factory{
        if (!DownloadUtil::dataSourceFactory.isInitialized){
            val appContext = context.applicationContext
            val upstreamFactory = DefaultDataSource.Factory(
                appContext,
                getHttpDataSourceFactory(appContext)
            )
            dataSourceFactory = CacheDataSource.Factory()
                .setCache(getDownloadCache(appContext))
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }
        return dataSourceFactory
    }

    @Synchronized
    private fun getDownloadCache(context: Context): Cache {
        if(!DownloadUtil::downloadCache.isInitialized) {
            val downloadContentDirectory =
                File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
            downloadCache = SimpleCache(
                downloadContentDirectory,
                NoOpCacheEvictor(),
                getDatabaseProvider(context)
            )
        }
        return downloadCache
    }


    @Synchronized
    fun getDownloadDirectory(context: Context): File {
        if(!DownloadUtil::downloadDirectory.isInitialized) {
            downloadDirectory = context.getExternalFilesDir(null) ?: context.filesDir
        }
        return downloadDirectory
    }

    @Synchronized
    fun getDownloadNotificationHelper(context: Context?): DownloadNotificationHelper {
        if(!DownloadUtil::downloadNotificationHelper.isInitialized) {
            downloadNotificationHelper =
                DownloadNotificationHelper(context!!, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        }
        return downloadNotificationHelper
    }

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        ensureDownloadManagerInitialized(context)
        return downloadManager
    }

    fun getDownloadString(context: Context, @Download.State downloadState: Int): String {
        return when (downloadState) {
            Download.STATE_COMPLETED -> context.resources.getString(R.string.exo_download_completed)
            Download.STATE_DOWNLOADING -> context.resources.getString(R.string.exo_download_downloading)
            Download.STATE_FAILED -> context.resources.getString(R.string.exo_download_failed)
            Download.STATE_QUEUED -> context.resources.getString(R.string.exo_download_queued)
            Download.STATE_REMOVING -> context.resources.getString(R.string.exo_download_removing)
            Download.STATE_RESTARTING -> context.resources.getString(R.string.exo_download_restarting)
            Download.STATE_STOPPED -> context.resources.getString(R.string.exo_download_stopped)
            else -> throw IllegalArgumentException()
        }
    }

    @Synchronized
    fun getDownloadTracker(context: Context): DownloadTracker {
        ensureDownloadManagerInitialized(context)
        return downloadTracker
    }

    @Synchronized
    private fun ensureDownloadManagerInitialized(context: Context) {
        if(!DownloadUtil::downloadManager.isInitialized) {
            downloadManager = DownloadManager(
                context,
                getDatabaseProvider(context),
                getDownloadCache(context),
                getHttpDataSourceFactory(context),
                Executors.newFixedThreadPool(6)
            ).apply {
                maxParallelDownloads = 2
            }
            downloadTracker =
                DownloadTracker(context, getHttpDataSourceFactory(context), downloadManager)
        }
    }

    @Synchronized
    private fun getDatabaseProvider(context: Context): DatabaseProvider {
        if(!DownloadUtil::databaseProvider.isInitialized) databaseProvider =
            StandaloneDatabaseProvider(context)
        return databaseProvider
    }

    private fun playDownloadedContent(context: Context): DataSource.Factory{

        val httpDataSourceFactory: DefaultHttpDataSource.Factory = getHttpDataSourceFactory()

        val cacheDataSource : DataSource.Factory = CacheDataSource.Factory()
            .setCache(DownloadTracker.getDownloadCache(context))
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setCacheWriteDataSinkFactory(null)


        return cacheDataSource
    }

    private fun getHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        val httpDataSourceFactory:  DefaultHttpDataSource.Factory?
        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        CookieHandler.setDefault(cookieManager)
        httpDataSourceFactory = DefaultHttpDataSource.Factory()
        return httpDataSourceFactory
    }

    fun returnCachedMediaSource(context: Context): ProgressiveMediaSource {

        val cachedDataSourceFactory = playDownloadedContent(context)

        val url = "https://www.4sync.com/web/directDownload/ROGREMlQ/nAXet_ZV.c002de9de273dae17b829f1f7370ce1f"


        val mediaItem= MediaItem.Builder()
            .setUri(url)
            .setMimeType("video/x-matroska")
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle("Meta data").build()
            )
            .setTag(MediaItemTag(-1, "Meta data"))
            .build()


        val mediaSource = ProgressiveMediaSource.Factory(cachedDataSourceFactory)
            .createMediaSource(
                mediaItem
            )
        return mediaSource
    }

    fun saveQualitySelected(
        context: Context,
        height: Int,
    ) {
        val sharedPreferences = context.getSharedPreferences("quality", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("height", height)
        editor.apply()
    }

    fun getQualitySelected(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences("quality", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("height", 0)
    }


}