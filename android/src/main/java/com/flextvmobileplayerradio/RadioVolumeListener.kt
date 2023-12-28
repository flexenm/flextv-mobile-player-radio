package com.flextvmobileplayerradio

import android.content.Context
import androidx.media.VolumeProviderCompat


class RadioVolumeListener : VolumeProviderCompat {
  private var context: Context
  private var emitter: RadioEventEmitter

  constructor (
    context: Context,
    emitter: RadioEventEmitter,
    changeable: Boolean,
    maxVolume: Int,
    currentVolume: Int
  ) : super( if (changeable) VOLUME_CONTROL_ABSOLUTE else VOLUME_CONTROL_FIXED, maxVolume, currentVolume)
  {
    this.context = context
    this.emitter = emitter
  }

  fun isChangeable(): Boolean {
    return volumeControl != VOLUME_CONTROL_FIXED
  }

  override fun onSetVolumeTo(volume: Int) {
    currentVolume = volume
    emitter.onVolumeChange(volume)
  }

  override fun onAdjustVolume(direction: Int) {
    val maxVolume = maxVolume
    val tick = direction * (maxVolume / 10)
    val volume = Math.max(Math.min(currentVolume + tick, maxVolume), 0)
    currentVolume = volume
    emitter.onVolumeChange(volume)
  }

  fun create(
    changeable: Boolean?,
    maxVolume: Int?,
    currentVolume: Int?
  ): RadioVolumeListener? {
    var changeable = changeable
    var maxVolume = maxVolume
    var currentVolume = currentVolume
    if (currentVolume == null) {
      currentVolume = getCurrentVolume()
    } else {
      setCurrentVolume(currentVolume)
    }
    if (changeable == null) changeable = isChangeable()
    if (maxVolume == null) maxVolume = getMaxVolume()
    return if (changeable == isChangeable() && maxVolume === getMaxVolume()) this else RadioVolumeListener(
      context,
      emitter,
      changeable,
      maxVolume,
      currentVolume
    )
  }

}
