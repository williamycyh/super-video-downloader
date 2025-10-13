import com.ytdlp.downloader.hls.HlsDownloader;
import com.ytdlp.downloader.hls.HlsDownloader.HlsPlaylist;
import com.ytdlp.downloader.hls.HlsDownloader.HlsSegment;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.utils.Logger;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

public class TestHlsPlaylistParsing {
    public static void main(String[] args) {
        System.out.println("=== 测试HLS播放列表解析 ===");
        
        // 模拟M3U8内容
        String m3u8Content = "#EXTM3U\n" +
                           "#EXT-X-VERSION:7\n" +
                           "#EXT-X-TARGETDURATION:3\n" +
                           "#EXT-X-MEDIA-SEQUENCE:0\n" +
                           "#EXT-X-PLAYLIST-TYPE:VOD\n" +
                           "#EXT-X-MAP:URI=\"init.mp4\"\n" +
                           "#EXTINF:3.003003,\n" +
                           "0.m4s\n" +
                           "#EXTINF:3.003003,\n" +
                           "1.m4s\n" +
                           "#EXTINF:3.003003,\n" +
                           "2.m4s\n" +
                           "#EXT-X-ENDLIST\n";
        
        System.out.println("M3U8内容:");
        System.out.println(m3u8Content);
        
        try {
            // 创建HLS下载器
            HlsDownloader hlsDownloader = new HlsDownloader();
            hlsDownloader.initialize(null, new Logger(true, false, false));
            
            // 使用反射调用私有方法parsePlaylist
            java.lang.reflect.Method method = HlsDownloader.class.getDeclaredMethod("parsePlaylist", BufferedReader.class, String.class);
            method.setAccessible(true);
            
            String baseUrl = "https://www.dailymotion.com/cdn/manifest/video/x9s1tzq.m3u8";
            BufferedReader reader = new BufferedReader(new StringReader(m3u8Content));
            
            HlsPlaylist playlist = (HlsPlaylist) method.invoke(hlsDownloader, reader, baseUrl);
            
            if (playlist != null) {
                System.out.println("\n✅ 播放列表解析成功:");
                System.out.println("版本: " + playlist.getVersion());
                System.out.println("目标时长: " + playlist.getTargetDuration());
                System.out.println("媒体序列: " + playlist.getMediaSequence());
                System.out.println("结束列表: " + playlist.isEndList());
                System.out.println("片段数量: " + playlist.getSegments().size());
                
                System.out.println("\n片段列表:");
                List<HlsSegment> segments = playlist.getSegments();
                for (int i = 0; i < segments.size(); i++) {
                    HlsSegment segment = segments.get(i);
                    System.out.printf("片段 %d: %s (时长: %.3f秒)%n", 
                        i, segment.getUrl(), segment.getDuration());
                }
            } else {
                System.out.println("❌ 播放列表解析失败");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
