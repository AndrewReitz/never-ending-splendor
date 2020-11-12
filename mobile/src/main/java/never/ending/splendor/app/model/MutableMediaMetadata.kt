package never.ending.splendor.app.model

import android.support.v4.media.MediaMetadataCompat

/**
 * Holder class that encapsulates a MediaMetadata and allows the actual metadata to be modified
 * without requiring to rebuild the collections the metadata is in.
 */
data class MutableMediaMetadata(
    val trackId: String,
    // todo make immutable
    var metadata: MediaMetadataCompat
)
