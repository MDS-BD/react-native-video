package com.google.ads.interactivemedia.v3.api;

import java.util.Map;

public abstract class AdEvent {
    public abstract AdEventType getType();
    public abstract Map<String, String> getAdData();
    public abstract Ad getAd();

    public enum AdEventType {
        AD_PROGRESS,
        CLICKED
    }

    public interface AdEventListener {
        public void onAdEvent(AdEvent adEvent);
    }
}
