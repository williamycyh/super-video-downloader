import com.ytdlp.downloader.hls.HlsDownloader;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.utils.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestHlsDebug {
    public static void main(String[] args) {
        String hlsUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8";
        
        System.out.println("=== 调试HLS下载器 ===");
        System.out.println("URL: " + hlsUrl);
        
        try {
            // 直接测试M3U8下载
            System.out.println("\n1. 测试M3U8下载...");
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
                System.out.println("✅ M3U8下载成功");
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    int lineCount = 0;
                    System.out.println("\n2. M3U8内容:");
                    while ((line = reader.readLine()) != null && lineCount < 20) {
                        System.out.println(line);
                        lineCount++;
                    }
                    if (lineCount >= 20) {
                        System.out.println("... (显示前20行)");
                    }
                }
            } else {
                System.out.println("❌ M3U8下载失败，响应码: " + responseCode);
            }
            
            connection.disconnect();
            
            // 测试HLS下载器
            System.out.println("\n3. 测试HLS下载器...");
            HlsDownloader hlsDownloader = new HlsDownloader();
            hlsDownloader.initialize(null, new Logger(true, false, false));
            
            // 使用反射调用私有方法
            java.lang.reflect.Method method = HlsDownloader.class.getDeclaredMethod("downloadPlaylist", String.class, VideoFormat.class);
            method.setAccessible(true);
            
            VideoFormat format = new VideoFormat();
            format.setUrl(hlsUrl);
            
            Object playlist = method.invoke(hlsDownloader, hlsUrl, format);
            System.out.println("播放列表对象: " + playlist);
            
        } catch (Exception e) {
            System.err.println("❌ 调试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
