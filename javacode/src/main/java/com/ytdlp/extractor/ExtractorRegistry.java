package com.ytdlp.extractor;

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
            register(new com.ytdlp.extractor.facebook.FinalFacebookExtractor());
            register(new com.ytdlp.extractor.instagram.InstagramExtractor());
            register(new com.ytdlp.extractor.tiktok.AdvancedTikTokExtractor());
            register(new com.ytdlp.extractor.twitter.AdvancedTwitterExtractor());
            register(new com.ytdlp.extractor.vimeo.AdvancedVimeoExtractor());
            register(new com.ytdlp.extractor.dailymotion.AdvancedDailymotionExtractor());
            register(new com.ytdlp.extractor.pornhub.AdvancedPornhubExtractor());
            register(new com.ytdlp.extractor.xhamster.AdvancedXHamsterExtractor());
            register(new com.ytdlp.extractor.xvideos.AdvancedXVideosExtractor());
            register(new com.ytdlp.extractor.xnxx.AdvancedXNXXExtractor());
            register(new YouTubeExtractor());
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

    // Simple YouTube extractor implementation
    private static class YouTubeExtractor extends InfoExtractor {
        @Override
        protected String extractVideoId(String url) {
            // Simple YouTube video ID extraction
            if (url.contains("youtube.com/watch?v=")) {
                return url.substring(url.indexOf("v=") + 2);
            } else if (url.contains("youtu.be/")) {
                return url.substring(url.lastIndexOf("/") + 1);
            }
            return null;
        }

        @Override
        protected com.ytdlp.core.VideoInfo realExtract(String url, String videoId) throws Exception {
            // This would implement actual YouTube extraction
            // For now, return a placeholder VideoInfo
            com.ytdlp.core.VideoInfo info = new com.ytdlp.core.VideoInfo();
            info.setId(videoId);
            info.setTitle("YouTube Video " + videoId);
            info.setUrl(url);
            return info;
        }

        @Override
        public String getIE_NAME() {
            return "youtube";
        }

        @Override
        public String getIE_DESC() {
            return "YouTube video extractor";
        }

        @Override
        public java.util.regex.Pattern getVALID_URL() {
            return java.util.regex.Pattern.compile(
                "(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([^&?/]+)"
            );
        }
    }
}
