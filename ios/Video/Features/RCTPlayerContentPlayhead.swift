#if USE_GOOGLE_IMA
import Foundation
import GoogleInteractiveMediaAds

// MARK: - RCTPlayerContentPlayhead

/// Custom IMAContentPlayhead to avoid depending on player instantiation
class RCTPlayerContentPlayhead: NSObject, IMAContentPlayhead {
    
    private weak var _video: RCTVideo?
    
    init(video: RCTVideo) {
        super.init()
        self._video = video
    }
    
    deinit {
        release()
    }
    
    /// Current content time (in seconds)
    var currentTime: TimeInterval {
        guard
            let player = _video
        else {
            return 0
        }
        
        return player.trackingTime
    }
    
    /// Release references
    func release(){
        self._video = nil
    }
}
#endif
