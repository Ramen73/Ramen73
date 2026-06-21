package com.ramen73.ramenchat.utils

import android.media.MediaPlayer

object AudioPlayer {
    private var player: MediaPlayer? = null
    private var currentUrl: String? = null

    fun toggle(url: String, onComplete: () -> Unit = {}): Boolean {
        if (player != null && currentUrl == url) {
            stop()
            return false
        }
        stop()
        currentUrl = url
        player = MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener { start() }
            setOnCompletionListener {
                stop()
                onComplete()
            }
        }
        return true
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) { }
        player = null
        currentUrl = null
    }

    fun isPlaying(url: String): Boolean =
        currentUrl == url && player?.isPlaying == true
}
