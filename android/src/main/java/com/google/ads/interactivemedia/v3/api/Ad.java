package com.google.ads.interactivemedia.v3.api;

public interface Ad {
    String getAdId();
    String getAdSystem();
    String getAdvertiserName();
    String getContentType();
    String getCreativeAdId();
    String getCreativeId();
    String getDealId();
    String getDescription();
    String getSurveyUrl();
    String getTitle();
    String getTraffickingParameters();
    double getDuration();
    double getSkipTimeOffset();
    int getHeight();
    int getVastMediaBitrate();
    int getVastMediaHeight();
    int getVastMediaWidth();
    int getWidth();
    boolean isLinear();
    boolean isSkippable();
    boolean isUiDisabled();
    AdPodInfo getAdPodInfo();
    UniversalAdId[] getUniversalAdIds();
    String[] getAdWrapperCreativeIds();
    String[] getAdWrapperIds();
    String[] getAdWrapperSystems();
}
