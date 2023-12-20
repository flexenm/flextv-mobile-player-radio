import MediaPlayer

@objc(FlextvMobilePlayerRadio)
class FlextvMobilePlayerRadio: RCTEventEmitter {
    
    public static var shared: FlextvMobilePlayerRadio?
    public let FLEX_RADIO_CONTROL_EVENT_NAME: String = "FlexRadioControlEvent"
    
    private var artworkUrl: String = ""
    private var audioInterruptionsObserved: Bool = false

    private let MEDIA_STATE_PLAYING: String = "STATE_PLAYING"
    private let MEDIA_STATE_PAUSED: String = "STATE_PAUSED"
    private let MEDIA_STATE_STOPPED: String = "STATE_STOPPED"
    private let MEDIA_STATE_ERROR: String = "STATE_ERROR"
    private let MEDIA_STATE_BUFFERING: String = "STATE_BUFFERING"
    private let MEDIA_STATE_RATING_PERCENTAGE: String = "STATE_RATING_PERCENTAGE"
    private let MEDIA_SPEED: String = "speed"
    private let MEDIA_STATE: String = "state"
    private let MEDIA_DICT: Dictionary<String, Any> = [:]

    
    override init() {
        super.init()
        
        FlextvMobilePlayerRadio.shared = self
        
        DispatchQueue.main.async {
            NotificationCenter.default.addObserver(self, selector: #selector(self.audioHardwareRouteChanged(_:)), name: AVAudioSession.routeChangeNotification, object: nil)
            UIApplication.shared.beginReceivingRemoteControlEvents()
        }
    }
    
    override func supportedEvents() -> [String]! {
        return [FLEX_RADIO_CONTROL_EVENT_NAME]
    }
    
    deinit {
        stop()
        NotificationCenter.default.removeObserver(self, name: AVAudioSession.routeChangeNotification, object: nil)
    }
    
    @objc func audioHardwareRouteChanged(_ notification: Notification) {
        let routeChangeReason = (notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? NSNumber)?.intValue ?? 0
        if routeChangeReason == AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue {
            //headphones unplugged or bluetooth device disconnected, iOS will pause audio
            
        }
    }

    @objc func updatePlayback(originalDetails: NSDictionary) {
    
    }
    
    @objc func setNowPlaying(_ details: NSDictionary) {
        Console.d("details : \(details)")
        let artwork = details["artwork"] as? String ?? ""
        let title = details["title"] as? String ?? ""
        let artist = details["artist"] as? String ?? ""
        
        let center = MPNowPlayingInfoCenter.default()
        var nowPlayingInfo = center.nowPlayingInfo ?? [String: Any]()
        
        nowPlayingInfo[MPMediaItemPropertyTitle] = title
        nowPlayingInfo[MPMediaItemPropertyArtist] = artist
        
        center.nowPlayingInfo = nowPlayingInfo
        
        self.updateArtworkIfNeeded(artwork)
    }
    
    @objc func resetNowPlaying() {
        Console.d("resetNowPlaying!!!")
        let center = MPNowPlayingInfoCenter.default()
        center.nowPlayingInfo = nil
        self.artworkUrl = ""
    }
    
    @objc func enableControl(_ controlName: NSString, enabled: Bool, options: NSDictionary?) {
      Console.d("controlName : \(controlName), enabled : \(enabled), options : \(options)")
        let remoteCenter = MPRemoteCommandCenter.shared()
        
        if controlName == "play" {              // 재생
            // TODO: - enabled 추가 처리
            remoteCenter.playCommand.addTarget { (_) -> MPRemoteCommandHandlerStatus in
                // TODO: play action
                Console.d("playCommand...")
                self.sendEventName(event: "play")
                return .success
            }
        } else if controlName == "pause" {      // 일시정지
            // TODO: - enabled 추가 처리
            remoteCenter.pauseCommand.addTarget { (_) -> MPRemoteCommandHandlerStatus in
                // TODO: pause action
                Console.d("pauseCommand...")
                self.sendEventName(event: "pause")
                return .success
            }
            
        } else {
            // TODO: - add control
        }
    }
    
    @objc func enableBackgroundMode(enabled: Bool) {
        
    }
    
    @objc func stopControl() {
        stop()
    }
    
    @objc func observeAudioInterruptions(_ observe: Bool) {
        if (self.audioInterruptionsObserved == observe) {
            return;
        }
        
        if observe {
            NotificationCenter.default.addObserver(self, selector: #selector(audioInterrupted(_:)), name: AVAudioSession.interruptionNotification, object: nil)
        } else {
            NotificationCenter.default.removeObserver(self, name: AVAudioSession.interruptionNotification, object: nil)
        }
        self.audioInterruptionsObserved = observe
    }
    
    @objc func audioInterrupted(_ notification: Notification) {
        if !self.audioInterruptionsObserved {
                return
        }
        
        let interruptionType = notification.userInfo![AVAudioSessionInterruptionTypeKey] as! AVAudioSession.InterruptionType
        
        if interruptionType == .began {                 // 전화 통화 시작 시 발생
            sendEventName(event: "pause")
            
        } else {
            let interruptionOption = notification.userInfo![AVAudioSessionInterruptionOptionKey] as! AVAudioSession.InterruptionOptions
            
            if interruptionOption == .shouldResume {    // 전화 통화 종료 시 발생
                sendEventName(event: "play")
            }
        }
    }
    
    func updateArtworkIfNeeded(_ artworkUrl: String?) {
        Console.d("artworkUrl : \(artworkUrl)")
        if artworkUrl == nil {
            return
        }
        
        let center = MPNowPlayingInfoCenter.default()
        if (artworkUrl == self.artworkUrl) && center.nowPlayingInfo?[MPMediaItemPropertyArtwork] != nil {
            return
        }
        
        self.artworkUrl = artworkUrl!
        
        guard let _artworkUrl = artworkUrl else { return }
        
        // Custom handling of artwork in another thread, will be loaded async
        DispatchQueue.global(qos: .default).async {
            var image: UIImage?
            
            // artwork is url download from the interwebs
            if _artworkUrl.hasPrefix("http://") || _artworkUrl.hasPrefix("https://") {
                let imageURL = URL(string: _artworkUrl)
                var imageData: Data?
                
                if let imageURL {
                    imageData = try? Data(contentsOf: imageURL)
                }
                if let imageData {
                    image = UIImage(data: imageData)
                }
                
            } else {
                let localArtworkUrl = _artworkUrl.replacingOccurrences(of: "file://", with: "")
                let fileExists = FileManager.default.fileExists(atPath: localArtworkUrl)
                if fileExists {
                    image = UIImage(named: localArtworkUrl)
                }
            }
            
            if image == nil {
                return
            }

            // check whether image is loaded
            let cgref = image!.cgImage
            let cim = image!.ciImage

            if (cim == nil && cgref == nil) {
                return
            }
            
            DispatchQueue.main.async {
                // Check if URL wasn't changed in the meantime
                if _artworkUrl != self.artworkUrl {
                    return
                }

                let center = MPNowPlayingInfoCenter.default()
                let artwork = MPMediaItemArtwork(boundsSize: image!.size, requestHandler: { (size) -> UIImage in
                    return image!
                })
                var mediaDict = (center.nowPlayingInfo != nil) ? center.nowPlayingInfo : [:]
                mediaDict?[MPMediaItemPropertyArtwork] = artwork
                center.nowPlayingInfo = mediaDict
            }
        }
    }
    
    func stop() {
        var remoteCenter = MPRemoteCommandCenter.shared()
        sendEventName(event: "pause")
        resetNowPlaying()
        observeAudioInterruptions(false)
        NotificationCenter.default.removeObserver(self, name: AVAudioSession.routeChangeNotification, object: nil)
    }
    
    private func sendEventName(event: String) {
        self.sendEvent(withName: FLEX_RADIO_CONTROL_EVENT_NAME, body: ["name" : event])
    }
}
