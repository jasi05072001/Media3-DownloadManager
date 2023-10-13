package com.example.mylibrary.common.downloadTracker

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Util
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadIndex
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.mylibrary.R
import com.example.mylibrary.common.service.MyDownloadService
import com.example.mylibrary.common.utils.DownloadUtil
import com.example.mylibrary.common.utils.MediaItemTag
import com.example.mylibrary.common.utils.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.abs


private const val TAG = "DownloadTracker"
private const val DEFAULT_BITRATE = 500_000

@SuppressLint("UnsafeOptInUsageError")
class DownloadTracker(
    context: Context,
    private val httpDataSourceFactory: HttpDataSource.Factory,
    private val downloadManager: DownloadManager
) {
    /**
     * Listens for changes in the tracked downloads.
     */
    interface Listener {
        /**
         * Called when the tracked downloads changed.
         */
        fun onDownloadsChanged(download: Download)

    }

    companion object{
        private var downloadCache: Cache? = null
        fun getDownloadCache(context: Context): Cache {
            return downloadCache ?: SimpleCache(getDownloadContentDirectory(context), NoOpCacheEvictor(), getDataBase(context))
                .also { downloadCache = it }
        }
        private fun getDownloadContentDirectory(context: Context): File {
            return File(context.getExternalFilesDir(null), "Downloads")
        }
        private fun getDataBase(context: Context): DatabaseProvider {
            return StandaloneDatabaseProvider(context)
        }
    }

    private val applicationContext: Context = context.applicationContext
    private val listeners: CopyOnWriteArraySet<Listener> = CopyOnWriteArraySet()
    private val downloadIndex: DownloadIndex = downloadManager.downloadIndex
    private var startDownloadHlsDialogHelper: StartDownloadHlsDialogHelper? = null
    private var availableBytesLeft: Long =
        StatFs(DownloadUtil.getDownloadDirectory(context).path).availableBytes

    val downloads: HashMap<Uri, Download> = HashMap()

    init {
        downloadManager.addListener(DownloadManagerListener())
        loadDownloads()
    }

    fun addListener(listener: Listener) {
        Assertions.checkNotNull(listener)
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun isDownloaded(mediaItem: MediaItem): Boolean {
        val download = downloads[mediaItem.localConfiguration?.uri]
        return download != null && download.state == Download.STATE_COMPLETED
    }

    fun hasDownload(uri: Uri?): Boolean = downloads.keys.contains(uri)

    fun getDownloadRequest(uri: Uri?): DownloadRequest? {
        uri ?: return null
        val download = downloads[uri]
        return if(download != null && download.state != Download.STATE_FAILED) download.request else null
    }

    fun toggleDownloadDialogHelper(
        context: Context, mediaItem: MediaItem,
        positiveCallback: (() -> Unit)? = null, dismissCallback: (() -> Unit)? = null,
        quality : Int? = null

    ) {
        startDownloadHlsDialogHelper?.release()
        startDownloadHlsDialogHelper =
            StartDownloadHlsDialogHelper(
                context,
                getDownloadHelper(mediaItem),
                mediaItem,
                positiveCallback,
                dismissCallback,
                quality
            )
    }

    fun toggleDownloadPopupMenu(context: Context, anchor: View, uri: Uri?) {
        val popupMenu = PopupMenu(context, anchor).apply { inflate(R.menu.popup_menu) }
        val download = downloads[uri]
        download ?: return

        popupMenu.menu.apply {
            findItem(R.id.cancel_download).isVisible =
                listOf(
                    Download.STATE_DOWNLOADING,
                    Download.STATE_STOPPED,
                    Download.STATE_QUEUED,
                    Download.STATE_FAILED
                ).contains(download.state)
            findItem(R.id.delete_download).isVisible = download.state == Download.STATE_COMPLETED
            findItem(R.id.resume_download).isVisible =
                listOf(Download.STATE_STOPPED, Download.STATE_FAILED).contains(download.state)
            findItem(R.id.pause_download).isVisible = download.state == Download.STATE_DOWNLOADING
        }

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.cancel_download, R.id.delete_download -> removeDownload(download.request.uri)
                R.id.resume_download -> {
                    DownloadService.sendSetStopReason(
                        context,
                        MyDownloadService::class.java,
                        download.request.id,
                        Download.STOP_REASON_NONE,
                        true
                    )
                }
                R.id.pause_download -> {
                    DownloadService.sendSetStopReason(
                        context,
                        MyDownloadService::class.java,
                        download.request.id,
                        Download.STATE_STOPPED,
                        false
                    )
                }
            }
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
    }

    fun removeDownload(uri: Uri?) {
        val download = downloads[uri]
        download?.let {
            DownloadService.sendRemoveDownload(
                applicationContext,
                MyDownloadService::class.java,
                download.request.id,
                false
            )
        }
    }
    fun pauseDownload(uri: Uri?) {
        val download = downloads[uri]
        download?.let {
            DownloadService.sendPauseDownloads(
                applicationContext,
                MyDownloadService::class.java,
                false
            )
        }
    }
    fun resumeDownload(uri: Uri?) {
        val download = downloads[uri]
        download?.let {
            DownloadService.sendResumeDownloads(
                applicationContext,
                MyDownloadService::class.java,
                true
            )
        }
    }

    private fun loadDownloads() {
        try {
            downloadIndex.getDownloads().use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    val download = loadedDownloads.download
                    downloads[download.request.uri] = download
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun getAllDownloadProgressFlow(): Flow<List<Download>> = callbackFlow {
        while (coroutineContext.isActive) {
            trySend(downloads.values.toList()).isSuccess
            delay(1000)
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun getCurrentProgressDownload(uri: Uri?): Flow<Float?> {
        var percent: Float? =
            downloadManager.currentDownloads.find { it.request.uri == uri }?.percentDownloaded
        return callbackFlow {
            while (percent != null) {
                percent =
                    downloadManager.currentDownloads.find { it.request.uri == uri }?.percentDownloaded
                trySend(percent).isSuccess
                withContext(Dispatchers.IO) {
                    delay(1000)
                }
            }
        }
    }


    private fun getDownloadHelper(mediaItem: MediaItem): DownloadHelper {
        return when (mediaItem.localConfiguration?.mimeType) {
            MimeTypes.APPLICATION_MPD, MimeTypes.APPLICATION_M3U8, MimeTypes.APPLICATION_SS -> {
                DownloadHelper.forMediaItem(
                    applicationContext,
                    mediaItem,
                    DefaultRenderersFactory(applicationContext),
                    httpDataSourceFactory
                )
            }
            else -> DownloadHelper.forMediaItem(applicationContext, mediaItem)
        }
    }

    private inner class DownloadManagerListener : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            downloads[download.request.uri] = download
            for (listener in listeners) {
                listener.onDownloadsChanged(download)
            }
            if(download.state == Download.STATE_COMPLETED) {
                // Add delta between estimation and reality to have a better availableBytesLeft
                availableBytesLeft +=
                    Util.fromUtf8Bytes(download.request.data).toLong() - download.bytesDownloaded
            }
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            downloads.remove(download.request.uri)
            for (listener in listeners) {
                listener.onDownloadsChanged(download)
            }

            // Add the estimated or downloaded bytes to the availableBytes
            availableBytesLeft += if(download.percentDownloaded == 100f) {
                download.bytesDownloaded
            } else {
                Util.fromUtf8Bytes(download.request.data).toLong()
            }
        }
    }

    /**
    Can't use applicationContext because it'll result in a crash, instead
    Use context of the activity calling for the AlertDialog
     */
    private inner class StartDownloadHlsDialogHelper(
        private val context: Context,
        private val downloadHelper: DownloadHelper,
        private val mediaItem: MediaItem,
        private val positiveCallback: (() -> Unit)? = null,
        private val dismissCallback: (() -> Unit)? = null,
        quality: Int?
    ) : DownloadHelper.Callback {

        private var trackSelectionDialog: AlertDialog? = null
        var userQuality : Int? = quality

        var closestElement = 0

        init {
            downloadHelper.prepare(this)
        }

        fun release() {
            downloadHelper.release()
            trackSelectionDialog?.dismiss()
        }

        // DownloadHelper.Callback implementation.
        override fun onPrepared(helper: DownloadHelper) {
            if(helper.periodCount == 0) {
                Log.d(TAG, "No periods found. Downloading entire stream.")
                val mediaItemTag: MediaItemTag = mediaItem.localConfiguration?.tag as MediaItemTag
                val estimatedContentLength: Long = (DEFAULT_BITRATE * mediaItemTag.duration)
                    .div(C.MILLIS_PER_SECOND).div(C.BITS_PER_BYTE)
                val downloadRequest: DownloadRequest = downloadHelper.getDownloadRequest(
                    mediaItemTag.title,
                    Util.getUtf8Bytes(estimatedContentLength.toString())
                )
                startDownload(downloadRequest)
                release()
                return
            }

            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
            val formatDownloadable: MutableList<Format> = mutableListOf()
            var qualitySelected: DefaultTrackSelector.Parameters
            val mappedTrackInfo = downloadHelper.getMappedTrackInfo(0)

            for (i in 0 until mappedTrackInfo.rendererCount) {
                if(C.TRACK_TYPE_VIDEO == mappedTrackInfo.getRendererType(i)) {
                    val trackGroups: TrackGroupArray = mappedTrackInfo.getTrackGroups(i)
                    for (j in 0 until trackGroups.length) {
                        val trackGroup: TrackGroup = trackGroups[j]
                        for (k in 0 until trackGroup.length) {
                            formatDownloadable.add(trackGroup.getFormat(k))
                        }
                    }
                }
            }

            if(formatDownloadable.isEmpty()) {
                dialogBuilder.setTitle("An error occurred")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }

            // We sort here because later we use formatDownloadable to select track
            formatDownloadable.sortBy { it.height }
            val mediaItemTag: MediaItemTag = mediaItem.localConfiguration?.tag as MediaItemTag
            val optionsDownload: List<String> = formatDownloadable.map {
                context.getString(
                    R.string.dialog_option, it.height,
                    (it.bitrate * mediaItemTag.duration).div(8000).formatFileSize()
                )
            }
            val qualityLis = formatDownloadable.map { it.bitrate }

            closestElement = qualityLis.minByOrNull { abs(it - userQuality!!) }!!

            val result = "Closest element to $userQuality is $closestElement"
            Log.e("Closed", result)

            //Default quality download
            qualitySelected = DefaultTrackSelector(context).buildUponParameters()
                .setMinVideoSize(formatDownloadable[0].width, closestElement)
                .setMinVideoBitrate(formatDownloadable[0].bitrate)
                .setMaxVideoSize(formatDownloadable[0].width, formatDownloadable[0].height)
                .setMaxVideoBitrate(formatDownloadable[0].bitrate)
                .build()

            dialogBuilder.setTitle("Select Download Format")
                .setSingleChoiceItems(optionsDownload.toTypedArray(), 0) { _, which ->
                    val format = formatDownloadable[which]
                    qualitySelected = DefaultTrackSelector(context).buildUponParameters()
                        .setMinVideoSize(format.width, format.height)
                        .setMinVideoBitrate(format.bitrate)
                        .setMaxVideoSize(format.width, format.height)
                        .setMaxVideoBitrate(format.bitrate)
                        .build()
                    Log.e(TAG, "format Selected= width: ${format.width}, height: ${format.height}, qualitySelected:${qualitySelected}",)


                    /**
                     * This function will save the quality selected by the user
                     * in the shared preferences
                     */
                    DownloadUtil.saveQualitySelected(context, format.height,qualitySelected)

                }.setPositiveButton("Download") { _, _ ->
                    val height = DownloadUtil.getQualitySelected(context)
                    //log the value of height and width
                    Log.e(TAG, "height: $height")

                    helper.clearTrackSelections(0)
                    helper.addTrackSelection(0, qualitySelected)
                    val estimatedContentLength: Long =
                        (qualitySelected.maxVideoBitrate * mediaItemTag.duration)
                            .div(C.MILLIS_PER_SECOND).div(C.BITS_PER_BYTE)
                    if(availableBytesLeft > estimatedContentLength) {
                        val downloadRequest: DownloadRequest = downloadHelper.getDownloadRequest(
                            (mediaItem.localConfiguration?.tag as MediaItemTag).title,
                            Util.getUtf8Bytes(estimatedContentLength.toString())
                        )
                      startDownload(downloadRequest)
                        availableBytesLeft -= estimatedContentLength
                        Log.e(TAG, "availableBytesLeft after calculation: $availableBytesLeft")
                    } else {
                        Toast.makeText(
                            context,
                            "Not enough space to download this file",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    positiveCallback?.invoke()
                }.setOnDismissListener {
                    trackSelectionDialog = null
                    downloadHelper.release()
                    dismissCallback?.invoke()
                }
            trackSelectionDialog = dialogBuilder.create().apply { show() }
        }

        override fun onPrepareError(helper: DownloadHelper, e: IOException) {
            Toast.makeText(applicationContext, R.string.download_start_error, Toast.LENGTH_LONG)
                .show()
            Log.e(
                TAG,
                if (e is DownloadHelper.LiveContentUnsupportedException) "Downloading live content unsupported" else "Failed to start download",
                e
            )
        }

        // Internal methods.
        private fun startDownload(downloadRequest: DownloadRequest = buildDownloadRequest()) {
            DownloadService.sendAddDownload(
                applicationContext,
                MyDownloadService::class.java,
                downloadRequest,
                true
            )
        }

        private fun buildDownloadRequest(): DownloadRequest {
            return downloadHelper.getDownloadRequest(
                (mediaItem.localConfiguration?.tag as MediaItemTag).title,
                Util.getUtf8Bytes(mediaItem.localConfiguration?.uri.toString())
            )
        }
    }


}