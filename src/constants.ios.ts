import { NativeModules } from 'react-native'
const NativeFlexRadioControl = NativeModules.FlexRadioControlManager

const STATE_PLAYING = NativeFlexRadioControl.STATE_PLAYING
const STATE_PAUSED = NativeFlexRadioControl.STATE_PAUSED
const STATE_ERROR = NativeFlexRadioControl.STATE_ERROR
const STATE_STOPPED = NativeFlexRadioControl.STATE_STOPPED
const STATE_BUFFERING = NativeFlexRadioControl.STATE_BUFFERING
const RATING_HEART = NativeFlexRadioControl.RATING_HEART
const RATING_THUMBS_UP_DOWN = NativeFlexRadioControl.RATING_THUMBS_UP_DOWN
const RATING_3_STARS = NativeFlexRadioControl.RATING_3_STARS
const RATING_4_STARS = NativeFlexRadioControl.RATING_4_STARS
const RATING_5_STARS = NativeFlexRadioControl.RATING_5_STARS
const RATING_PERCENTAGE = NativeFlexRadioControl.RATING_PERCENTAGE

export default {
  STATE_PLAYING,
  STATE_PAUSED,
  STATE_ERROR,
  STATE_STOPPED,
  STATE_BUFFERING,
  RATING_HEART,
  RATING_THUMBS_UP_DOWN,
  RATING_3_STARS,
  RATING_4_STARS,
  RATING_5_STARS,
  RATING_PERCENTAGE
}