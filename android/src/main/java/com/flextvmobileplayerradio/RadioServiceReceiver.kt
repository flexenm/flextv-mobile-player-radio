package com.flextvmobileplayerradio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.flextvmobileplayerradio.`interface`.RadioIntents


class RadioServiceReceiver(
  private val reactContext: ReactApplicationContext,
  private val radioService: RadioService,
  private val radioServiceWrapper: RadioServiceWrapper
): BroadcastReceiver() {

  private var packageName: String = reactContext.packageName

  override fun onReceive(context: Context?, intent: Intent?) {
    intent?.let { intent ->
      Log.d(TAG, "RadioServiceReceiver intent.action=${intent.action}")
      when (intent.action) {
        RadioIntents.ACTION_REMOVE_NOTIFICATION -> {
          if (!checkApp(intent)) return

          // Removes the notification and deactivates the media session
          radioService.session.isActive = false
          radioServiceWrapper.stopService()

          // Notify react native
          val data: WritableMap = Arguments.createMap()
          data.putString("name", "closeNotification")
          reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("RNMusicControlEvent", data)
        }
        Intent.ACTION_MEDIA_BUTTON,
        RadioIntents.ACTION_MEDIA_BUTTON -> {
          if (!intent.hasExtra(Intent.EXTRA_KEY_EVENT)) return
          if (!checkApp(intent)) return

          // Dispatch media buttons to MusicControlListener
          // Copy of MediaButtonReceiver.handleIntent without action check
          val ke: KeyEvent? = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
          radioService.session.controller.dispatchMediaButtonEvent(ke)
        }
        AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
          radioService.session.controller.transportControls?.pause()
        }
        else -> {

        }
      }
    }
  }

  private fun checkApp(intent: Intent): Boolean {
    if (intent.hasExtra(RadioIntents.BUNDLE_KEY_PACKAGE_NAME)) {
      val name = intent.getStringExtra(RadioIntents.BUNDLE_KEY_PACKAGE_NAME)
      if (packageName != name) return false // This event is not for this package. We'll ignore it
    }
    return true
  }
}
