import java.util.regex.Pattern;

public class SimpleTest {
    public static void main(String[] args) {
        String m3u8Url = "https://vod3.cf.dmcdn.net/sec2(...)/video/fmp4/591312388/h264_aac/manifest.m3u8#cell=cf3";
        String pattern = "(?i).*\\.m3u8.*";
        
        Pattern p = Pattern.compile(pattern);
        boolean matches = p.matcher(m3u8Url).matches();
        
        System.out.println("URL: " + m3u8Url);
        System.out.println("Pattern: " + pattern);
        System.out.println("Matches: " + matches);
    }
}
