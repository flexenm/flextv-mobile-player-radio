package com.flextvmobileplayerradio.wakelock

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import com.flextvmobileplayerradio.TAG

class WakeLockManager(context: Context): Wakelocks {
  private val serviceWakelock =
    (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
      .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "reactVideoView:RadioServiceWrapper")
  private val wifiLock = (context.getSystemService(Context.WIFI_SERVICE) as WifiManager)
    .createWifiLock(WifiManager.WIFI_MODE_FULL, "musicservice:wifilock")

  override fun acquireServiceLock() {
    Log.d(TAG, "Acquired service wakelock")
    serviceWakelock.acquire()
    wifiLock.acquire()
  }

  override fun releaseServiceLock() {
    if (serviceWakelock.isHeld) {
      Log.d(TAG, "Released service wakelock")
      serviceWakelock.release()
      wifiLock.release()
    }
  }

  companion object {
    const val COUNT = "WakeLockManager.COUNT"
  }
}
