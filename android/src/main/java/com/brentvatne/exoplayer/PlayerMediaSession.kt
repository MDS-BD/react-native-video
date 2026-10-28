package com.brentvatne.exoplayer

import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession

/**
 * Shared MediaSession setup for the in-player session (Fire OS voice control)
 * and the notification session of VideoPlaybackService, so both apply the same rules.
 */
object PlayerMediaSession {
    private const val FIRE_TV_FEATURE = "amazon.hardware.fire_tv"

    fun isFireTv(context: Context): Boolean =
        Build.MANUFACTURER.equals("Amazon", ignoreCase = true) &&
            context.packageManager.hasSystemFeature(FIRE_TV_FEATURE)

    fun builder(context: Context, player: Player): MediaSession.Builder =
        MediaSession.Builder(context, SessionPlayer(player))
            // Session IDs must be unique per process, otherwise media3 throws on build()
            .setId("RNVideo_" + player.hashCode())

    /** Returns the player the session was built for, unwrapping [SessionPlayer]. */
    fun unwrap(player: Player): Player = (player as? SessionPlayer)?.wrappedPlayer ?: player
}

/**
 * Player handed to MediaSession controllers (Alexa, notification, headsets).
 * Every controller command ends up in one of the overridden methods below, so this is the
 * single place to restrict what external controllers may do. ReactExoplayerView itself keeps
 * using the unwrapped ExoPlayer and is not affected.
 */
@OptIn(UnstableApi::class)
class SessionPlayer(player: Player) : ForwardingPlayer(player) {

    // Original session listener -> wrapper that reports our filtered commands, see addListener
    private val listeners = mutableMapOf<Player.Listener, Player.Listener>()

    /*
     * No transport control from controllers during ads and on live streams, so Live TV stays
     * unpausable. Live uses the same check as the live badge in ExoPlayerView; ODC streams are
     * expected to be VOD manifests and therefore behave like any other VOD.
     */
    private val isTransportLocked: Boolean
        get() = isPlayingAd || isCurrentMediaItemLive

    override fun getAvailableCommands(): Player.Commands {
        val commands = super.getAvailableCommands().buildUpon()
            // The player only ever holds one item: don't let "next"/"previous" restart playback
            .removeAll(
                COMMAND_SEEK_TO_NEXT,
                COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                COMMAND_SEEK_TO_PREVIOUS,
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
            )
        if (isTransportLocked) {
            commands.removeAll(
                COMMAND_PLAY_PAUSE,
                COMMAND_STOP,
                COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                COMMAND_SEEK_TO_DEFAULT_POSITION,
                COMMAND_SEEK_BACK,
                COMMAND_SEEK_FORWARD
            )
        }
        return commands.build()
    }

    override fun isCommandAvailable(command: Int): Boolean = availableCommands.contains(command)

    /*
     * ForwardingPlayer passes the wrapped player's commands to listeners in
     * onAvailableCommandsChanged. The session caches what it gets there, so without this it
     * would advertise ExoPlayer's commands instead of ours after every change (e.g. ad start).
     */
    override fun addListener(listener: Player.Listener) {
        val filtering = CommandFilteringListener(listener) { availableCommands }
        listeners[listener] = filtering
        super.addListener(filtering)
    }

    override fun removeListener(listener: Player.Listener) {
        listeners.remove(listener)?.let { super.removeListener(it) }
    }

    // Guards below make the lock independent of what a controller thinks is available

    override fun play() {
        if (!isTransportLocked) super.play()
    }

    override fun pause() {
        if (!isTransportLocked) super.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (!isTransportLocked) super.setPlayWhenReady(playWhenReady)
    }

    // "Alexa, stop" must pause, not drop the player into STATE_IDLE behind the JS layer's back
    override fun stop() = pause()

    override fun seekTo(positionMs: Long) {
        if (!isTransportLocked) super.seekTo(positionMs)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (!isTransportLocked) super.seekTo(mediaItemIndex, positionMs)
    }

