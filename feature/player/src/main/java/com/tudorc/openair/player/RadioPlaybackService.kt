package com.tudorc.openair.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var sessionPlayer: Player
    private lateinit var mediaSession: MediaSession

    override fun attachBaseContext(newBase: Context) {
        val attributed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            newBase.createAttributionContext(ATTRIBUTION_TAG)
        } else {
            newBase
        }
        super.attachBaseContext(attributed)
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            val attributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(attributes, true)
        }
        sessionPlayer = PlaylistAwarePlayer(player)
        val callback = object : MediaSession.Callback {
            override fun onPlaybackResumption(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val empty = MediaSession.MediaItemsWithStartPosition(
                    emptyList<MediaItem>(),
                    C.INDEX_UNSET,
                    C.TIME_UNSET
                )
                return Futures.immediateFuture(empty)
            }
        }
        val mediaSessionBuilder = MediaSession.Builder(this, sessionPlayer)
            .setCallback(callback)
        createSessionActivity()?.let { sessionActivity ->
            mediaSessionBuilder.setSessionActivity(sessionActivity)
        }
        mediaSession = mediaSessionBuilder.build()
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun createSessionActivity(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

private const val ATTRIBUTION_TAG = "openair"
