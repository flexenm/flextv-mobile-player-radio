package com.flextvmobileplayerradio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext


class RadioAudioFocusListener: AudioManager.OnAudioFocusChangeListener {

  private val emitter: RadioEventEmitter
  private val volume: RadioVolumeListener

  private val mAudioManager: AudioManager
  private var mFocusRequest: AudioFocusRequest? = null

  private var mPlayOnAudioFocus = false

  constructor(
    context: ReactApplicationContext,
    emitter: RadioEventEmitter,
    volume: RadioVolumeListener
  ) : super()
  {
    this.emitter = emitter
    this.volume = volume

    mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }

  override fun onAudioFocusChange(focusChange: Int) {
    when (focusChange) {
      AudioManager.AUDIOFOCUS_LOSS -> {
        Log.d(TAG, "RadioAudioFocusListener AudioManager.AUDIOFOCUS_LOSS")
        abandonAudioFocus()
        mPlayOnAudioFocus = false;
        emitter.onStop();
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
        Log.d(TAG, "RadioAudioFocusListener AudioManager.AUDIOFOCUS_LOSS_TRANSIENT")
        if (RadioService.isPlaying) {
          mPlayOnAudioFocus = true;
          emitter.onPause();
        }
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        Log.d(TAG, "RadioAudioFocusListener AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
        volume.currentVolume = 40;
      }
      AudioManager.AUDIOFOCUS_GAIN -> {
        Log.d(TAG, "RadioAudioFocusListener AudioManager.AUDIOFOCUS_GAIN")
        if (volume.currentVolume != 100) {
          volume.currentVolume = 100;
        }
        if (mPlayOnAudioFocus) {
          emitter.onPlay();
        }
        mPlayOnAudioFocus = false;
      }
    }
  }

  fun requestAudioFocus() {
    Log.d(TAG, "RadioAudioFocusListener requestAudioFocus()")
    mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
      .setOnAudioFocusChangeListener(this).build().also {
        mAudioManager.requestAudioFocus(it)
      }
  }

  fun abandonAudioFocus() {
    Log.d(TAG, "RadioAudioFocusListener abandonAudioFocus()")
    if (mFocusRequest != null) {
      mFocusRequest?.let {
        mAudioManager.abandonAudioFocusRequest(it)
      }
    } else {
      mAudioManager.abandonAudioFocus(this)
    }
  }
}
