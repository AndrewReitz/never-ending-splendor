package nes.app.util

import android.text.format.DateUtils
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

val Long.formatedElapsedTime: String get() = DateUtils.formatElapsedTime(this / 1000L)
val Player.formatedElapsedTime: String get() = DateUtils.formatElapsedTime(currentPosition / 1000L)
val MediaItem?.title: String get() = this?.mediaMetadata?.title?.toString() ?: "--"

/** Must be called on metadata with extras **/
val MediaItem.mediaExtras: Pair<Long, String> get() = mediaMetadata.extras!!.toShowInfo()