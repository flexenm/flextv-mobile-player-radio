import { NativeModules } from 'react-native'
const NativeFlexRadioControl = NativeModules.FlexRadioControlManager

const STATE_PLAYING = NativeFlexRadioControl.STATE_PLAYING
const STATE_PAUSED = NativeFlexRadioControl.STATE_PAUSED
const STATE_ERROR = NativeFlexRadioControl.STATE_PAUSED
const STATE_STOPPED = NativeFlexRadioControl.STATE_PAUSED
const STATE_BUFFERING = NativeFlexRadioControl.STATE_PAUSED
const RATING_HEART = 0
const RATING_THUMBS_UP_DOWN = 0
const RATING_3_STARS = 0
const RATING_4_STARS = 0
const RATING_5_STARS = 0
const RATING_PERCENTAGE = 0

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
