/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package never.ending.splendor.app.playback

import android.support.v4.media.session.MediaSessionCompat

/**
 * Interface representing either Local or Remote Playback. The [MusicService] works
 * directly with an instance of the Playback object to make the various calls such as
 * play, pause etc.
 */
interface Playback {
    /**
     * Start/setup the playback.
     * Resources/listeners would be allocated by implementations.
     */
    fun start()

    /**
     * Stop the playback. All resources can be de-allocated by implementations here.
     * @param notifyListeners if true and a callback has been set by setCallback,
     * callback.onPlaybackStatusChanged will be called after changing
     * the state.
     */
    fun stop(notifyListeners: Boolean)
    /**
     * Get the current [android.media.session.PlaybackState.getState]
     */
    /**
     * Set the latest playback state as determined by the caller.
     */
    var state: Int

    /**
     * @return boolean that indicates that this is ready to be used.
     */
    val isConnected: Boolean

    /**
     * @return boolean indicating whether the player is playing or is supposed to be
     * playing when we gain audio focus.
     */
    val isPlaying: Boolean
    /**
     * @return pos if currently playing an item
     */
    /**
     * Set the current position. Typically used when switching players that are in
     * paused state.
     *
     * @param pos position in the stream
     */
    var currentStreamPosition: Int

    /**
     * Query the underlying stream and update the internal last known stream position.
     */
    fun updateLastKnownStreamPosition()

    /**
     * @param item to play
     */
    fun play(item: MediaSessionCompat.QueueItem)

    /**
     * @param item to play after the currently playing item, using gapless playback
     * returns true if gapless is supported, false otherwise
     */
    fun playNext(item: MediaSessionCompat.QueueItem): Boolean

    /**
     * Pause the current playing item
     */
    fun pause()

    /**
     * Seek to the given position
     */
    fun seekTo(position: Int)

    /**
     * The current media Id being processed in any state or null.
     *
     * Set the current mediaId. This is only used when switching from one
     * playback to another.
     */
    var currentMediaId: String?

    interface Callback {
        /**
         * On current music completed.
         */
        fun onCompletion()

        /**
         * on Playback status changed
         * Implementations can use this callback to update
         * playback state on the media sessions.
         */
        fun onPlaybackStatusChanged(state: Int)

        /**
         * @param error to be added to the PlaybackState
         */
        fun onError(error: String?)

        /**
         * @param mediaId being currently played
         */
        fun setCurrentMediaId(mediaId: String?)

        companion object {
            val EMPTY = object : Callback {
                override fun onCompletion() = Unit
                override fun onPlaybackStatusChanged(state: Int) = Unit
                override fun onError(error: String?) = Unit
                override fun setCurrentMediaId(mediaId: String?) = Unit
            }
        }
    }

    var callback: Callback

    val supportsGapless: Boolean
}
