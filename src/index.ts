// @ts-ignore
import {
  NativeModules,
  DeviceEventEmitter,
  NativeEventEmitter,
  Platform,
} from 'react-native';
// @ts-ignore
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';
// @ts-ignore
import constants from './constants';
import { Command } from './types';

export { Command };

const LINKING_ERROR =
  `The package 'flextv-mobile-player-radio' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const NativeFlexRadioControl = NativeModules.FlextvMobilePlayerRadio
  ? NativeModules.FlextvMobilePlayerRadio
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );
let handlers: { [key in Command]?: (value: any) => void } = {};
let listenerOfNativeMusicControl: any = null;
const IS_ANDROID = Platform.OS === 'android';
type TPlayingInfo = any;

const FlexRadioControl = {
  STATE_PLAYING: constants.STATE_PLAYING,
  STATE_PAUSED: constants.STATE_PAUSED,
  STATE_ERROR: constants.STATE_ERROR,
  STATE_STOPPED: constants.STATE_STOPPED,
  STATE_BUFFERING: constants.STATE_BUFFERING,

  RATING_HEART: constants.RATING_HEART,
  RATING_THUMBS_UP_DOWN: constants.RATING_THUMBS_UP_DOWN,
  RATING_3_STARS: constants.RATING_3_STARS,
  RATING_4_STARS: constants.RATING_4_STARS,
  RATING_5_STARS: constants.RATING_5_STARS,
  RATING_PERCENTAGE: constants.RATING_PERCENTAGE,

  enableBackgroundMode: function (enable: boolean) {
    NativeFlexRadioControl.enableBackgroundMode(enable);
  },
  setNowPlaying: function (info: TPlayingInfo) {
    // Check if we have an android asset from react style image require
    if (info.artwork) {
      info.artwork = resolveAssetSource(info.artwork) || info.artwork;
    }

    NativeFlexRadioControl.setNowPlaying(info);
  },
  setPlayback: function (info: TPlayingInfo): void {
    // Backwards compatibility. Use updatePlayback instead.
    NativeFlexRadioControl.updatePlayback(info);
  },
  updatePlayback: function (info: TPlayingInfo): void {
    NativeFlexRadioControl.updatePlayback(info);
  },
  resetNowPlaying: function () {
    NativeFlexRadioControl.resetNowPlaying();
  },
  enableControl: function (controlName: string, enable: boolean, options = {}) {
    NativeFlexRadioControl.enableControl(controlName, enable, options || {});
  },
  handleCommand: function (commandName: Command, value: any) {
    if (handlers[commandName]) {
      //@ts-ignore
      handlers[commandName](value);
    }
  },
  setNotificationId: function (notificationId: any, channelId: any) {
    if (IS_ANDROID) {
      NativeFlexRadioControl.setNotificationIds(notificationId, channelId);
    }
  },
  on: function (actionName: Command, cb: (value: any) => void) {
    if (!listenerOfNativeMusicControl) {
      listenerOfNativeMusicControl = (
        IS_ANDROID
          ? DeviceEventEmitter
          : new NativeEventEmitter(NativeFlexRadioControl)
      ).addListener('RNMusicControlEvent', (event) => {
        FlexRadioControl.handleCommand(event.name, event.value);
      });
    }
    handlers[actionName] = cb;
  },
  off: function (actionName: Command): void {
    delete handlers[actionName];
    if (!Object.keys(handlers).length && listenerOfNativeMusicControl) {
      listenerOfNativeMusicControl.remove();
      listenerOfNativeMusicControl = null;
    }
  },
  stopControl: function (): void {
    if (listenerOfNativeMusicControl) {
      listenerOfNativeMusicControl.remove();
      listenerOfNativeMusicControl = null;
    }
    Object.keys(handlers).map((key) => {
      //@ts-ignore
      delete handlers[key];
    });
    NativeFlexRadioControl.stopControl();
  },
  handleAudioInterruptions: function (enable: boolean): void {
    NativeFlexRadioControl.observeAudioInterruptions(enable);
  },
};

export default FlexRadioControl;

// export function multiply(a: number, b: number): Promise<number> {
//   return FlextvMobilePlayerRadio.multiply(a, b);
// }
