import com.ytdlp.downloader.hls.HlsDownloader;
import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.utils.Logger;
import java.io.File;

public class TestHlsWithPublicUrl {
    public static void main(String[] args) {
        // 使用一个公开的HLS测试URL
        String hlsUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8";
        String outputPath = "test_public_hls.mp4";
        
        System.out.println("=== 测试HLS下载器与公开URL ===");
        System.out.println("HLS URL: " + hlsUrl);
        System.out.println("输出文件: " + outputPath);
        
        try {
            // 创建虚拟的VideoInfo和VideoFormat对象
            VideoInfo videoInfo = new VideoInfo();
            videoInfo.setTitle("Public HLS Test");
            videoInfo.setId("public_test");
            
            VideoFormat format = new VideoFormat();
            format.setUrl(hlsUrl);
            format.setExt("mp4");
            format.setProtocol("hls");
            
            // 使用HLS下载器
            HlsDownloader hlsDownloader = new HlsDownloader();
            hlsDownloader.initialize(null, new Logger(true, false, false));
            
            System.out.println("开始HLS下载...");
            boolean success = hlsDownloader.download(videoInfo, format, outputPath);
            
            System.out.println("下载结果: " + success);
            
            // 检查文件是否存在
            File file = new File(outputPath);
            if (file.exists()) {
                System.out.println("文件大小: " + file.length() + " bytes");
                System.out.println("文件类型: " + getFileType(file));
            } else {
                System.out.println("❌ 文件不存在");
            }
            
        } catch (Exception e) {
            System.err.println("❌ HLS下载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String getFileType(File file) {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            byte[] header = new byte[8];
            if (fis.read(header) >= 8) {
                fis.close();
                
                // 检查文件头
                String headerStr = new String(header);
                if (headerStr.startsWith("#EXTM3U")) {
                    return "M3U8 Playlist";
                } else if (header[0] == 0x00 && header[1] == 0x00 && header[2] == 0x00 && header[3] == 0x18) {
                    return "MP4 Video";
                } else if (header[0] == 0x47) {
                    return "MPEG-TS Video";
                }
            }
            fis.close();
        } catch (Exception e) {
            // 忽略错误
        }
        return "Unknown";
    }
}
