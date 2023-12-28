package com.flextvmobileplayerradio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.flextvmobileplayerradio.`interface`.RadioIntents
import com.flextvmobileplayerradio.utils.ReactNativeJson
import kotlinx.coroutines.*

const val TAG = "FMPR"
class FlextvMobilePlayerRadioModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private var notificationId: Int = 100
  private var channelId: String = ""

  private var isOftenPreventCheck = false
  private val WHAT_FOREGROUND_CHECK = 1
  private val WHAT_RE_FOREGROUND_CHECK = 2
  private var tempNowPlayingMetadata: String? = null

  private val checkForegroundHandler: Handler = object : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
      if (isOftenPreventCheck && msg.what != WHAT_RE_FOREGROUND_CHECK) {
        return
      }
      if (msg.what == WHAT_FOREGROUND_CHECK || msg.what == WHAT_RE_FOREGROUND_CHECK) {
        isOftenPreventCheck = true
        if (ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)
        ) {
          tempNowPlayingMetadata?.let {
            startService(it)
            tempNowPlayingMetadata = null
          }
        } else {
          val newMsg: Message = this.obtainMessage(WHAT_RE_FOREGROUND_CHECK)
          this.sendMessageDelayed(newMsg, 1000)
        }
      }
    }
  }

  var isPendingSendAction = false
  var serviceWrapper: RadioServiceWrapper? = null
  private var serviceHandler: ServiceHandler
  inner class ServiceHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
      Log.d(TAG, "ServiceHandler isPendingSendAction=$isPendingSendAction")
      if (!isPendingSendAction) return

      runBlocking {
        while (serviceWrapper == null && isPendingSendAction) { // 서비스가 바인드완료 되기전에 수신된 브릿지 처리를 위해 잡아주는기능
          Log.d(TAG, "ServiceHandler runBlocking 500ms")
          delay(500)
        }
      }

      if (isPendingSendAction) {
        serviceWrapper?.let { service ->
          (msg.obj as? Bundle)?.let {
            Log.d(TAG, "ServiceHandler sendAction() action=${it.getString(RadioIntents.BUNDLE_KEY_ACTION)}")
            service.sendAction(it)
          }
        }
      } else {
        Log.d(TAG, "ServiceHandler do not sendAction()")
      }
    }
  }

  private fun checkForeground() {
    checkForegroundHandler.removeMessages(WHAT_FOREGROUND_CHECK)
    val msg = Message()
    msg.what = WHAT_FOREGROUND_CHECK
    checkForegroundHandler.sendMessage(msg)
  }

  init {
    val handlerThread = HandlerThread("RadioServiceBindingProcess", THREAD_PRIORITY_BACKGROUND)
    handlerThread.start()
    serviceHandler = ServiceHandler(handlerThread.looper)
    FlextvMobilePlayerRadioModule.reactContext = reactContext
  }

  private fun sendActionToService(bundle: Bundle) {
    serviceHandler.sendMessage(serviceHandler.obtainMessage(0).apply { obj = bundle })
  }

  @Synchronized
  private fun startService(nowPlayingData: String) {
    Log.d(TAG, "FlextvMobilePlayerRadioModule startService()")
    if (!isPendingSendAction) {
      isPendingSendAction = true
      val startIntent = Intent(reactContext, RadioServiceWrapper::class.java).apply {
        putExtra(RadioIntents.BUNDLE_KEY_SET_NOW_PLAYING, nowPlayingData)
        putExtra(RadioIntents.BUNDLE_KEY_NOTIFICATION_ID, notificationId)
        putExtra(RadioIntents.BUNDLE_KEY_CHANNEL_ID, channelId)
      }
      ContextCompat.startForegroundService(reactContext, startIntent)
      val bindIntent = Intent(reactContext, RadioServiceWrapper::class.java)
      reactContext.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)
    }
  }

  private val connection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      Log.w(TAG, "onServiceConnected")
      // The binder of the service that returns the instance that is created.
      (service as? RadioServiceWrapper.LocalBinder)?.let { binder ->
        serviceWrapper = binder.getService()
      }

      // Release the connection to prevent leaks.
//      reactContext.unbindService(this)
    }

    override fun onBindingDied(name: ComponentName) {
      Log.w(TAG, "Binding has dead.")
    }

    override fun onNullBinding(name: ComponentName) {
      Log.w(TAG, "Bind was null.")
    }

    override fun onServiceDisconnected(name: ComponentName) {
      Log.w(TAG, "Service is disconnected..")
    }
  }

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  @Synchronized
  fun setNotificationIds(notificationId: Int, channelId: String) {
    Log.d(TAG, "setNotificationIds(notificationId=$notificationId, channelId=$channelId)")
    this.notificationId = notificationId
    this.channelId = channelId
  }

  @ReactMethod
  @Synchronized
  fun stopControl() {
    Log.d(TAG, "stopControl()")
    isPendingSendAction = false
    serviceHandler.removeMessages(0)
    serviceWrapper?.let {
      reactContext.unbindService(connection)
      it.stopService()
      serviceWrapper = null
    }
  }

  @Synchronized
  fun destroy() {
    stopControl()
  }

  @ReactMethod
  fun enableBackgroundMode(enable: Boolean) {
    Log.d(TAG, "enableBackgroundMode(enable=$enable)")
    // Nothing?
  }

  @ReactMethod
  fun observeAudioInterruptions(enable: Boolean) {
    Log.d(TAG, "observeAudioInterruptions(enable=$enable)")
    sendActionToService(Bundle().apply {
      putString(RadioIntents.BUNDLE_KEY_ACTION, RadioIntents.ACTION_OBSERVE_AUDIO_INTERRUPTIONS)
      putBoolean(RadioIntents.BUNDLE_KEY_ENABLE, enable)
    })
  }

  @ReactMethod
  @Synchronized
  fun setNowPlaying(metadata: ReadableMap) {
    Log.d(TAG, "setNowPlaying() metadata=$metadata")

    val jsonString = ReactNativeJson.convertMapToJson(metadata).toString()
    if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
      startService(jsonString)
    } else {
      tempNowPlayingMetadata = jsonString
      checkForeground()
    }
  }

  @ReactMethod
  @Synchronized
  fun updatePlayback(info: ReadableMap) {
    Log.d(TAG, "updatePlayback() info=$info")
    val jsonString = ReactNativeJson.convertMapToJson(info).toString()
    sendActionToService(Bundle().apply {
      putString(RadioIntents.BUNDLE_KEY_ACTION, RadioIntents.ACTION_UPDATE_PLAYBACK)
      putString(RadioIntents.BUNDLE_KEY_INFO, jsonString)
    })
  }

  @ReactMethod
  @Synchronized
  fun resetNowPlaying() {
    Log.d(TAG, "resetNowPlaying()")
    sendActionToService(Bundle().apply {
      putString(RadioIntents.BUNDLE_KEY_ACTION, RadioIntents.ACTION_RESET_NOW_PLAYING)
    })
  }

  @ReactMethod
  @Synchronized
  fun enableControl(control: String, enable: Boolean, options: ReadableMap) {
    Log.d(TAG, "enableControl() control=$control, enable=$enable, options=$options")
    sendActionToService(Bundle().apply {
      putString(RadioIntents.BUNDLE_KEY_ACTION, RadioIntents.ACTION_ENABLE_CONTROL)
      putString(RadioIntents.BUNDLE_KEY_CONTROL, control)
      putBoolean(RadioIntents.BUNDLE_KEY_ENABLE, enable)
    })
  }

  companion object {
    var reactContext: ReactApplicationContext? = null
    const val NAME = "FlextvMobilePlayerRadio"
  }
}
