#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(FlextvMobilePlayerRadio, NSObject)

RCT_EXTERN_METHOD(multiply:(float)a withB:(float)b
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(updatePlayback:(NSDictionary *) originalDetails)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
