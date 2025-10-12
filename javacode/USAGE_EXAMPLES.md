# 使用示例

## 🚀 基本示例

### 1. Facebook视频下载

```java
import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.extractor.facebook.AdvancedFacebookExtractor;
import com.ytdlp.downloader.FileDownloader;

public class FacebookDownloadExample {
    public static void main(String[] args) {
        try {
            // 创建Facebook提取器
            AdvancedFacebookExtractor extractor = new AdvancedFacebookExtractor();
            
            // 提取视频信息
            VideoInfo videoInfo = extractor.extract("https://www.facebook.com/watch/?v=123456789");
            
            System.out.println("标题: " + videoInfo.getTitle());
            System.out.println("时长: " + videoInfo.getDuration() + " 秒");
            System.out.println("格式数量: " + videoInfo.getFormats().size());
            
            // 选择最佳格式
            VideoFormat bestFormat = videoInfo.getFormats().stream()
                .filter(f -> "mp4".equals(f.getExt()))
                .findFirst()
                .orElse(videoInfo.getFormats().get(0));
            
            // 下载视频
            FileDownloader downloader = new FileDownloader();
            boolean success = downloader.download(bestFormat.getUrl(), "facebook_video.mp4");
            
            if (success) {
                System.out.println("下载成功!");
            } else {
                System.out.println("下载失败!");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 2. TikTok视频下载

```java
import com.ytdlp.extractor.tiktok.AdvancedTikTokExtractor;

