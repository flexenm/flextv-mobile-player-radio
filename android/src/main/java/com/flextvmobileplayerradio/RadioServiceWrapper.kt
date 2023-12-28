package com.flextvmobileplayerradio

import android.app.Notification
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import com.flextvmobileplayerradio.`interface`.RadioIntents
import com.flextvmobileplayerradio.utils.ReactNativeJson
import com.flextvmobileplayerradio.wakelock.WakeLockManager
import org.json.JSONObject
import java.lang.ref.WeakReference


class RadioServiceWrapper : MediaBrowserServiceCompat() {
  private val binder = LocalBinder()
  var nowPlayingData: String? = null
  var notificationId: Int = 100
  var channelId: String? = null

  class LocalBinder : Binder() {
    private var weakService: WeakReference<RadioServiceWrapper>? = null

    /**
     * Inject service instance to weak reference.
     */
    fun onBind(service: RadioServiceWrapper?) {
      Log.d(TAG, "LocalBinder onBind()")
      weakService = WeakReference(service)
    }

    fun getService(): RadioServiceWrapper? {
      Log.d(TAG, "LocalBinder getService()")
      return weakService?.get()
    }
  }

  override fun onBind(intent: Intent?): IBinder {
    Log.d(TAG, "RadioServiceWrapper onBind()")
    binder.onBind(this)
    return binder
  }

  override fun onGetRoot(
    clientPackageName: String,
    clientUid: Int,
    rootHints: Bundle?
  ): BrowserRoot? {
    return null
  }

  override fun onLoadChildren(
    parentId: String,
    result: Result<MutableList<MediaBrowserCompat.MediaItem>>
  ) {
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "RadioServiceWrapper onStartCommand()")

    intent?.let {
      nowPlayingData = it.getStringExtra(RadioIntents.BUNDLE_KEY_SET_NOW_PLAYING)
      notificationId = it.getIntExtra(RadioIntents.BUNDLE_KEY_NOTIFICATION_ID, notificationId)
      channelId = it.getStringExtra(RadioIntents.BUNDLE_KEY_CHANNEL_ID)
    }
    Log.d(TAG, "RadioServiceWrapper nowPlayingData=$nowPlayingData, notificationId=$notificationId, channelId=$channelId")
    if (!nowPlayingData.isNullOrEmpty() && !channelId.isNullOrEmpty()) {
      if (radioService == null) {
        radioService = FlextvMobilePlayerRadioModule.reactContext?.let {
          RadioService(
            context = it,
            radioServiceWrapper = this,
            notificationId = notificationId,
            channelId = channelId!!,
            nowPlayingData = ReactNativeJson.convertJsonToMap(JSONObject(nowPlayingData!!)),
            wakelocks = wakelocks,
            enclosing = object: EnclosingService {
              override fun startForeground(id: Int, notification: Notification) {
                Log.d(TAG, "RadioServiceWrapper startForeground(id=$id, notification=$notification")
                this@RadioServiceWrapper.startForeground(id, notification)
              }

              override fun stopService() {
                Log.d(TAG, "RadioServiceWrapper stopService()")
                this@RadioServiceWrapper.stopService()
              }
            }
          )
        }
      }
    } else {
      stopService()
    }
    return START_NOT_STICKY
  }

  fun stopService() {
    radioService?.session?.release()

    stopForeground(STOP_FOREGROUND_REMOVE)
    FlextvMobilePlayerRadioModule.reactContext?.let {
      NotificationManagerCompat.from(it).cancel(notificationId)
    }
    stopSelf()
  }

  fun sendAction(actionBundle: Bundle) {
    Log.d(TAG, "RadioServiceWrapper sendAction=${actionBundle.getString(RadioIntents.BUNDLE_KEY_ACTION)}")
    when (actionBundle.getString(RadioIntents.BUNDLE_KEY_ACTION)) {
      RadioIntents.ACTION_ENABLE_CONTROL -> {
        val control = actionBundle.getString(RadioIntents.BUNDLE_KEY_CONTROL)
        val enable = actionBundle.getBoolean(RadioIntents.BUNDLE_KEY_ENABLE)
        Log.d(TAG, "enableControl control=$control enable=$enable")
        radioService?.enableControl(control, enable, null)
      }
      RadioIntents.ACTION_OBSERVE_AUDIO_INTERRUPTIONS -> {
        val enable = actionBundle.getBoolean(RadioIntents.BUNDLE_KEY_ENABLE)
        Log.d(TAG, "observeAudioInterruptions enable=$enable")
        radioService?.observeAudioInterruptions(enable)
      }
      RadioIntents.ACTION_RESET_NOW_PLAYING -> {
        radioService?.resetNowPlaying()
      }
      RadioIntents.ACTION_UPDATE_PLAYBACK -> {
        val info = actionBundle.getString(RadioIntents.BUNDLE_KEY_INFO)
        Log.d(TAG, "updatePlayback info=$info")
        info?.let {
          radioService?.updatePlayback(ReactNativeJson.convertJsonToMap(JSONObject(it)))
        }
      }

      else -> {
        Log.d(TAG, "do nothing!")
      }
    }
  }

  override fun onDestroy() {
    radioService?.onDestroy()
    radioService = null
  }

  private lateinit var wakelocks: WakeLockManager
  private var radioService: RadioService? = null
  private var notification: Notification? = null

  override fun onCreate() {
    Log.d(TAG, "RadioServiceWrapper onCreate()")
    wakelocks = WakeLockManager(this)
  }
}
