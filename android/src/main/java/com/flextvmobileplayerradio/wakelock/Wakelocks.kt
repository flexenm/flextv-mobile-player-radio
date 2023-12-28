package com.flextvmobileplayerradio.wakelock

interface Wakelocks {
  fun acquireServiceLock()

  fun releaseServiceLock()
}
