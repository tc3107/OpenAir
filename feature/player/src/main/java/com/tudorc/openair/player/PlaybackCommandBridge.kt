package com.tudorc.openair.player

internal interface PlaylistCommandHandler {
    fun hasPlaylistContext(): Boolean
    fun skipNext(): Boolean
    fun skipPrevious(): Boolean
}

internal object PlaybackCommandBridge {
    @Volatile private var handler: PlaylistCommandHandler? = null

    fun bind(handler: PlaylistCommandHandler?) {
        this.handler = handler
    }

    fun hasPlaylist(): Boolean = handler?.hasPlaylistContext() ?: false

    fun skipNext(): Boolean = handler?.skipNext() ?: false

    fun skipPrevious(): Boolean = handler?.skipPrevious() ?: false
}