    override fun seekToDefaultPosition() {
        if (!isTransportLocked) super.seekToDefaultPosition()
    }

    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        if (!isTransportLocked) super.seekToDefaultPosition(mediaItemIndex)
    }

    override fun seekBack() {
        if (!isTransportLocked) super.seekBack()
    }

    override fun seekForward() {
        if (!isTransportLocked) super.seekForward()
    }
}

/**
 * Forwards every [Player.Listener] callback unchanged, except that onAvailableCommandsChanged
 * reports the commands of [SessionPlayer] instead of the wrapped player's.
 *
 * All Player.Listener methods are Java default methods, which Kotlin's `by` delegation does not
 * forward, so each one is forwarded explicitly (media3 1.4.1). A callback missing here never
 * reaches the session: check this list when bumping media3.
 */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
private class CommandFilteringListener(
    private val listener: Player.Listener,
    private val filteredCommands: () -> Player.Commands
) : Player.Listener {
    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) =
        listener.onAvailableCommandsChanged(filteredCommands())

    override fun onEvents(player: Player, events: Player.Events) = listener.onEvents(player, events)
    override fun onTimelineChanged(timeline: Timeline, reason: Int) = listener.onTimelineChanged(timeline, reason)
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = listener.onMediaItemTransition(mediaItem, reason)
    override fun onTracksChanged(tracks: Tracks) = listener.onTracksChanged(tracks)
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = listener.onMediaMetadataChanged(mediaMetadata)
    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) = listener.onPlaylistMetadataChanged(mediaMetadata)
    override fun onIsLoadingChanged(isLoading: Boolean) = listener.onIsLoadingChanged(isLoading)
    override fun onLoadingChanged(isLoading: Boolean) = listener.onLoadingChanged(isLoading)
    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) =
        listener.onTrackSelectionParametersChanged(parameters)
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) =
        listener.onPlayerStateChanged(playWhenReady, playbackState)
    override fun onPlaybackStateChanged(playbackState: Int) = listener.onPlaybackStateChanged(playbackState)
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = listener.onPlayWhenReadyChanged(playWhenReady, reason)
    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) =
        listener.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
    override fun onIsPlayingChanged(isPlaying: Boolean) = listener.onIsPlayingChanged(isPlaying)
    override fun onRepeatModeChanged(repeatMode: Int) = listener.onRepeatModeChanged(repeatMode)
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = listener.onShuffleModeEnabledChanged(shuffleModeEnabled)
    override fun onPlayerError(error: PlaybackException) = listener.onPlayerError(error)
    override fun onPlayerErrorChanged(error: PlaybackException?) = listener.onPlayerErrorChanged(error)
    override fun onPositionDiscontinuity(reason: Int) = listener.onPositionDiscontinuity(reason)
    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) =
        listener.onPositionDiscontinuity(oldPosition, newPosition, reason)
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) =
        listener.onPlaybackParametersChanged(playbackParameters)
    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) = listener.onSeekBackIncrementChanged(seekBackIncrementMs)
    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) =
        listener.onSeekForwardIncrementChanged(seekForwardIncrementMs)
    override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) =
        listener.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)
    override fun onAudioSessionIdChanged(audioSessionId: Int) = listener.onAudioSessionIdChanged(audioSessionId)
    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) = listener.onAudioAttributesChanged(audioAttributes)
    override fun onVolumeChanged(volume: Float) = listener.onVolumeChanged(volume)
    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) = listener.onSkipSilenceEnabledChanged(skipSilenceEnabled)
    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) = listener.onDeviceInfoChanged(deviceInfo)
    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) = listener.onDeviceVolumeChanged(volume, muted)
    override fun onVideoSizeChanged(videoSize: VideoSize) = listener.onVideoSizeChanged(videoSize)
    override fun onSurfaceSizeChanged(width: Int, height: Int) = listener.onSurfaceSizeChanged(width, height)
    override fun onRenderedFirstFrame() = listener.onRenderedFirstFrame()
    override fun onCues(cues: List<Cue>) = listener.onCues(cues)
    override fun onCues(cueGroup: CueGroup) = listener.onCues(cueGroup)
    override fun onMetadata(metadata: Metadata) = listener.onMetadata(metadata)
}
