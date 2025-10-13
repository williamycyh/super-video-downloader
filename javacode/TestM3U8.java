import com.ytdlp.extractor.m3u8.M3U8Extractor;
import com.ytdlp.extractor.dailymotion.AdvancedDailymotionExtractor;
import java.util.regex.Pattern;

public class TestM3U8 {
    public static void main(String[] args) {
        String m3u8Url = "https://vod3.cf.dmcdn.net/sec2(99c5dHufOxUn8GFbShMto6BcVKfndCT1eiWErXJ8cY4wz4jDsmvhAoEs3AK97Zo0luIqscH2me_syQoopH4-ss2SDjcVsHALOooOyDnwEApwuUxTNHaO2_H3n9HlRI7lz_TKXeH-JJ4xSk8XTNU_zx9TQc418uqZDqaOHC8NG_AC3QiStNssOVSSF4LxZL3q)/video/fmp4/591312388/h264_aac/manifest.m3u8#cell=cf3";
        
        System.out.println("Testing M3U8 URL: " + m3u8Url);
        System.out.println();
        
        // Test M3U8 Extractor
        M3U8Extractor m3u8Extractor = new M3U8Extractor();
        System.out.println("M3U8Extractor.suitable(): " + m3u8Extractor.suitable(m3u8Url));
        
        // Test Dailymotion Extractor
        AdvancedDailymotionExtractor dmExtractor = new AdvancedDailymotionExtractor();
        System.out.println("AdvancedDailymotionExtractor.suitable(): " + dmExtractor.suitable(m3u8Url));
        
        // Test regex patterns
        String m3u8Pattern = "(?i).*\\.m3u8.*";
        Pattern m3u8Regex = Pattern.compile(m3u8Pattern);
        System.out.println("M3U8 regex matches: " + m3u8Regex.matcher(m3u8Url).matches());
    }
}
