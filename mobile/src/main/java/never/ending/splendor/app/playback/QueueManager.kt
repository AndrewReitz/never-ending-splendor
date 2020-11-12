package never.ending.splendor.app.playback

import android.content.res.Resources
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.squareup.picasso.Picasso
import never.ending.splendor.R
import never.ending.splendor.app.model.MusicProvider
import never.ending.splendor.app.utils.MediaIdHelper
import never.ending.splendor.app.utils.MediaIdHelper.musicId
import never.ending.splendor.app.utils.QueueHelper
import never.ending.splendor.app.utils.loadLargeAndSmallImage
import timber.log.Timber
import java.util.Collections
import kotlin.math.max

/**
 * Simple data provider for queues. Keeps track of a current queue and a current index in the
 * queue. Also provides methods to set the current queue based on common queries, relying on a
 * given MusicProvider to provide the actual media metadata.
 */
class QueueManager(
    private val musicProvider: MusicProvider,
    private val resources: Resources,
    private val picasso: Picasso,
    private val metadataUpdateListener: MetadataUpdateListener
) {
    // "Now playing" queue:
    private var playingQueue: List<MediaSessionCompat.QueueItem>

    private var currentIndex: Int = 0
        set(value) {
            if (value >= 0 && value < playingQueue.size) {
                field = value
                metadataUpdateListener.onCurrentQueueIndexUpdated(currentIndex)
            }
        }

    private fun isSameBrowsingCategory(mediaId: String): Boolean {
        val newBrowseHierarchy = MediaIdHelper.getHierarchy(mediaId)
        val current = currentMusic ?: return false
        val currentBrowseHierarchy = MediaIdHelper.getHierarchy(
            current.description.mediaId!!
        )
        return newBrowseHierarchy.contentEquals(currentBrowseHierarchy)
    }

    fun setCurrentQueueItem(queueId: Long): Boolean {
        // set the current index on queue from the queue Id:
        val index = QueueHelper.getMusicIndexOnQueue(playingQueue, queueId)
        currentIndex = index
        return index >= 0
    }

    private fun setCurrentQueueItem(mediaId: String): Boolean {
        // set the current index on queue from the music Id:
        val index = QueueHelper.getMusicIndexOnQueue(playingQueue, mediaId)
        currentIndex = index
        return index >= 0
    }

    fun skipQueuePosition(amount: Int): Boolean {
        var index = currentIndex + amount
        if (index < 0) {
            // skip backwards before the first song will keep you on the first song
            index = 0
        } else {
            // skip forwards when in last song will cycle back to start of the queue
            index %= playingQueue.size
        }
        if (!QueueHelper.isIndexPlayable(index, playingQueue)) {
            Timber.e(
                "Cannot increment queue index by %s . Current=%s queue length=%s", amount,
                currentIndex, playingQueue.size
            )
            return false
        }
        currentIndex = index
        return true
    }

    fun setQueueFromSearch(query: String?, extras: Bundle?): Boolean {
        val queue = QueueHelper.getPlayingQueueFromSearch(query, extras)
        setCurrentQueue(resources.getString(R.string.search_queue_title), queue)
        return queue.isNotEmpty()
    }

    fun setRandomQueue() {
        setCurrentQueue(
            resources.getString(R.string.random_queue_title),
            QueueHelper.getRandomQueue()
        )
    }

    fun setQueueFromMusic(mediaId: String) {
        Timber.d("setQueueFromMusic %s", mediaId)

        // The mediaId used here is not the unique musicId. This one comes from the
        // MediaBrowser, and is actually a "hierarchy-aware mediaID": a concatenation of
        // the hierarchy in MediaBrowser and the actual unique musicID. This is necessary
        // so we can build the correct playing queue, based on where the track was
        // selected from.
        var canReuseQueue = false
        if (isSameBrowsingCategory(mediaId)) {
            canReuseQueue = setCurrentQueueItem(mediaId)
        }
        if (!canReuseQueue) {
            val queueTitle = resources.getString(
                R.string.browse_musics_by_genre_subtitle,
                MediaIdHelper.extractBrowseCategoryValueFromMediaID(mediaId)
            )
            setCurrentQueue(
                queueTitle,
                requireNotNull(QueueHelper.getPlayingQueue(mediaId, musicProvider)),
                mediaId
            )
        }
        updateMetadata()
    }

    val currentMusic: MediaSessionCompat.QueueItem?
        get() = if (!QueueHelper.isIndexPlayable(currentIndex, playingQueue)) {
            null
        } else playingQueue[currentIndex]

    val currentQueueSize: Int
        get() = playingQueue.size

    private fun setCurrentQueue(title: String, newQueue: List<MediaSessionCompat.QueueItem>) {
        setCurrentQueue(title, newQueue, null)
    }

    private fun setCurrentQueue(
        title: String,
        newQueue: List<MediaSessionCompat.QueueItem>,
        initialMediaId: String?
    ) {
        playingQueue = newQueue
        val index = QueueHelper.getMusicIndexOnQueue(playingQueue, initialMediaId)
        currentIndex = max(index, 0)
        metadataUpdateListener.onQueueUpdated(title, newQueue)
    }

    val duration: Long
        get() {
            val currentMusic = currentMusic ?: return -1
            val musicId = currentMusic.description.mediaId?.musicId
            val metadata = musicProvider.getMusic(musicId)
                ?: throw IllegalArgumentException("Invalid musicId $musicId")
            return metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        }

    fun updateMetadata() {
        val currentMusic = currentMusic
        if (currentMusic == null) {
            metadataUpdateListener.onMetadataRetrieveError()
            return
        }

        val musicId = requireNotNull(currentMusic.description.mediaId?.musicId)

        val metadata = musicProvider.getMusic(musicId)
            ?: throw IllegalArgumentException("Invalid musicId $musicId")
        metadataUpdateListener.onMetadataChanged(metadata)

        // Set the proper album artwork on the media session, so it can be shown in the
        // locked screen and in other places.
        if (metadata.description.iconBitmap == null &&
            metadata.description.iconUri != null
        ) {
            val albumUri = metadata.description.iconUri.toString()
            picasso.loadLargeAndSmallImage(albumUri) { (image, icon) ->
                musicProvider.updateMusicArt(musicId, image, icon)

                // If we are still playing the same music, notify the listeners:
                val music = currentMusic
                val currentPlayingId = music.description.mediaId?.musicId
                if (musicId == currentPlayingId) {
                    metadataUpdateListener.onMetadataChanged(requireNotNull(musicProvider.getMusic(currentPlayingId)))
                }
            }
        }
    }

    interface MetadataUpdateListener {
        fun onMetadataChanged(metadata: MediaMetadataCompat)
        fun onMetadataRetrieveError()
        fun onCurrentQueueIndexUpdated(queueIndex: Int)
        fun onQueueUpdated(title: String, newQueue: List<MediaSessionCompat.QueueItem>)
    }

    init {
        playingQueue = Collections.synchronizedList(ArrayList<MediaSessionCompat.QueueItem>())
        currentIndex = 0
    }
}