public class TikTokDownloadExample {
    public static void main(String[] args) {
        try {
            AdvancedTikTokExtractor extractor = new AdvancedTikTokExtractor();
            VideoInfo videoInfo = extractor.extract("https://www.tiktok.com/@user/video/123456789");
            
            // 处理视频信息...
            System.out.println("TikTok视频: " + videoInfo.getTitle());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 3. Dailymotion HLS下载

```java
import com.ytdlp.extractor.dailymotion.AdvancedDailymotionExtractor;
import com.ytdlp.downloader.HLSDownloader;

public class DailymotionDownloadExample {
    public static void main(String[] args) {
        try {
            AdvancedDailymotionExtractor extractor = new AdvancedDailymotionExtractor();
            VideoInfo videoInfo = extractor.extract("https://www.dailymotion.com/video/x123456");
            
            // 找到HLS格式
            VideoFormat hlsFormat = videoInfo.getFormats().stream()
                .filter(f -> "m3u8".equals(f.getExt()))
                .findFirst()
                .orElse(null);
            
            if (hlsFormat != null) {
                // 使用HLS下载器
                boolean success = HLSDownloader.downloadHLS(hlsFormat.getUrl(), "dailymotion_video.mp4");
                System.out.println("HLS下载结果: " + (success ? "成功" : "失败"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 📱 Android示例

### 1. 基本Activity

```java
public class MainActivity extends AppCompatActivity {
    private EditText urlInput;
    private Button downloadButton;
    private ProgressBar progressBar;
    private TextView statusText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupDownload();
    }
    
    private void initViews() {
        urlInput = findViewById(R.id.url_input);
        downloadButton = findViewById(R.id.download_button);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        
        downloadButton.setOnClickListener(v -> startDownload());
    }
    
    private void startDownload() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "请输入视频URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new VideoDownloadTask().execute(url);
    }
    
    private class VideoDownloadTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                String videoUrl = urls[0];
                
                // 根据URL选择提取器
                InfoExtractor extractor = createExtractor(videoUrl);
                VideoInfo videoInfo = extractor.extract(videoUrl);
                
                publishProgress(50);
                
                // 选择最佳格式并下载
                VideoFormat bestFormat = selectBestFormat(videoInfo.getFormats());
                String outputPath = getOutputPath(videoInfo.getTitle(), bestFormat.getExt());
                
                FileDownloader downloader = new FileDownloader();
                boolean success = downloader.download(bestFormat.getUrl(), outputPath);
                
                return success ? outputPath : null;
                
            } catch (Exception e) {
                return null;
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }
        
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("下载完成: " + result);
                Toast.makeText(MainActivity.this, "下载成功!", Toast.LENGTH_SHORT).show();
            } else {
                statusText.setText("下载失败");
                Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
```

### 2. 下载管理器

```java
public class VideoDownloadManager {
    private Context context;
    
    public VideoDownloadManager(Context context) {
        this.context = context;
    }
    
    public void downloadVideo(String url, DownloadCallback callback) {
        new DownloadTask(callback).execute(url);
    }
    
    private class DownloadTask extends AsyncTask<String, Void, DownloadResult> {
        private DownloadCallback callback;
        
        public DownloadTask(DownloadCallback callback) {
            this.callback = callback;
        }
        
        @Override
        protected DownloadResult doInBackground(String... urls) {
            try {
                String videoUrl = urls[0];
                InfoExtractor extractor = createExtractor(videoUrl);
                VideoInfo videoInfo = extractor.extract(videoUrl);
                
                VideoFormat bestFormat = selectBestFormat(videoInfo.getFormats());
                String outputPath = getOutputPath(videoInfo.getTitle(), bestFormat.getExt());
                
                FileDownloader downloader = new FileDownloader();
                boolean success = downloader.download(bestFormat.getUrl(), outputPath);
                
                return new DownloadResult(success, outputPath, null);
                
            } catch (Exception e) {
                return new DownloadResult(false, null, e.getMessage());
            }
        }
        
        @Override
        protected void onPostExecute(DownloadResult result) {
            if (callback != null) {
                if (result.isSuccess()) {
                    callback.onSuccess(result.getFilePath());
                } else {
                    callback.onError(result.getErrorMessage());
                }
            }
        }
    }
    
    public interface DownloadCallback {
        void onSuccess(String filePath);
        void onError(String error);
    }
    
    public static class DownloadResult {
        private boolean success;
        private String filePath;
        private String errorMessage;
        
        public DownloadResult(boolean success, String filePath, String errorMessage) {
            this.success = success;
            this.filePath = filePath;
            this.errorMessage = errorMessage;
        }
        
        // Getters...
    }
}
```

## 🔧 高级示例

### 1. 批量下载

```java
public class BatchDownloadExample {
    public static void main(String[] args) {
        List<String> urls = Arrays.asList(
            "https://www.facebook.com/watch/?v=123456789",
            "https://www.instagram.com/p/ABC123/",
            "https://www.tiktok.com/@user/video/123456789"
        );
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (String url : urls) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    InfoExtractor extractor = createExtractor(url);
                    VideoInfo videoInfo = extractor.extract(url);
                    VideoFormat bestFormat = selectBestFormat(videoInfo.getFormats());
                    
                    FileDownloader downloader = new FileDownloader();
                    return downloader.download(bestFormat.getUrl(), generateFileName(url));
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });
            
            futures.add(future);
        }
        
        // 等待所有下载完成
        for (Future<Boolean> future : futures) {
            try {
                boolean success = future.get();
                System.out.println("下载结果: " + (success ? "成功" : "失败"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
    }
}
```

### 2. 进度监控

```java
public class ProgressMonitorExample {
    public static void main(String[] args) {
        FileDownloader downloader = new FileDownloader();
        
        downloader.download("https://example.com/video.mp4", "output.mp4", new ProgressCallback() {
            @Override
            public void onProgress(int percentage, long bytesDownloaded, long totalBytes) {
                System.out.printf("下载进度: %d%% (%d/%d bytes)%n", 
                    percentage, bytesDownloaded, totalBytes);
            }
            
            @Override
            public void onComplete(String filePath) {
                System.out.println("下载完成: " + filePath);
            }
            
            @Override
            public void onError(String error) {
                System.err.println("下载失败: " + error);
            }
        });
    }
}
```

### 3. 自定义提取器

```java
public class CustomExtractorExample extends InfoExtractor {
    
    public CustomExtractorExample() {
        super();
        this.logger = new Logger(true, false, false);
    }
    
    @Override
    public String getIE_NAME() {
        return "custom";
    }
    
    @Override
    public String getIE_DESC() {
        return "Custom video extractor";
    }
    
    @Override
    public Pattern getVALID_URL() {
        return Pattern.compile("https://custom-site\\.com/video/(\\d+)");
    }
    
    @Override
    public boolean suitable(String url) {
        return getVALID_URL().matcher(url).find();
    }
    
    @Override
    public String extractVideoId(String url) {
        Matcher matcher = getVALID_URL().matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    @Override
    protected VideoInfo realExtract(String url, String videoId) throws Exception {
        // 实现自定义提取逻辑
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setTitle("Custom Video Title");
        
        VideoFormat format = new VideoFormat();
        format.setUrl("https://custom-site.com/video/" + videoId + ".mp4");
        format.setFormatId("custom");
        format.setExt("mp4");
        
        videoInfo.setFormats(Arrays.asList(format));
        
        return videoInfo;
    }
}
```

## 🛠️ 工具类示例

### 1. URL检测工具

```java
public class VideoUrlDetector {
    
    private static final Map<String, Class<? extends InfoExtractor>> EXTRACTORS = new HashMap<>();
    
    static {
        EXTRACTORS.put("facebook.com", AdvancedFacebookExtractor.class);
        EXTRACTORS.put("instagram.com", AdvancedInstagramExtractor.class);
        EXTRACTORS.put("tiktok.com", AdvancedTikTokExtractor.class);
        // 添加更多...
    }
    
    public static InfoExtractor createExtractor(String url) {
        for (Map.Entry<String, Class<? extends InfoExtractor>> entry : EXTRACTORS.entrySet()) {
            if (url.contains(entry.getKey())) {
                try {
                    return entry.getValue().newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    
    public static boolean isSupported(String url) {
        return createExtractor(url) != null;
    }
    
    public static String getPlatformName(String url) {
        for (String platform : EXTRACTORS.keySet()) {
            if (url.contains(platform)) {
                return platform.replace(".com", "");
            }
        }
        return "unknown";
    }
}
```

### 2. 文件管理工具

```java
public class FileManager {
    
    public static String generateFileName(String title, String ext) {
        if (title == null || title.trim().isEmpty()) {
            return "video_" + System.currentTimeMillis() + "." + ext;
        }
        
        String cleanTitle = title.replaceAll("[^a-zA-Z0-9\\s\\-_]", "");
        cleanTitle = cleanTitle.replaceAll("\\s+", "_");
        
        return cleanTitle + "." + ext;
    }
    
    public static String getOutputDirectory() {
        return System.getProperty("user.home") + File.separator + "Downloads";
    }
    
    public static String getOutputPath(String fileName) {
        File outputDir = new File(getOutputDirectory());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        return new File(outputDir, fileName).getAbsolutePath();
    }
    
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }
}
```

这些示例展示了如何在各种场景下使用yt-dlp Java版本，从基本的视频下载到复杂的Android集成和批量处理。
