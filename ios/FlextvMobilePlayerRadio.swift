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
}
