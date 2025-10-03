package com.example.tubedown.rereads;

import android.view.View;

import androidx.annotation.IdRes;

import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder;

public class ApproveNativeBuilder extends MaxNativeAdViewBinder.Builder {
    public ApproveNativeBuilder(View view) {
        super(view);
    }

    public ApproveNativeBuilder(int i) {
        super(i);
    }

    public MaxNativeAdViewBinder.Builder setIconContentView(@IdRes int id) {
        super.setIconContentViewId(id);
        return this;
    }

    @Deprecated
    protected MaxNativeAdViewBinder.Builder setOptionsContentFrameLayout(@IdRes int id) {
        super.setOptionsContentFrameLayoutId(id);
        return this;
    }

    @Deprecated
    protected MaxNativeAdViewBinder.Builder setMediaContentFrameLayout(@IdRes int id) {
        super.setMediaContentFrameLayoutId(id);
        return this;
    }
}
