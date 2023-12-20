#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(FlextvMobilePlayerRadio, RCTEventEmitter)

RCT_EXTERN_METHOD(updatePlayback:(NSDictionary *) originalDetails)
RCT_EXTERN_METHOD(setNowPlaying:(NSDictionary *) details)
RCT_EXTERN_METHOD(resetNowPlaying)
RCT_EXTERN_METHOD(enableControl:(NSString *) controlName enabled:(BOOL) enabled options:(NSDictionary *)options)
RCT_EXTERN_METHOD(enableBackgroundMode:(BOOL) enabled)
RCT_EXTERN_METHOD(stopControl)
RCT_EXTERN_METHOD(observeAudioInterruptions:(BOOL) observe)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"FlexRadioControlEvent"];
}

@end
