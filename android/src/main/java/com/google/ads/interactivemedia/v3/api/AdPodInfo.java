package com.google.ads.interactivemedia.v3.api;

public interface AdPodInfo {
    double getMaxDuration();
    double getTimeOffset();
    int getAdPosition();
    int getPodIndex();
    int getTotalAds();
    boolean isBumper();
}
