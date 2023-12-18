import {
  requireNativeComponent,
  UIManager,
  Platform,
  type ViewStyle,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-flextv-mobile-player-radio' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

type FlextvMobilePlayerRadioProps = {
  color: string;
  style: ViewStyle;
};

const ComponentName = 'FlextvMobilePlayerRadioView';

export const FlextvMobilePlayerRadioView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<FlextvMobilePlayerRadioProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };
