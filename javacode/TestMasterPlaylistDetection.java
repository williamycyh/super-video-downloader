import com.ytdlp.downloader.hls.HlsDownloader;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.utils.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestMasterPlaylistDetection {
    public static void main(String[] args) {
        String hlsUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8";
        
        System.out.println("=== 测试主播放列表检测 ===");
        System.out.println("URL: " + hlsUrl);
        
        try {
            // 直接测试M3U8下载和解析
            URL url = new URL(hlsUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            System.out.println("响应码: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    int lineCount = 0;
                    boolean isMasterPlaylist = false;
                    
                    while ((line = reader.readLine()) != null && lineCount < 10) {
                        System.out.println("行 " + lineCount + ": " + line);
                        
                        if (lineCount == 0 && line.trim().equals("#EXTM3U")) {
                            System.out.println("✅ 找到EXTM3U标签");
                        }
                        
                        if (lineCount > 0 && line.contains("EXT-X-STREAM-INF")) {
                            System.out.println("✅ 找到EXT-X-STREAM-INF，这是主播放列表");
                            isMasterPlaylist = true;
                        }
                        
                        lineCount++;
                    }
                    
                    System.out.println("\n检测结果: " + (isMasterPlaylist ? "主播放列表" : "普通播放列表"));
                }
            }
            
            connection.disconnect();
            
            // 测试HLS下载器的变体播放列表下载
            System.out.println("\n=== 测试变体播放列表下载 ===");
            HlsDownloader hlsDownloader = new HlsDownloader();
            hlsDownloader.initialize(null, new Logger(true, false, false));
            
            // 使用反射调用私有方法
            java.lang.reflect.Method method = HlsDownloader.class.getDeclaredMethod("downloadVariantPlaylist", String.class, VideoFormat.class);
            method.setAccessible(true);
            
            VideoFormat format = new VideoFormat();
            format.setUrl(hlsUrl);
            
            Object playlist = method.invoke(hlsDownloader, hlsUrl, format);
            System.out.println("变体播放列表下载结果: " + playlist);
            
        } catch (Exception e) {
            System.err.println("❌ 测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
