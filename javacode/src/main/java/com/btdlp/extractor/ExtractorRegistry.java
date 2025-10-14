package com.btdlp.extractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry for managing information extractors
 */
public class ExtractorRegistry {
    private List<InfoExtractor> extractors;

    public ExtractorRegistry() {
        this.extractors = new ArrayList<>();
        registerDefaultExtractors();
    }

    private void registerDefaultExtractors() {
        // Register default extractors
        try {
            // Register all available extractors
            register(new com.btdlp.extractor.facebook.FinalFacebookExtractor());
            register(new com.btdlp.extractor.instagram.InstagramExtractor());
            register(new com.btdlp.extractor.tiktok.AdvancedTikTokExtractor());
            register(new com.btdlp.extractor.twitter.AdvancedTwitterExtractor());
            register(new com.btdlp.extractor.vimeo.AdvancedVimeoExtractor());
            register(new com.btdlp.extractor.dailymotion.AdvancedDailymotionExtractor());
            register(new com.btdlp.extractor.pornhub.AdvancedPornhubExtractor());
            register(new com.btdlp.extractor.xhamster.AdvancedXHamsterExtractor());
            register(new com.btdlp.extractor.xvideos.AdvancedXVideosExtractor());
            register(new com.btdlp.extractor.xnxx.AdvancedXNXXExtractor());
            register(new com.btdlp.extractor.m3u8.M3U8Extractor());
            // register(new BubeExtractor());
        } catch (Exception e) {
            // Log error but continue with available extractors
            System.err.println("Error registering extractors: " + e.getMessage());
        }
    }

    public void register(InfoExtractor extractor) {
        extractors.add(extractor);
    }

    public InfoExtractor getExtractor(String url) {
        for (InfoExtractor extractor : extractors) {
            if (extractor.suitable(url)) {
                return extractor;
            }
        }
        return null;
    }

    public List<InfoExtractor> getAllExtractors() {
        return new ArrayList<>(extractors);
    }

}
