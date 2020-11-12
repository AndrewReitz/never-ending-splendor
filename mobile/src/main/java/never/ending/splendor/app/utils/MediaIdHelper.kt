package never.ending.splendor.app.utils

import android.support.v4.media.MediaBrowserCompat
import timber.log.Timber

/**
 * Utility class to help on queue related tasks.
 */
object MediaIdHelper {
    const val MEDIA_ID_ROOT = "__ROOT__"
    const val MEDIA_ID_MUSICS_BY_SEARCH = "__BY_SEARCH__"
    const val MEDIA_ID_SHOWS_BY_YEAR = "__SHOWS_BY_YEAR__"
    const val MEDIA_ID_TRACKS_BY_SHOW = "__TRACKS_BY_SHOW__"
    private const val CATEGORY_SEPARATOR = '/'
    private const val LEAF_SEPARATOR = '|'

    /**
     * Create a String value that represents a playable or a browsable media.
     *
     * Encode the media browseable categories, if any, and the unique music ID, if any,
     * into a single String mediaID.
     *
     * MediaIDs are of the form <categoryType>/<categoryValue>|<musicUniqueId>, to make it easy
     * to find the category (like genre) that a music was selected from, so we
     * can correctly build the playing queue. This is specially useful when
     * one music can appear in more than one list, like "by genre -> genre_1"
     * and "by artist -> artist_1".
     *
     * @param musicID Unique music ID for playable items, or null for browseable items.
     * @param categories hierarchy of categories representing this item's browsing parents
     * @return a hierarchy-aware media ID
     </musicUniqueId></categoryValue></categoryType> */
    fun createMediaId(musicID: String?, vararg categories: String): String {
        val sb = StringBuilder()
        for (i in categories.indices) {
            require(isValidCategory(categories[i])) { "Invalid category: ${categories[0]}" }
            sb.append(categories[i])
            if (i < categories.size - 1) {
                sb.append(CATEGORY_SEPARATOR)
            }
        }
        if (musicID != null) {
            sb.append(LEAF_SEPARATOR).append(musicID)
        }
        return sb.toString()
    }

    private fun isValidCategory(category: String?): Boolean {
        return category == null ||
            category.indexOf(CATEGORY_SEPARATOR) < 0 &&
            category.indexOf(LEAF_SEPARATOR) < 0
    }

    /**
     * Extracts unique musicID from the mediaID. mediaID is, by this sample's convention, a
     * concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and unique
     * musicID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param mediaId that contains the musicID
     * @return musicID
     */
    @Deprecated(message = "Use musicId getter instead")
    fun extractMusicIDFromMediaID(mediaId: String): String? {
        val pos = mediaId.indexOf(LEAF_SEPARATOR)
        return if (pos >= 0) {
            mediaId.substring(pos + 1)
        } else null
    }

    // TODO write tests.
    @Suppress("DEPRECATION")
    val MediaBrowserCompat.MediaItem.musicId: String? get() = extractMusicIDFromMediaID(description.mediaId.orEmpty())
    @Suppress("DEPRECATION")
    val String.musicId: String? get() = extractMusicIDFromMediaID(this)

    fun extractShowFromMediaID(mediaID: String): String? {
        val pos = mediaID.indexOf(CATEGORY_SEPARATOR)
        return if (pos >= 0) {
            mediaID.substring(pos + 1)
        } else null
    }

    /**
     * Extracts category and categoryValue from the mediaID. mediaID is, by this sample's
     * convention, a concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and
     * mediaID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param mediaId that contains a category and categoryValue.
     */
    fun getHierarchy(mediaId: String): Array<String> {
        var newMediaId = mediaId
        val pos = newMediaId.indexOf(LEAF_SEPARATOR)
        if (pos >= 0) {
            newMediaId = newMediaId.substring(0, pos)
        }
        return newMediaId.split(CATEGORY_SEPARATOR.toString().toRegex()).toTypedArray()
    }

    fun extractBrowseCategoryValueFromMediaID(mediaID: String): String? {
        val hierarchy = getHierarchy(mediaID)
        return if (hierarchy.size == 2) {
            hierarchy[1]
        } else null
    }

    fun isBrowseable(mediaID: String): Boolean = mediaID.indexOf(LEAF_SEPARATOR) < 0

    fun isShow(mediaID: String): Boolean {
        Timber.d(mediaID)
        val hierarchy = getHierarchy(mediaID)
        return hierarchy[0].matches(MEDIA_ID_TRACKS_BY_SHOW.toRegex())
    }

    fun getParentMediaID(mediaID: String): String {
        val hierarchy = getHierarchy(mediaID)
        if (!isBrowseable(mediaID)) {
            return createMediaId(null, *hierarchy)
        }
        if (hierarchy.size <= 1) {
            return MEDIA_ID_ROOT
        }
        val parentHierarchy = hierarchy.copyOfRange(0, hierarchy.size - 1)
        return createMediaId(null, *parentHierarchy)
    }
}
