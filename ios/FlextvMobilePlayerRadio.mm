#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(FlextvMobilePlayerRadio, NSObject)

// RCT_EXTERN_METHOD(multiply:(float)a withB:(float)b
//                  withResolver:(RCTPromiseResolveBlock)resolve
//                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(updatePlayback:(NSDictionary *) originalDetails)
RCT_EXPORT_METHOD(setNowPlaying:(NSDictionary *) details)
RCT_EXPORT_METHOD(resetNowPlaying)
RCT_EXPORT_METHOD(enableControl:(NSString *) controlName enabled:(BOOL) enabled options:(NSDictionary *)options)
RCT_EXPORT_METHOD(enableBackgroundMode:(BOOL) enabled)
RCT_EXPORT_METHOD(stopControl)
RCT_EXPORT_METHOD(observeAudioInterruptions:(BOOL) observe)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
