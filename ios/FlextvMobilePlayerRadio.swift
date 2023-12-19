@objc(FlextvMobilePlayerRadio)
class FlextvMobilePlayerRadio: NSObject {

  // @objc(multiply:withB:withResolver:withRejecter:)
  // func multiply(a: Float, b: Float, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
  //   resolve(a*b)
  // }

    private let MEDIA_STATE_PLAYING: String = "STATE_PLAYING"
    private let MEDIA_STATE_PAUSED: String = "STATE_PAUSED"
    private let MEDIA_STATE_STOPPED: String = "STATE_STOPPED"
    private let MEDIA_STATE_ERROR: String = "STATE_ERROR"
    private let MEDIA_STATE_BUFFERING: String = "STATE_BUFFERING"
    private let MEDIA_STATE_RATING_PERCENTAGE: String = "STATE_RATING_PERCENTAGE"
    private let MEDIA_SPEED: String = "speed"
    private let MEDIA_STATE: String = "state"
    private let MEDIA_DICT: Dictionary<String, Any> = [:]


    @objc func updatePlayback(originalDetails: NSDictionary) {
    
    }

    @objc func setNowPlaying(details: NSDictionary) {
//        MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
//        NSMutableDictionary *mediaDict = [NSMutableDictionary dictionary];
//
//
//        center.nowPlayingInfo = [self update:mediaDict with:details andSetDefaults:true];
//
//        NSString *artworkUrl = [self getArtworkUrl:[details objectForKey:@"artwork"]];
//        [self updateArtworkIfNeeded:artworkUrl];
        let center: MPNowPlayingInfoCenter = MPNowPlayingInfoCenter.default()
        print("center : \(center)")
    }
    
    @objc func resetNowPlaying() {
        print("resetNowPlaying!!!")
    }
    
    @objc func enableControl(controlName: NSString, enabled: Bool, options: NSDictionary) {
        
    }
    
    @objc func enableBackgroundMode(enabled: Bool) {
        
    }
    
    @objc func stopControl() {
        
    }
    
    @objc func observeAudioInterruptions(observe: Bool) {
        
    }
}
