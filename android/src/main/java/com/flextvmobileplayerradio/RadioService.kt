package com.flextvmobileplayerradio

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.views.imagehelper.ResourceDrawableIdHelper
import com.flextvmobileplayerradio.`interface`.RadioIntents
import com.flextvmobileplayerradio.wakelock.Wakelocks
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLConnection


interface EnclosingService {
  fun startForeground(id: Int, notification: Notification)
  fun stopService()
}

class RadioService(private val context: ReactApplicationContext,
                   private val radioServiceWrapper: RadioServiceWrapper,
                   private val notificationId: Int,
                   private val channelId: String,
                   private val nowPlayingData: ReadableMap,
                   private val wakelocks: Wakelocks,
                   private val enclosing: EnclosingService) {

  var session: MediaSessionCompat
  private var artWorkJob: Job? = null
  private var md: MediaMetadataCompat.Builder
  private var pb: PlaybackStateCompat.Builder
  private var nb: NotificationCompat.Builder
  private var state: PlaybackStateCompat? = null

  private var volume: RadioVolumeListener
  private var receiver: RadioServiceReceiver
  private var emitter: RadioEventEmitter = RadioEventEmitter(context, radioServiceWrapper)
  private var afListener: RadioAudioFocusListener

  private var play: NotificationCompat.Action? = null
  private var pause: NotificationCompat.Action? = null
  private var stop: NotificationCompat.Action? = null

  private var smallIcon = 0
  private var customIcon = 0

  private var remoteVolume = false
  private var controls: Long = 0

  init {
    Log.d(TAG, "RadioService init{}")

    session = MediaSessionCompat(context, "RadioService").apply {
      setFlags(
        MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
          MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
      )

      setCallback(MediaSessionCallback(emitter))
    }

    volume = RadioVolumeListener(context, emitter, true, 100, 100)
    if (remoteVolume) {
      session.setPlaybackToRemote(volume)
    } else {
      session.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
    }

    afListener = RadioAudioFocusListener(context, emitter, volume)

    md = MediaMetadataCompat.Builder()
    pb = PlaybackStateCompat.Builder()
    pb.setActions(controls)

    createChannel(context)
    nb = NotificationCompat.Builder(context, channelId)
    nb.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    nb.priority = NotificationCompat.PRIORITY_HIGH

    updateNotificationMediaStyle()

    state = pb.build()

    updateActions(controls)

    smallIcon = context.resources.getIdentifier("music_control_icon", "drawable", context.packageName);
    if (smallIcon == 0) smallIcon = context.resources.getIdentifier("play", "drawable", context.packageName);

    val notification = prepareNotification(nb, isPlaying)
    enclosing.startForeground(notificationId, notification)
    setNowPlaying(nowPlayingData)

    wakelocks.acquireServiceLock()

    val filter = IntentFilter()
    filter.addAction(RadioIntents.ACTION_REMOVE_NOTIFICATION)
    filter.addAction(RadioIntents.ACTION_MEDIA_BUTTON)
    filter.addAction(Intent.ACTION_MEDIA_BUTTON)
    filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    receiver = RadioServiceReceiver(context, this, radioServiceWrapper)
    context.registerReceiver(receiver, filter)
  }

  private fun createChannel(context: ReactApplicationContext) {
    val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val mChannel = NotificationChannel(channelId, "Media playback", NotificationManager.IMPORTANCE_LOW)
    mChannel.description = "Radio controls"
    mChannel.setShowBadge(false)
    mChannel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
    mNotificationManager.createNotificationChannel(mChannel)
  }

  private fun setNowPlaying(metadata: ReadableMap) {
    artWorkJob?.cancel()

    val title = if (metadata.hasKey("title")) metadata.getString("title") else null
    val artist = if (metadata.hasKey("artist")) metadata.getString("artist") else null
    val album = if (metadata.hasKey("album")) metadata.getString("album") else null
    val genre = if (metadata.hasKey("genre")) metadata.getString("genre") else null
    val description =
      if (metadata.hasKey("description")) metadata.getString("description") else null
    val date = if (metadata.hasKey("date")) metadata.getString("date") else null
    val duration =
      if (metadata.hasKey("duration")) (metadata.getDouble("duration") * 1000).toLong() else 0
    val notificationColor =
      if (metadata.hasKey("color")) metadata.getInt("color") else NotificationCompat.COLOR_DEFAULT
    val isColorized =
      if (metadata.hasKey("colorized")) metadata.getBoolean("colorized") else !metadata.hasKey("color")
    val notificationIcon =
      if (metadata.hasKey("notificationIcon")) metadata.getString("notificationIcon") else null
    md.putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
    md.putText(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
    md.putText(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
    md.putText(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
    md.putText(
      MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
      description
    )
    md.putText(MediaMetadataCompat.METADATA_KEY_DATE, date)
    md.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
    nb.setContentTitle(title)
    nb.setContentText(artist)
    nb.setContentInfo(album)
    nb.color = notificationColor
    nb.setColorized(false)
    notificationIcon?.let {
      setCustomNotificationIcon(it)
    }
    if (metadata.hasKey("artwork")) {
      var artwork: String? = null
      var localArtwork = false
      if (metadata.getType("artwork") == ReadableType.Map) {
        artwork = metadata.getMap("artwork")!!.getString("uri")
        localArtwork = true
      } else {
        artwork = metadata.getString("artwork")
      }
      val artworkLocal = localArtwork
      artwork?.let { artworkUrl ->
        artWorkJob = CoroutineScope(Dispatchers.Default).launch {
          loadArtwork(artworkUrl, artworkLocal)?.let { bitmap ->
            session.controller?.metadata.let { currentMetadata ->
              val newBuilder =
                if (currentMetadata == null) MediaMetadataCompat.Builder() else MediaMetadataCompat.Builder(
                  currentMetadata
                )
              session.setMetadata(
                newBuilder.putBitmap(
                  MediaMetadataCompat.METADATA_KEY_ART,
                  bitmap
                ).build()
              )
            }
            // If enabled, Android 8+ "colorizes" the notification color by extracting colors from the artwork
            nb.setColorized(isColorized)
            nb.setLargeIcon(bitmap)
            show(nb, isPlaying)
            artWorkJob?.cancel()
            artWorkJob = null
          }
        }
      }
    } else {
      md.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, null)
    }
    session.setMetadata(md.build())
    session.isActive = true
    show(nb, isPlaying)
  }

  @Synchronized
  fun updatePlayback(info: ReadableMap) {
    Log.d(TAG, "RadioService updatePlayback() info=$info")
    state?.let { state ->
      val updateTime: Long
      val elapsedTime: Long
      var speed = if (info.hasKey("speed")) info.getDouble("speed").toFloat() else state.playbackSpeed
      val pbState = if (info.hasKey("state")) info.getInt("state") else state.state
      val maxVol = if (info.hasKey("maxVolume")) info.getInt("maxVolume") else volume.maxVolume
      val vol = if (info.hasKey("volume")) info.getInt("volume") else volume.currentVolume
      isPlaying = pbState == PlaybackStateCompat.STATE_PLAYING || pbState == PlaybackStateCompat.STATE_BUFFERING

      if (isPlaying && speed == 0F) {
        speed = 1F;
      }

      if(info.hasKey("elapsedTime")) {
        elapsedTime = (info.getDouble("elapsedTime") * 1000).toLong()
        updateTime = SystemClock.elapsedRealtime();
      } else {
        elapsedTime = state.position;
        updateTime = state.lastPositionUpdateTime;
      }

      pb.setState(pbState, elapsedTime, speed, updateTime)
      pb.setActions(controls)
      if (session.isActive) {
        show(nb, isPlaying)
      }
      this@RadioService.state = pb.build()
      session.setPlaybackState(this@RadioService.state)

      if(remoteVolume) {
        session.setPlaybackToRemote(volume.create(null, maxVol, vol))
      } else {
        session.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
      }
    }
  }

  @Synchronized
  fun enableControl(control: String?, enable: Boolean, options: ReadableMap?) {
    val controlValue = when (control) {
      "play" -> PlaybackStateCompat.ACTION_PLAY
      "pause" -> PlaybackStateCompat.ACTION_PAUSE
      "stop" -> PlaybackStateCompat.ACTION_STOP
      else -> return
    }
    controls = if (enable) {
      controls or controlValue
    } else {
      controls and controlValue.inv()
    }
    updateActions(controls)
    pb.setActions(controls)
    state = pb.build()
    session.setPlaybackState(state)
    updateNotificationMediaStyle()
    if (session.isActive) {
      show(nb, isPlaying)
    }
  }

  fun observeAudioInterruptions(enable: Boolean) {
    if (enable) {
      afListener.requestAudioFocus()
    } else {
      afListener.abandonAudioFocus()
    }
  }

  private fun updateNotificationMediaStyle() {
    Log.d(TAG, "updateNotificationMediaStyle() ")
    val style = androidx.media.app.NotificationCompat.MediaStyle()
    style.setMediaSession(session.sessionToken)
    var controlCount = 0
    if (hasControl(PlaybackStateCompat.ACTION_PLAY) || hasControl(PlaybackStateCompat.ACTION_PAUSE) || hasControl(
        PlaybackStateCompat.ACTION_PLAY_PAUSE
      )
    ) {
      controlCount += 1
    }
    if (hasControl(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)) {
      controlCount += 1
    }
    if (hasControl(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)) {
      controlCount += 1
    }
    if (hasControl(PlaybackStateCompat.ACTION_FAST_FORWARD)) {
      controlCount += 1
    }
    if (hasControl(PlaybackStateCompat.ACTION_REWIND)) {
      controlCount += 1
    }

    val actions = IntArray(controlCount)
    for (i in actions.indices) {
      actions[i] = i
    }

    style.setShowActionsInCompactView(*actions)
    nb.setStyle(style)
  }

  private fun hasControl(control: Long): Boolean {
    return controls and control == control
  }

  private fun loadArtwork(url: String, local: Boolean): Bitmap? {
    var bitmap: Bitmap? = null
    try {
      // If we are running the app in debug mode, the "local" image will be served from htt://localhost:8080, so we need to check for this case and load those images from URL
      if (local && !url.startsWith("http")) {
        // Gets the drawable from the RN's helper for local resources
        val helper = ResourceDrawableIdHelper.getInstance()
        val image: Drawable? = helper.getResourceDrawable(context, url)
        bitmap = if (image is BitmapDrawable) {
          (image as BitmapDrawable?)!!.bitmap
        } else {
          BitmapFactory.decodeFile(url)
        }
      } else {
        // Open connection to the URL and decodes the image
        val con: URLConnection = URL(url).openConnection()
        con.connect()
        val input: InputStream = con.getInputStream()
        bitmap = BitmapFactory.decodeStream(input)
        input.close()
      }
    } catch (ex: IOException) {
      Log.w(TAG, "Could not load the artwork", ex)
    } catch (ex: IndexOutOfBoundsException) {
      Log.w(TAG, "Could not load the artwork", ex)
    }
    return bitmap
  }

  @Synchronized
  fun setCustomNotificationIcon(resourceName: String) {
    customIcon = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    Log.d(TAG, "RadioService setCustomNotificationIcon() customIcon=$customIcon")
  }

  @Synchronized
  fun updateActions(mask: Long) {
    Log.d(TAG, "RadioService updateActions mask=$mask")
    Log.d(TAG, "RadioService ProcessLifecycleOwner.get().lifecycle.currentState=${ProcessLifecycleOwner.get().lifecycle.currentState}")

    play = createAction("play", "Play", mask, PlaybackStateCompat.ACTION_PLAY, play)
    pause = createAction("pause", "Pause", mask, PlaybackStateCompat.ACTION_PAUSE, pause)
    stop = createAction("stop", "Stop", mask, PlaybackStateCompat.ACTION_STOP, stop)
  }

  @SuppressLint("RestrictedApi")
  @Synchronized
  fun prepareNotification(builder: NotificationCompat.Builder, isPlaying: Boolean): Notification {
    Log.d(TAG, "RadioService prepareNotification isPlaying=$isPlaying")
    // Add the buttons
    builder.mActions.clear()
    Log.d(TAG, "RadioService prepareNotification play=$play, pause=$pause, stop=$stop")
    if (play != null && !isPlaying) {
      Log.d(TAG, "RadioService prepareNotification addAction(play)")
      builder.addAction(play)
    }
    if (pause != null && isPlaying) {
      Log.d(TAG, "RadioService prepareNotification addAction(pause)")
      builder.addAction(pause)
    }
    if (stop != null && isPlaying) {
      Log.d(TAG, "RadioService prepareNotification addAction(stop)")
      builder.addAction(stop)
    }

    builder.setOngoing(false)
    builder.setSmallIcon(if (customIcon != 0) customIcon else smallIcon)

    // Open the app when the notification is clicked
    val packageName = context.packageName
    val openApp = context.packageManager.getLaunchIntentForPackage(packageName)
    try {
      builder.setContentIntent(
        PendingIntent.getActivity(
          context,
          0,
          openApp,
          PendingIntent.FLAG_IMMUTABLE
        )
      )
    } catch (e: Exception) {
      println(e.message)
    }

    // Remove notification
    val remove = Intent(RadioIntents.ACTION_REMOVE_NOTIFICATION)
    remove.putExtra(RadioIntents.BUNDLE_KEY_PACKAGE_NAME, context.applicationInfo.packageName)
    builder.setDeleteIntent(
      PendingIntent.getBroadcast(
        context,
        0,
        remove,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )
    )
    return builder.build()
  }

  @Synchronized
  fun show(builder: NotificationCompat.Builder, isPlaying: Boolean) {
    Log.d(TAG, "show() notificationId=$notificationId, isPlaying=$isPlaying")
    NotificationManagerCompat.from(context).notify(notificationId, prepareNotification(builder, isPlaying))
//    enclosing.startForeground(notificationId, prepareNotification(builder, isPlaying))
  }

  private fun createAction(
    iconName: String,
    title: String,
    mask: Long,
    action: Long,
    oldAction: NotificationCompat.Action?
  ): NotificationCompat.Action? {
    if (mask and action == 0L) return null // When this action is not enabled, return null
    if (oldAction != null) return oldAction // If this action was already created, we won't create another instance

    // Finds the icon with the given name
    val packageName: String = context.packageName
    val icon = context.resources.getIdentifier(iconName, "drawable", packageName)

    // Creates the intent based on the action
    val keyCode = toKeyCode(action)
    val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
    intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    intent.putExtra(RadioIntents.BUNDLE_KEY_PACKAGE_NAME, packageName)
    val i = PendingIntent.getBroadcast(
      context,
      keyCode,
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    return NotificationCompat.Action(icon, title, i)
  }

  private fun toKeyCode(action: Long): Int {
    return when (action) {
      PlaybackStateCompat.ACTION_PLAY -> {
        KeyEvent.KEYCODE_MEDIA_PLAY
      }
      PlaybackStateCompat.ACTION_PAUSE -> {
        KeyEvent.KEYCODE_MEDIA_PAUSE
      }
      PlaybackStateCompat.ACTION_STOP -> {
        KeyEvent.KEYCODE_MEDIA_STOP
      }
      else -> {
        KeyEvent.KEYCODE_UNKNOWN
      }
    }
  }

  fun onDestroy() {
    Log.d(TAG, "RadioService onDestroy()")
    artWorkJob?.cancel()
    session.release()
    context.unregisterReceiver(receiver)
    wakelocks.releaseServiceLock()
    enclosing.stopService()
  }

  fun resetNowPlaying() {
    session.isActive = false
  }

  companion object {
    var isPlaying: Boolean = false
  }
}
