package com.flextvmobileplayerradio

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule

class RadioEventEmitter internal constructor(
  private val context: ReactApplicationContext,
  private val radioServiceWrapper: RadioServiceWrapper
) {
  fun onPlay() {
    sendEvent(context, "play", null)
  }

  fun onPause() {
    sendEvent(context, "pause", null)
  }

  fun onStop() {
    stopForegroundService()
    sendEvent(context, "stop", null)
  }

  fun onSkipToNext() {
    sendEvent(context, "nextTrack", null)
  }

  fun onSkipToPrevious() {
    sendEvent(context, "previousTrack", null)
  }

  fun onSeekTo(pos: Long) {
    sendEvent(context, "seek", pos / 1000.0)
  }

  fun onFastForward() {
    sendEvent(context, "skipForward", null)
  }

  fun onRewind() {
    sendEvent(context, "skipBackward", null)
  }

  fun onSetRating(rating: Float) {
    sendEvent(context, "setRating", rating)
  }

  fun onSetRating(hasHeartOrThumb: Boolean) {
    sendEvent(context, "setRating", hasHeartOrThumb)
  }

  fun onVolumeChange(volume: Int) {
    sendEvent(context, "volume", volume)
  }

  private fun stopForegroundService() {
    radioServiceWrapper.stopService()
  }

  companion object {
    private fun sendEvent(context: ReactApplicationContext, type: String, value: Any?) {
      val data = Arguments.createMap()
      data.putString("name", type)
      if (value != null) {
        if (value is Double || value is Float) {
          data.putDouble("value", value as Double)
        } else if (value is Boolean) {
          data.putBoolean("value", value)
        } else if (value is Int) {
          data.putInt("value", value)
        }
      }
      context.getJSModule(
        DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
      ).emit("FlexRadioControlEvent", data)
    }
  }
}
