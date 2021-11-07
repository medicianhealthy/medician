package com.robinzon.madician.ads.interfaces;

public interface AdsInitializeCallBack {
    public enum AdsInitializeState{
        ALL_NETWORKS_READY,
        SOME_NETWORKS_READY,
        NO_NETWORKS_ARE_READY
    }
    public void onAdsInitialized(final AdsInitializeState adsInitializeState);

}
