package com.flextvmobileplayerradio

import android.support.v4.media.session.MediaSessionCompat
import android.util.Log

class MediaSessionCallback internal constructor(private val emitter: RadioEventEmitter) :
  MediaSessionCompat.Callback() {
  override fun onPlay() {
    Log.d(TAG, "MediaSessionCallback onPlay()")
    emitter.onPlay()
  }

  override fun onPause() {
    Log.d(TAG, "MediaSessionCallback onPause()")
    emitter.onPause()
  }
}
