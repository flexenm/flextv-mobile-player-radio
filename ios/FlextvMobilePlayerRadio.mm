#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(FlextvMobilePlayerRadio, NSObject)

// RCT_EXTERN_METHOD(multiply:(float)a withB:(float)b
//                  withResolver:(RCTPromiseResolveBlock)resolve
//                  withRejecter:(RCTPromiseRejectBlock)reject)

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

@end
