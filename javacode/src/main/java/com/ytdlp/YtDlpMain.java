package com.ytdlp;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;

import java.util.*;

/**
 * yt-dlp Java版本主入口类
 * 参考Python版本的命令行接口设计
 */
public class YtDlpMain {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        // 解析命令行参数
        Map<String, String> options = parseArgs(args);
        List<String> urls = getUrls(args);
        
        if (urls.isEmpty()) {
            System.err.println("错误: 未提供视频URL");
            printUsage();
            System.exit(1);
        }
        
        // 创建主下载器
        YtDlpJava ytdlp = new YtDlpJava(options);
        
        // 设置进度回调
        ytdlp.addProgressCallback(new YtDlpJava.ProgressCallback() {
            @Override
            public void onProgress(int percentage, long bytesDownloaded, long totalBytes) {
                if (!"true".equals(options.get("quiet"))) {
                    System.out.printf("\r下载进度: %d%%", percentage);
                    if (totalBytes > 0) {
                        System.out.printf(" (%d/%d bytes)", bytesDownloaded, totalBytes);
                    }
                }
            }
            
            @Override
            public void onComplete(String filePath) {
                if (!"true".equals(options.get("quiet"))) {
                    System.out.println("\n✅ 下载完成: " + filePath);
                }
            }
            
            @Override
            public void onError(String error) {
                System.err.println("\n❌ 下载失败: " + error);
            }
        });
        
        // 处理每个URL
        boolean hasError = false;
        for (String url : urls) {
            try {
                if (!"true".equals(options.get("quiet"))) {
                    System.out.println("开始下载: " + url);
                }
                
                if ("true".equals(options.get("list-formats"))) {
                    // 列出格式
                    listFormats(ytdlp, url);
                } else if ("true".equals(options.get("simulate"))) {
                    // 模拟下载
                    simulateDownload(ytdlp, url);
                } else {
                    // 实际下载
                    YtDlpJava.DownloadResult result = ytdlp.download(url);
                    
                    if (result.isSuccess()) {
                        if (!"true".equals(options.get("quiet"))) {
                            System.out.println("🎉 下载成功!");
                            System.out.println("文件路径: " + result.getFilePath());
                            System.out.println("视频标题: " + result.getVideoInfo().getTitle());
                        }
                    } else {
                        System.err.println("💥 下载失败: " + result.getErrorMessage());
                        if (!"true".equals(options.get("ignore-errors"))) {
                            hasError = true;
                        }
                    }
                }
                
            } catch (Exception e) {
                System.err.println("💥 处理URL时发生异常: " + e.getMessage());
                if (!"true".equals(options.get("ignore-errors"))) {
                    hasError = true;
                }
            }
            
            System.out.println(); // 空行分隔
        }
        
        if (hasError) {
            System.exit(1);
        }
    }
    
    /**
     * 解析命令行参数
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.startsWith("--")) {
                String option = arg.substring(2);
                
                if (i + 1 < args.length && !args[i + 1].startsWith("--") && !args[i + 1].startsWith("-")) {
                    // 带值的选项
                    options.put(option, args[i + 1]);
                    i++; // 跳过值
                } else {
                    // 布尔选项
                    options.put(option, "true");
                }
            } else if (arg.startsWith("-") && arg.length() > 1) {
                // 短选项
                String option = arg.substring(1);
                options.put(option, "true");
            }
        }
        
        return options;
    }
    
    /**
     * 获取URL列表
     */
    private static List<String> getUrls(String[] args) {
        List<String> urls = new ArrayList<>();
        
        for (String arg : args) {
            if (!arg.startsWith("-") && (arg.startsWith("http://") || arg.startsWith("https://"))) {
                urls.add(arg);
            }
        }
        
        return urls;
    }
    
    /**
     * 列出可用格式
     */
    private static void listFormats(YtDlpJava ytdlp, String url) {
        System.out.println("=== 可用格式 ===");
        
        List<VideoFormat> formats = ytdlp.listFormats(url);
        
        if (formats.isEmpty()) {
            System.out.println("未找到可用格式");
            return;
        }
        
        System.out.printf("%-4s %-15s %-8s %-10s %-15s%n", "ID", "格式", "质量", "扩展名", "协议");
        System.out.println("-".repeat(60));
        
        for (int i = 0; i < formats.size(); i++) {
            VideoFormat format = formats.get(i);
            System.out.printf("%-4d %-15s %-8s %-10s %-15s%n",
                i,
                format.getFormatId(),
                format.getQuality() != null ? format.getQuality() + "p" : "未知",
                format.getExt(),
                format.getProtocol() != null ? format.getProtocol() : "http");
        }
    }
    
    /**
     * 模拟下载
     */
    private static void simulateDownload(YtDlpJava ytdlp, String url) {
        System.out.println("=== 模拟下载 ===");
        
        VideoInfo videoInfo = ytdlp.extractInfo(url);
        
        if (videoInfo != null) {
            System.out.println("标题: " + videoInfo.getTitle());
            System.out.println("ID: " + videoInfo.getId());
            System.out.println("时长: " + videoInfo.getDuration() + " 秒");
            System.out.println("上传者: " + videoInfo.getUploader());
            System.out.println("格式数量: " + videoInfo.getFormats().size());
            System.out.println("缩略图: " + videoInfo.getThumbnail());
        } else {
            System.out.println("无法提取视频信息");
        }
    }
    
    private static void printUsage() {
        System.out.println("yt-dlp Java版本 - 视频下载工具");
        System.out.println();
        System.out.println("用法:");
        System.out.println("  java -cp . com.ytdlp.YtDlpMain [选项] URL [URL ...]");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  -f, --format FORMAT        视频格式选择 (默认: best)");
        System.out.println("  -o, --output TEMPLATE      输出文件名模板");
        System.out.println("  -q, --quiet                静默模式");
        System.out.println("  -v, --verbose              详细输出");
        System.out.println("  --list-formats             列出可用格式");
        System.out.println("  --simulate                 模拟下载，不实际下载文件");
        System.out.println("  --ignore-errors            忽略错误继续处理");
        System.out.println("  -h, --help                 显示帮助信息");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -cp . com.ytdlp.YtDlpMain https://www.facebook.com/watch/?v=123456789");
        System.out.println("  java -cp . com.ytdlp.YtDlpMain -f 720p -o 'video_%(title)s.%(ext)s' https://www.instagram.com/p/ABC123/");
        System.out.println("  java -cp . com.ytdlp.YtDlpMain --list-formats https://www.tiktok.com/@user/video/123456789");
        System.out.println("  java -cp . com.ytdlp.YtDlpMain --simulate https://www.pornhub.com/view_video.php?viewkey=123456789");
        System.out.println();
        System.out.println("支持的平台:");
        for (String platform : YtDlpJava.getSupportedPlatforms()) {
            System.out.println("  - " + platform);
        }
    }
}
