package nes.app.util

import android.net.Uri
import android.text.format.DateUtils
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

val Player.formatedElapsedTime: String get() = DateUtils.formatElapsedTime(currentPosition / 1000L)
val MediaItem?.title: String get() = this?.mediaMetadata?.title?.toString() ?: "--"
val MediaItem?.artworkUri: Uri? get() = this?.mediaMetadata?.artworkUri
val MediaItem?.albumTitle: String get() = this?.mediaMetadata?.albumTitle?.toString() ?: "--"

/** Must be called on metadata with extras **/
val MediaItem.mediaMetaData: Pair<Long, String> get() = mediaMetadata.extras!!.toShowInfo()