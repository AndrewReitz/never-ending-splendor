package never.ending.splendor.app.utils

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import never.ending.splendor.app.VoiceSearchParams
import never.ending.splendor.app.model.MusicProvider
import never.ending.splendor.app.utils.MediaIdHelper.MEDIA_ID_MUSICS_BY_SEARCH
import never.ending.splendor.app.utils.MediaIdHelper.MEDIA_ID_TRACKS_BY_SHOW
import never.ending.splendor.app.utils.MediaIdHelper.createMediaId
import never.ending.splendor.app.utils.MediaIdHelper.getHierarchy
import timber.log.Timber

/**
 * Utility class to help on queue related tasks.
 */
object QueueHelper {
    private const val RANDOM_QUEUE_SIZE = 10
    fun getPlayingQueue(
        mediaId: String,
        musicProvider: MusicProvider
    ): List<MediaSessionCompat.QueueItem>? {

        // extract the browsing hierarchy from the media ID:
        val hierarchy = getHierarchy(mediaId)
        if (hierarchy.size != 2) {
            Timber.e("Could not build a playing queue for this mediaId: %s", mediaId)
            return null
        }
        val categoryType = hierarchy[0]
        val categoryValue = hierarchy[1]
        Timber.d("Creating playing queue for %s, %s", categoryType, categoryValue)
        var tracks: Iterable<MediaMetadataCompat>? = null
        // This sample only supports genre and by_search category types.
        if (categoryType == MEDIA_ID_TRACKS_BY_SHOW) {
            tracks = musicProvider.getCachedTracksForShow(categoryValue)
        }
        /*
        else if (categoryType.equals(MEDIA_ID_MUSICS_BY_SEARCH)) {
            tracks = musicProvider.searchMusicBySongTitle(categoryValue);
        }
*/if (tracks == null) {
            Timber.e("Unrecognized category type: %s for media %s", categoryType, mediaId)
            return null
        }
        return convertToQueue(tracks, hierarchy[0], hierarchy[1])
    }

    fun getPlayingQueueFromSearch(
        query: String?,
        queryParams: Bundle?
    ): List<MediaSessionCompat.QueueItem> {
        Timber.d(
            "Creating playing queue for musics from search: %s params=%s", query,
            queryParams
        )
        val params = VoiceSearchParams(query!!, queryParams)
        Timber.d("VoiceSearchParams: %s", params)
        if (params.isAny) {
            // If isAny is true, we will play anything. This is app-dependent, and can be,
            // for example, favorite playlists, "I'm feeling lucky", most recent, etc.
            return getRandomQueue()
        }
        val result: Iterable<MediaMetadataCompat>? = null

/*
        if (params.isAlbumFocus) {
            result = musicProvider.searchMusicByAlbum(params.album);
        } else if (params.isGenreFocus) {
            result = musicProvider.getMusicsByGenre(params.genre);
        } else if (params.isArtistFocus) {
            result = musicProvider.searchMusicByArtist(params.artist);
        } else if (params.isSongFocus) {
            result = musicProvider.searchMusicBySongTitle(params.song);
        }

        // If there was no results using media focus parameter, we do an unstructured query.
        // This is useful when the user is searching for something that looks like an artist
        // to Google, for example, but is not. For example, a user searching for Madonna on
        // a PodCast application wouldn't get results if we only looked at the
        // Artist (podcast author). Then, we can instead do an unstructured search.
        if (params.isUnstructured || result == null || !result.iterator().hasNext()) {
            // To keep it simple for this example, we do unstructured searches on the
            // song title only. A real world application could search on other fields as well.
            result = musicProvider.searchMusicBySongTitle(query);
        }
*/
        return convertToQueue(result, MEDIA_ID_MUSICS_BY_SEARCH, query)
    }

    fun getMusicIndexOnQueue(
        queue: Iterable<MediaSessionCompat.QueueItem>?,
        mediaId: String?
    ): Int {
        if (queue == null) return -1
        for ((index, item) in queue.withIndex()) {
            if (mediaId == item.description.mediaId) {
                return index
            }
        }
        return -1
    }

    fun getMusicIndexOnQueue(
        queue: Iterable<MediaSessionCompat.QueueItem>,
        queueId: Long
    ): Int {
        for ((index, item) in queue.withIndex()) {
            if (queueId == item.queueId) {
                return index
            }
        }
        return -1
    }

    private fun convertToQueue(
        tracks: Iterable<MediaMetadataCompat>?,
        vararg categories: String
    ): List<MediaSessionCompat.QueueItem> {
        val queue: MutableList<MediaSessionCompat.QueueItem> = ArrayList()
        for ((count, track) in tracks!!.withIndex()) {

            // We create a hierarchy-aware mediaID, so we know what the queue is about by looking
            // at the QueueItem media IDs.
            val hierarchyAwareMediaID = createMediaId(
                track.description.mediaId, *categories
            )
            val trackCopy = MediaMetadataCompat.Builder(track)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build()

            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            val item = MediaSessionCompat.QueueItem(
                trackCopy.description, count.toLong()
            )
            queue.add(item)
        }
        return queue
    }

    /**
     * Create a random queue with at most [.RANDOM_QUEUE_SIZE] elements.
     *
     * @return list containing [MediaSessionCompat.QueueItem]'s
     */
    fun getRandomQueue(): List<MediaSessionCompat.QueueItem> {
        val result: List<MediaMetadataCompat> = ArrayList(RANDOM_QUEUE_SIZE)
        return convertToQueue(result, MEDIA_ID_MUSICS_BY_SEARCH, "random")
    }

    fun isIndexPlayable(index: Int, queue: List<MediaSessionCompat.QueueItem?>?): Boolean {
        return queue != null && index >= 0 && index < queue.size
    }
}
