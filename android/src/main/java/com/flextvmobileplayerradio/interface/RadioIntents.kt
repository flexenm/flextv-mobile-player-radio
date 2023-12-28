package com.flextvmobileplayerradio.`interface`

interface RadioIntents {
  companion object {
    const val ACTION_REMOVE_NOTIFICATION = "radio_service_remove_notification"
    const val ACTION_MEDIA_BUTTON = "radio_service_media_button"

    const val ACTION_ENABLE_CONTROL = "radio_service_enableControl"
    const val ACTION_OBSERVE_AUDIO_INTERRUPTIONS = "radio_service_observe_audio_interruptions"
    const val ACTION_RESET_NOW_PLAYING = "radio_service_reset_now_playing"
    const val ACTION_UPDATE_PLAYBACK = "radio_service_update_playback"

    const val BUNDLE_KEY_SET_NOW_PLAYING = "radio_service_set_now_playing"
    const val BUNDLE_KEY_PACKAGE_NAME = "radio_service_package_name"
    const val BUNDLE_KEY_NOTIFICATION_ID = "radio_service_notification_id"
    const val BUNDLE_KEY_CHANNEL_ID = "radio_service_channel_id"

    const val BUNDLE_KEY_ACTION = "radio_service_action"
    const val BUNDLE_KEY_CONTROL = "radio_service_control"
    const val BUNDLE_KEY_ENABLE = "radio_service_enable"
    const val BUNDLE_KEY_INFO = "radio_service_info"
  }
}
