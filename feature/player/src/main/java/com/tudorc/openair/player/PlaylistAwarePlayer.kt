package com.tudorc.openair.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

@UnstableApi
internal class PlaylistAwarePlayer(player: Player) : ForwardingPlayer(player) {
    override fun getAvailableCommands(): Player.Commands {
        val base = super.getAvailableCommands().buildUpon()
        return if (PlaybackCommandBridge.hasPlaylist()) {
            base.add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .build()
        } else {
            base.remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .remove(Player.COMMAND_SEEK_TO_NEXT)
                .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                .build()
        }
    }

    override fun seekToNextMediaItem() {
        if (!PlaybackCommandBridge.skipNext()) {
            super.seekToNextMediaItem()
        }
    }

    override fun seekToPreviousMediaItem() {
        if (!PlaybackCommandBridge.skipPrevious()) {
            super.seekToPreviousMediaItem()
        }
    }

    override fun seekToNext() {
        seekToNextMediaItem()
    }

    override fun seekToPrevious() {
        seekToPreviousMediaItem()
    }
}
