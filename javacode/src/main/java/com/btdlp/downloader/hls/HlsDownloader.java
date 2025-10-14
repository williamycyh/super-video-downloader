package com.btdlp.downloader.hls;

import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;
import com.btdlp.downloader.BaseDownloader;
import com.btdlp.utils.Logger;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * HLS (HTTP Live Streaming) 下载器
 */
public class HlsDownloader extends BaseDownloader {
    
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_CONCURRENT_DOWNLOADS = 4;
    
    private ExecutorService executorService;
    
    public HlsDownloader() {
        super();
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        
        // 确保logger被初始化
        if (this.logger == null) {
            this.logger = new Logger(true, true, true);
        }
    }
    
    public String getDownloaderName() {
        return "hls";
    }
    
    public String getDownloaderDescription() {
        return "HLS (HTTP Live Streaming) downloader";
    }
    
    public boolean suitable(VideoFormat format) {
        if (format == null || format.getUrl() == null) {
            return false;
        }
        
        String url = format.getUrl().toLowerCase();
        String protocol = format.getProtocol();
        
        return "hls".equals(protocol) || url.contains(".m3u8") || url.contains("m3u8");
    }
    
    @Override
    protected boolean performDownload(VideoInfo videoInfo, VideoFormat format, File outputFile) throws Exception {
        String url = format.getUrl();
        logger.info("=== HLS Downloader performDownload 开始 ===");
        logger.info("VideoInfo: " + videoInfo.getTitle());
        logger.info("Format ID: " + format.getFormatId());
        logger.info("Format Protocol: " + format.getProtocol());
        logger.info("Format URL: " + url);
        logger.info("Output File: " + outputFile.getAbsolutePath());
        
        try {
            // 下载播放列表
            logger.info("开始下载播放列表...");
            HlsPlaylist playlist = downloadPlaylist(url, format);
            if (playlist == null || playlist.getSegments().isEmpty()) {
                throw new IOException("No segments found in playlist");
            }
            
            logger.info("播放列表下载完成，找到 " + playlist.getSegments().size() + " 个片段");
            
            // 下载所有片段
            logger.info("开始下载所有片段...");
            boolean success = downloadSegments(playlist, format, outputFile);
            
            if (success) {
                logger.info("HLS下载完成: " + outputFile.getAbsolutePath());
                logger.info("输出文件大小: " + outputFile.length() + " bytes");
            } else {
                logger.error("HLS下载失败");
            }
            
            logger.info("=== HLS Downloader performDownload 结束 ===");
            return success;
            
        } catch (Exception e) {
            logger.error("HLS下载过程中发生异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            shutdownExecutor();
        }
    }
    
    private HlsPlaylist downloadPlaylist(String url, VideoFormat format) throws Exception {
        URL urlObj = new URL(url);
        HttpURLConnection connection = createConnection(urlObj, format);
        
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }
            
            // 首先检查是否是主播放列表
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                boolean isMasterPlaylist = false;
                int lineCount = 0;
                
                // 检查前10行是否包含EXT-X-STREAM-INF
                while ((line = reader.readLine()) != null && lineCount < 10) {
                    if (line.contains("EXT-X-STREAM-INF")) {
                        isMasterPlaylist = true;
                        break;
                    }
                    lineCount++;
                }
                
                if (isMasterPlaylist) {
                    // 这是主播放列表，需要选择最佳变体
                    logger.info("检测到主播放列表，选择最佳变体");
                    connection.disconnect();
                    return downloadVariantPlaylist(url, format);
                }
                
                // 重置流到开始位置
                reader.close();
                connection.disconnect();
                
                // 重新创建连接
                connection = createConnection(urlObj, format);
                try (BufferedReader newReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    return parsePlaylist(newReader, url);
                }
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    private HlsPlaylist parsePlaylist(BufferedReader reader, String baseUrl) throws Exception {
        HlsPlaylist playlist = new HlsPlaylist();
        String line;
        HlsSegment currentSegment = null;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.startsWith("#EXT-X-VERSION:")) {
                playlist.setVersion(parseVersion(line));
            } else if (line.startsWith("#EXT-X-TARGETDURATION:")) {
                playlist.setTargetDuration(parseTargetDuration(line));
            } else if (line.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                playlist.setMediaSequence(parseMediaSequence(line));
            } else if (line.startsWith("#EXTINF:")) {
                // 创建一个新的片段并设置时长
                currentSegment = new HlsSegment();
                currentSegment.setDuration(parseSegmentDuration(line));
            } else if (line.startsWith("#EXT-X-ENDLIST")) {
                playlist.setEndList(true);
            } else if (!line.startsWith("#") && !line.isEmpty()) {
                // 这是一个片段URL
                if (currentSegment != null) {
                    currentSegment.setUrl(resolveUrl(line, baseUrl));
                    playlist.addSegment(currentSegment);
                    currentSegment = null; // 重置当前片段
                }
            }
        }
        
        return playlist;
    }
    
    private HlsPlaylist downloadVariantPlaylist(String url, VideoFormat format) throws Exception {
        // 下载变体播放列表，选择最佳质量
        URL urlObj = new URL(url);
        HttpURLConnection connection = createConnection(urlObj, format);
        
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                String bestUrl = null;
                int bestBandwidth = 0;
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    if (line.startsWith("#EXT-X-STREAM-INF:")) {
                        // 解析带宽信息
                        String bandwidthStr = extractAttribute(line, "BANDWIDTH");
                        if (bandwidthStr != null) {
                            int bandwidth = Integer.parseInt(bandwidthStr);
                            if (bandwidth > bestBandwidth) {
                                bestBandwidth = bandwidth;
                                String nextLine = reader.readLine();
                                if (nextLine != null && !nextLine.startsWith("#")) {
                                    bestUrl = resolveUrl(nextLine.trim(), url);
                                }
                            }
                        }
                    }
                }
                
                if (bestUrl != null) {
                    return downloadPlaylist(bestUrl, format);
                }
            }
            
        } finally {
            connection.disconnect();
        }
        
        return null;
    }
    
    private boolean downloadSegments(HlsPlaylist playlist, VideoFormat format, File outputFile) throws Exception {
        List<HlsSegment> segments = playlist.getSegments();
        Path tempDir = Files.createTempDirectory("hls_download");
        
        try {
            // 计算总大小估算（基于片段时长）
            long estimatedTotalSize = calculateEstimatedTotalSize(playlist);
            logger.info("估算总大小: %s bytes", estimatedTotalSize);
            
            // 并发下载所有片段
            List<Future<Long>> downloadTasks = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                final HlsSegment segment = segments.get(i);
                // 根据片段URL的扩展名确定文件扩展名
                String extension = getSegmentExtension(segment.getUrl());
                final Path segmentFile = tempDir.resolve(String.format("segment_%06d.%s", i, extension));
                final int segmentIndex = i;
                
                Future<Long> task = executorService.submit(() -> {
                    return downloadSegmentWithProgress(segment, format, segmentFile, segmentIndex);
                });
                
                downloadTasks.add(task);
            }
            
            // 等待所有下载完成并收集进度
            long totalDownloaded = 0;
            for (int i = 0; i < downloadTasks.size(); i++) {
                Future<Long> task = downloadTasks.get(i);
                try {
                    long segmentSize = task.get();
                    totalDownloaded += segmentSize;
                    
                    // 更新进度 - 基于片段数量或估算大小
                    int percentage;
                    if (estimatedTotalSize > 0) {
                        percentage = (int) Math.min(95, (totalDownloaded * 100) / estimatedTotalSize); // 留5%给合并过程
                    } else {
                        percentage = (int) ((i + 1) * 90 / downloadTasks.size()); // 留10%给合并过程
                    }
                    
                    updateProgress(totalDownloaded, estimatedTotalSize, 0);
                    
                    logger.debug("片段 %s 下载完成，累计下载: %s bytes (%s%%)", i + 1, totalDownloaded, percentage);
                } catch (ExecutionException e) {
                    logger.error("Segment download failed: " + e.getCause().getMessage());
                    return false;
                }
            }
            
            // 合并片段
            boolean success = mergeSegments(playlist, outputFile, tempDir);
            
            if (success) {
                // 合并完成，更新进度到100%
                updateProgress(outputFile.length(), outputFile.length(), 0);
                logger.info("HLS下载和合并完成，最终文件大小: %s bytes", outputFile.length());
            }
            
            return success;
            
        } finally {
            // 清理临时文件
            try {
                Files.walk(tempDir)
                     .sorted(Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
            } catch (Exception e) {
                logger.warning("Failed to clean temp directory: " + e.getMessage());
            }
        }
    }
    
    /**
     * 计算估算的总大小（基于片段时长）
     */
    private long calculateEstimatedTotalSize(HlsPlaylist playlist) {
        double totalDuration = 0;
        for (HlsSegment segment : playlist.getSegments()) {
            totalDuration += segment.getDuration();
        }
        
        // 假设平均比特率为2Mbps，这是一个合理的估算
        // 总大小 = 时长(秒) * 比特率(bps) / 8
        long estimatedSize = (long) (totalDuration * 2 * 1024 * 1024 / 8);
        logger.info("总时长: %s 秒，估算大小: %s bytes", totalDuration, estimatedSize);
        
        return estimatedSize;
    }
    
    /**
     * 下载片段并返回实际大小（带进度更新）
     */
    private long downloadSegmentWithProgress(HlsSegment segment, VideoFormat format, Path segmentFile, int index) throws Exception {
        logger.info("下载片段 %s: %s", index, segment.getUrl());
        
        URL urlObj = new URL(segment.getUrl());
        HttpURLConnection connection = createConnection(urlObj, format);
        
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode + " for segment " + index);
            }
            
            long contentLength = connection.getContentLengthLong();
            logger.debug("片段 %s 响应成功，内容长度: %s bytes", index, contentLength);
            
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(segmentFile.toFile())) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (shouldCancel()) {
                        throw new InterruptedException("Download cancelled");
                    }
                    
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                logger.debug("Downloaded segment " + index + ": " + totalBytes + " bytes");
                return totalBytes;
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    
    private boolean mergeSegments(HlsPlaylist playlist, File outputFile, Path tempDir) throws Exception {
        logger.info("开始合并 %s 个片段到文件: %s", playlist.getSegments().size(), outputFile.getAbsolutePath());
        
        // 使用简单的TS合并方式
        logger.info("使用简单的TS合并方式");
        return mergeTsSegmentsSimple(playlist, outputFile, tempDir);
    }
    
    private boolean mergeTsSegmentsSimple(HlsPlaylist playlist, File outputFile, Path tempDir) throws Exception {
        logger.info("使用简单的TS文件合并");
        
        try (FileOutputStream merged = new FileOutputStream(outputFile)) {
            // 按照用户指导：合并所有TS文件
            for (int i = 0; i < playlist.getSegments().size(); i++) {
                Path segmentFile = findSegmentFile(tempDir, i);
                if (segmentFile != null && Files.exists(segmentFile)) {
                    logger.info("合并片段 %s: %s (大小: %s bytes)", i, segmentFile, Files.size(segmentFile));
                    Files.copy(segmentFile, merged);
                } else {
                    logger.warning("片段文件不存在: segment_%s", i);
                }
            }
            
            logger.info("简单的TS合并完成，输出文件大小: %s bytes", outputFile.length());
            return outputFile.length() > 0;
            
        } catch (Exception e) {
            logger.error("简单的TS合并失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private Path findFirstSegment(Path tempDir) throws Exception {
        return findSegmentFile(tempDir, 0);
    }
    
    private Path findSegmentFile(Path tempDir, int index) throws Exception {
        // 查找指定索引的片段文件（可能是不同的扩展名）
        String[] extensions = {"ts", "m4s", "mp4"};
        for (String ext : extensions) {
            Path segmentFile = tempDir.resolve(String.format("segment_%06d.%s", index, ext));
            if (Files.exists(segmentFile)) {
                return segmentFile;
            }
        }
        return null;
    }
    
    private boolean isFmp4Format(Path segmentFile) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(segmentFile.toFile())) {
            byte[] header = new byte[8];
            if (inputStream.read(header) >= 8) {
                // 检查是否以MP4原子开头
                int size = ((header[0] & 0xFF) << 24) | ((header[1] & 0xFF) << 16) | 
                          ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
                String type = new String(header, 4, 4);
                
                logger.info("片段头部: size=%s, type=%s", size, type);
                
                // 常见的MP4原子类型
                return type.equals("ftyp") || type.equals("styp") || type.equals("moof") || 
                       type.equals("mdat") || type.equals("sidx");
            }
        }
        return false;
    }
    
    private boolean mergeTsSegments(HlsPlaylist playlist, File outputFile, Path tempDir) throws Exception {
        logger.info("使用TS格式合并");
        
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            for (int i = 0; i < playlist.getSegments().size(); i++) {
                Path segmentFile = findSegmentFile(tempDir, i);
                if (segmentFile == null || !Files.exists(segmentFile)) {
                    logger.warning("TS片段文件不存在: segment_{:06d}", i);
                    continue;
                }
                
                logger.info("合并TS片段 %s: %s (大小: %s bytes)", i, segmentFile, Files.size(segmentFile));
                
                try (FileInputStream inputStream = new FileInputStream(segmentFile.toFile())) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesRead;
                        long totalBytesRead = 0;
                        
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                        
                        logger.info("TS片段 %s 合并完成，读取了 %s bytes", i, totalBytesRead);
                    }
            }
            
            logger.info("所有TS片段合并完成，输出文件大小: %s bytes", outputFile.length());
            return true;
        } catch (Exception e) {
            logger.error("合并TS片段失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean mergeFmp4SegmentsSimple(HlsPlaylist playlist, File outputFile, Path tempDir) throws Exception {
        logger.info("使用正确的fMP4合并方法");
        
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            // 1. 写入ftyp原子（28字节，与Python版本一致）
            writeFtypAtomCorrect(outputStream);
            
            // 2. 写入moov原子（108字节，与Python版本一致）
            writeMoovAtomCorrect(outputStream);
            
            // 3. 创建mdat原子头部
            long totalMdatSize = 0;
            for (int i = 0; i < playlist.getSegments().size(); i++) {
                Path segmentFile = findSegmentFile(tempDir, i);
                if (segmentFile != null && Files.exists(segmentFile)) {
                    byte[] segmentData = Files.readAllBytes(segmentFile);
                    
                    // 查找mdat原子
                    int offset = 0;
                    while (offset < segmentData.length - 8) {
                        int size = ((segmentData[offset] & 0xFF) << 24) | 
                                  ((segmentData[offset + 1] & 0xFF) << 16) | 
                                  ((segmentData[offset + 2] & 0xFF) << 8) | 
                                  (segmentData[offset + 3] & 0xFF);
                        
                        if (size <= 0 || size > segmentData.length - offset) {
                            break;
                        }
                        
                        String type = new String(segmentData, offset + 4, 4);
                        
                        if ("mdat".equals(type)) {
                            totalMdatSize += (size - 8);
                            break;
                        }
                        
                        offset += size;
                    }
                }
            }
            
            // 4. 写入mdat原子头部
            long mdatSize = totalMdatSize + 8;
            outputStream.write((byte) ((mdatSize >> 24) & 0xFF));
            outputStream.write((byte) ((mdatSize >> 16) & 0xFF));
            outputStream.write((byte) ((mdatSize >> 8) & 0xFF));
            outputStream.write((byte) (mdatSize & 0xFF));
            outputStream.write("mdat".getBytes());
            
            // 5. 写入所有片段的mdat数据
            for (int i = 0; i < playlist.getSegments().size(); i++) {
                Path segmentFile = findSegmentFile(tempDir, i);
                if (segmentFile != null && Files.exists(segmentFile)) {
                    byte[] segmentData = Files.readAllBytes(segmentFile);
                    
                    // 查找mdat原子
                    int offset = 0;
                    while (offset < segmentData.length - 8) {
                        int size = ((segmentData[offset] & 0xFF) << 24) | 
                                  ((segmentData[offset + 1] & 0xFF) << 16) | 
                                  ((segmentData[offset + 2] & 0xFF) << 8) | 
                                  (segmentData[offset + 3] & 0xFF);
                        
                        if (size <= 0 || size > segmentData.length - offset) {
                            break;
                        }
                        
                        String type = new String(segmentData, offset + 4, 4);
                        
                        if ("mdat".equals(type)) {
                            // 写入mdat原子的数据部分（跳过原子头部）
                            outputStream.write(segmentData, offset + 8, size - 8);
                            logger.info("写入片段 %s 的mdat数据，大小: %s bytes", i, size - 8);
                            break;
                        }
                        
                        offset += size;
                    }
                }
            }
            
            logger.info("正确的fMP4合并完成，输出文件大小: %s bytes", outputFile.length());
            return outputFile.length() > 0;
            
        } catch (Exception e) {
            logger.error("正确的fMP4合并失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void writeFtypAtomCorrect(FileOutputStream outputStream) throws Exception {
        // 创建与Python版本完全一致的ftyp原子（28字节）
        byte[] ftypAtom = new byte[28];
        
        // 原子大小 (28 bytes)
        ftypAtom[0] = 0x00;
        ftypAtom[1] = 0x00;
        ftypAtom[2] = 0x00;
        ftypAtom[3] = 0x1C; // 28
        
        // 原子类型
        ftypAtom[4] = 'f';
        ftypAtom[5] = 't';
        ftypAtom[6] = 'y';
        ftypAtom[7] = 'p';
        
        // 主要品牌 (iso5)
        ftypAtom[8] = 'i';
        ftypAtom[9] = 's';
        ftypAtom[10] = 'o';
        ftypAtom[11] = '5';
        
        // 版本
        ftypAtom[12] = 0x00;
        ftypAtom[13] = 0x00;
        ftypAtom[14] = 0x02;
        ftypAtom[15] = 0x00;
        
        // 兼容品牌 (iso5)
        ftypAtom[16] = 'i';
        ftypAtom[17] = 's';
        ftypAtom[18] = 'o';
        ftypAtom[19] = '5';
        
        // 兼容品牌 (iso6)
        ftypAtom[20] = 'i';
        ftypAtom[21] = 's';
        ftypAtom[22] = 'o';
        ftypAtom[23] = '6';
        
        // 兼容品牌 (mp41)
        ftypAtom[24] = 'm';
        ftypAtom[25] = 'p';
        ftypAtom[26] = '4';
        ftypAtom[27] = '1';
        
        outputStream.write(ftypAtom);
        logger.info("写入ftyp原子，大小: 28 bytes");
    }
    
    private void writeMoovAtomCorrect(FileOutputStream outputStream) throws Exception {
        // 创建与Python版本一致的moov原子（108字节）
        byte[] moovAtom = new byte[108];
        
        // 原子大小 (108 bytes)
        moovAtom[0] = 0x00;
        moovAtom[1] = 0x00;
        moovAtom[2] = 0x00;
        moovAtom[3] = 0x6C; // 108
        
        // 原子类型
        moovAtom[4] = 'm';
        moovAtom[5] = 'o';
        moovAtom[6] = 'o';
        moovAtom[7] = 'v';
        
        // mvhd原子大小 (32 bytes)
        moovAtom[8] = 0x00;
        moovAtom[9] = 0x00;
        moovAtom[10] = 0x00;
        moovAtom[11] = 0x20; // 32
        
        // mvhd原子类型
        moovAtom[12] = 'm';
        moovAtom[13] = 'v';
        moovAtom[14] = 'h';
        moovAtom[15] = 'd';
        
        // 版本和标志 (1 + 3)
        moovAtom[16] = 0x00; // 版本
        moovAtom[17] = 0x00; // 标志
        moovAtom[18] = 0x00; // 标志
        moovAtom[19] = 0x00; // 标志
        
        // 创建时间 (4 bytes) - 使用当前时间
        long creationTime = System.currentTimeMillis() / 1000 + 2082844800; // 转换为Mac时间
        moovAtom[20] = (byte) ((creationTime >> 24) & 0xFF);
        moovAtom[21] = (byte) ((creationTime >> 16) & 0xFF);
        moovAtom[22] = (byte) ((creationTime >> 8) & 0xFF);
        moovAtom[23] = (byte) (creationTime & 0xFF);
        
        // 修改时间 (4 bytes) - 使用当前时间
        moovAtom[24] = (byte) ((creationTime >> 24) & 0xFF);
        moovAtom[25] = (byte) ((creationTime >> 16) & 0xFF);
        moovAtom[26] = (byte) ((creationTime >> 8) & 0xFF);
        moovAtom[27] = (byte) (creationTime & 0xFF);
        
        // 时间刻度 (4 bytes) - 1000
        moovAtom[28] = 0x00;
        moovAtom[29] = 0x00;
        moovAtom[30] = 0x03;
        moovAtom[31] = (byte) 0xE8; // 1000
        
        // 持续时间 (4 bytes) - 40秒 * 1000 = 40000
        moovAtom[32] = 0x00;
        moovAtom[33] = 0x00;
        moovAtom[34] = (byte) 0x9C;
        moovAtom[35] = 0x40; // 40000
        
        // 速率 (4 bytes) - 1.0
        moovAtom[36] = 0x00;
        moovAtom[37] = 0x01;
        moovAtom[38] = 0x00;
        moovAtom[39] = 0x00; // 65536 = 1.0
        
        // 音量 (2 bytes) - 1.0
        moovAtom[40] = 0x01;
        moovAtom[41] = 0x00; // 256 = 1.0
        
        // 保留字段 (10 bytes)
        for (int i = 42; i < 52; i++) {
            moovAtom[i] = 0x00;
        }
        
        // 矩阵 (36 bytes) - 单位矩阵
        // 前16字节
        moovAtom[52] = 0x00; moovAtom[53] = 0x01; moovAtom[54] = 0x00; moovAtom[55] = 0x00;
        moovAtom[56] = 0x00; moovAtom[57] = 0x00; moovAtom[58] = 0x00; moovAtom[59] = 0x00;
        moovAtom[60] = 0x00; moovAtom[61] = 0x00; moovAtom[62] = 0x00; moovAtom[63] = 0x00;
        moovAtom[64] = 0x00; moovAtom[65] = 0x01; moovAtom[66] = 0x00; moovAtom[67] = 0x00;
        
        // 中间16字节
        moovAtom[68] = 0x00; moovAtom[69] = 0x00; moovAtom[70] = 0x00; moovAtom[71] = 0x00;
        moovAtom[72] = 0x00; moovAtom[73] = 0x00; moovAtom[74] = 0x00; moovAtom[75] = 0x00;
        moovAtom[76] = 0x00; moovAtom[77] = 0x01; moovAtom[78] = 0x00; moovAtom[79] = 0x00;
        moovAtom[80] = 0x00; moovAtom[81] = 0x00; moovAtom[82] = 0x00; moovAtom[83] = 0x00;
        
        // 最后4字节
        moovAtom[84] = 0x00; moovAtom[85] = 0x00; moovAtom[86] = 0x00; moovAtom[87] = 0x00;
        
        // 预定义 (24 bytes)
        for (int i = 88; i < 108; i++) {
            moovAtom[i] = 0x00;
        }
        
        outputStream.write(moovAtom);
        logger.info("写入moov原子，大小: 108 bytes");
    }

    private boolean mergeFmp4Segments(HlsPlaylist playlist, File outputFile, Path tempDir) throws Exception {
        logger.info("使用fMP4格式合并");
        
        // 对于fMP4 HLS，我们需要使用ffmpeg或类似工具来正确合并
        // 由于这是Java实现，我们使用一个简化的方法：
        // 1. 找到第一个包含初始化信息的片段
        // 2. 创建一个基本的MP4容器
        // 3. 将所有片段的mdat部分合并
        
        try {
            // 查找第一个片段
            Path firstSegment = findSegmentFile(tempDir, 0);
            if (firstSegment == null || !Files.exists(firstSegment)) {
                logger.error("找不到第一个fMP4片段");
                return false;
            }
            
            logger.info("处理fMP4片段合并，共 %s 个片段", playlist.getSegments().size());
            
            // 创建一个临时的m3u8文件用于ffmpeg处理
            File tempM3u8 = File.createTempFile("temp_", ".m3u8");
            try {
                // 生成m3u8播放列表
                generateM3u8Playlist(playlist, tempM3u8, tempDir);
                
                // 使用ffmpeg合并（如果可用）
                if (isFfmpegAvailable()) {
                    logger.info("使用ffmpeg合并fMP4片段");
                    return mergeWithFfmpeg(tempM3u8, outputFile);
                } else {
                    // 回退到简单合并（可能不完美）
                    logger.warning("ffmpeg不可用，使用简单合并方法");
                    return simpleFmp4Merge(playlist, outputFile, tempDir);
                }
                
            } finally {
                if (tempM3u8.exists()) {
                    tempM3u8.delete();
                }
            }
            
        } catch (Exception e) {
            logger.error("合并fMP4片段失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean simpleFmp4Merge(HlsPlaylist playlist, File outputFile, Path tempDir) throws Exception {
        logger.info("使用改进的fMP4合并方法");
        
        // 改进的fMP4合并方法：
        // 1. 找到第一个包含初始化信息的片段
        // 2. 提取并合并所有片段的媒体数据
        // 3. 创建正确的MP4容器结构
        
        try {
            // 查找所有片段文件
            List<Path> segmentFiles = new ArrayList<>();
            for (int i = 0; i < playlist.getSegments().size(); i++) {
                Path segmentFile = findSegmentFile(tempDir, i);
                if (segmentFile != null && Files.exists(segmentFile)) {
                    segmentFiles.add(segmentFile);
                    logger.info("找到fMP4片段 %s: %s (大小: %s bytes)", i, segmentFile, Files.size(segmentFile));
                } else {
                    logger.warning("fMP4片段文件不存在: segment_%s", i);
                }
            }
            
            if (segmentFiles.isEmpty()) {
                logger.error("没有找到任何fMP4片段文件");
                return false;
            }
            
            // 使用改进的MP4合并算法
            return mergeFmp4SegmentsImproved(segmentFiles, outputFile);
            
        } catch (Exception e) {
            logger.error("改进的fMP4合并失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean mergeFmp4SegmentsImproved(List<Path> segmentFiles, File outputFile) throws Exception {
        logger.info("使用改进的MP4合并算法，共 %s 个片段", segmentFiles.size());
        
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            // 读取第一个片段作为基础
            Path firstSegment = segmentFiles.get(0);
            byte[] firstSegmentData = Files.readAllBytes(firstSegment);
            
            // 分析第一个片段的MP4结构
            Mp4Structure firstStructure = analyzeMp4Structure(firstSegmentData);
            logger.info("第一个片段结构分析: 包含 %s 个原子", firstStructure.atoms.size());
            
            // 创建输出MP4文件
            // 1. 写入ftyp原子（如果第一个片段有的话，否则创建默认的）
            if (firstStructure.hasFtyp) {
                writeFtypAtom(outputStream, firstSegmentData, firstStructure);
            } else {
                // 创建默认的ftyp原子
                writeDefaultFtypAtom(outputStream);
            }
            
            // 2. 写入moov原子（从第一个片段提取，如果不存在则创建基本的）
            if (firstStructure.hasMoov) {
                writeMoovAtom(outputStream, firstSegmentData, firstStructure);
            } else {
                // 创建基本的moov原子
                writeBasicMoovAtom(outputStream);
            }
            
            // 3. 合并所有片段的mdat原子
            writeMdatAtom(outputStream, segmentFiles);
            
            logger.info("改进的MP4合并完成，输出文件大小: %s bytes", outputFile.length());
            return outputFile.length() > 0;
            
        } catch (Exception e) {
            logger.error("改进的MP4合并异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static class Mp4Atom {
        int size;
        String type;
        int offset;
        byte[] data;
        
        Mp4Atom(int size, String type, int offset) {
            this.size = size;
            this.type = type;
            this.offset = offset;
        }
    }
    
    private static class Mp4Structure {
        List<Mp4Atom> atoms = new ArrayList<>();
        boolean hasFtyp = false;
        boolean hasMoov = false;
        boolean hasMdat = false;
    }
    
    private Mp4Structure analyzeMp4Structure(byte[] data) {
        Mp4Structure structure = new Mp4Structure();
        int offset = 0;
        
        while (offset < data.length - 8) {
            // 读取原子大小（4字节，大端序）
            int size = ((data[offset] & 0xFF) << 24) | 
                      ((data[offset + 1] & 0xFF) << 16) | 
                      ((data[offset + 2] & 0xFF) << 8) | 
                      (data[offset + 3] & 0xFF);
            
            if (size <= 0 || size > data.length - offset) {
                break;
            }
            
            // 读取原子类型（4字节）
            String type = new String(data, offset + 4, 4);
            
            Mp4Atom atom = new Mp4Atom(size, type, offset);
            atom.data = new byte[size];
            System.arraycopy(data, offset, atom.data, 0, size);
            structure.atoms.add(atom);
            
            // 检查特殊原子类型
            switch (type) {
                case "ftyp":
                    structure.hasFtyp = true;
                    break;
                case "moov":
                    structure.hasMoov = true;
                    break;
                case "mdat":
                    structure.hasMdat = true;
                    break;
            }
            
            offset += size;
        }
        
        return structure;
    }
    
    private void writeFtypAtom(FileOutputStream outputStream, byte[] firstSegmentData, Mp4Structure structure) throws Exception {
        for (Mp4Atom atom : structure.atoms) {
            if ("ftyp".equals(atom.type)) {
                outputStream.write(atom.data);
                logger.info("写入ftyp原子，大小: %s bytes", atom.size);
                break;
            }
        }
    }
    
    private void writeMoovAtom(FileOutputStream outputStream, byte[] firstSegmentData, Mp4Structure structure) throws Exception {
        for (Mp4Atom atom : structure.atoms) {
            if ("moov".equals(atom.type)) {
                outputStream.write(atom.data);
                logger.info("写入moov原子，大小: %s bytes", atom.size);
                break;
            }
        }
    }
    
    private void writeMdatAtom(FileOutputStream outputStream, List<Path> segmentFiles) throws Exception {
        logger.info("开始合并mdat原子，共 %s 个片段", segmentFiles.size());
        
        for (int i = 0; i < segmentFiles.size(); i++) {
            Path segmentFile = segmentFiles.get(i);
            byte[] segmentData = Files.readAllBytes(segmentFile);
            
            Mp4Structure structure = analyzeMp4Structure(segmentData);
            
            // 找到并写入mdat原子
            for (Mp4Atom atom : structure.atoms) {
                if ("mdat".equals(atom.type)) {
                    outputStream.write(atom.data);
                    logger.info("写入片段 %s 的mdat原子，大小: %s bytes", i, atom.size);
                    break;
                }
            }
        }
        
        logger.info("mdat原子合并完成");
    }
    
    private void writeDefaultFtypAtom(FileOutputStream outputStream) throws Exception {
        // 创建Python兼容的ftyp原子 (28 bytes)
        // 与Python yt-dlp生成的格式完全一致
        byte[] ftypAtom = new byte[28];
        
        // 原子大小 (28 bytes)
        ftypAtom[0] = 0x00;
        ftypAtom[1] = 0x00;
        ftypAtom[2] = 0x00;
        ftypAtom[3] = 0x1c;
        
        // 原子类型 "ftyp"
        ftypAtom[4] = 'f';
        ftypAtom[5] = 't';
        ftypAtom[6] = 'y';
        ftypAtom[7] = 'p';
        
        // major_brand "iso5" (与Python版本一致)
        ftypAtom[8] = 'i';
        ftypAtom[9] = 's';
        ftypAtom[10] = 'o';
        ftypAtom[11] = '5';
        
        // minor_version (与Python版本一致: 0x00000200)
        ftypAtom[12] = 0x00;
        ftypAtom[13] = 0x00;
        ftypAtom[14] = 0x02;
        ftypAtom[15] = 0x00;
        
        // compatible_brands: "iso5", "iso6", "mp41" (与Python版本一致)
        System.arraycopy("iso5".getBytes(), 0, ftypAtom, 16, 4);
        System.arraycopy("iso6".getBytes(), 0, ftypAtom, 20, 4);
        System.arraycopy("mp41".getBytes(), 0, ftypAtom, 24, 4);
        
        outputStream.write(ftypAtom);
        logger.info("写入Python兼容的ftyp原子，大小: 28 bytes (iso5品牌)");
    }
    
    private void writeBasicMoovAtom(FileOutputStream outputStream) throws Exception {
        // 创建一个更完整的moov原子，基于Python版本的结构
        // 参考Python版本生成的moov原子结构
        
        // moov原子总大小 (108 bytes，与Python版本一致)
        byte[] moovAtom = new byte[108];
        int offset = 0;
        
        // moov原子大小 (108 bytes)
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x6c;
        
        // moov原子类型
        moovAtom[offset++] = 'm';
        moovAtom[offset++] = 'o';
        moovAtom[offset++] = 'o';
        moovAtom[offset++] = 'v';
        
        // mvhd原子 (电影头部)
        // mvhd原子大小 (108 bytes)
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x6c;
        
        // mvhd原子类型
        moovAtom[offset++] = 'm';
        moovAtom[offset++] = 'v';
        moovAtom[offset++] = 'h';
        moovAtom[offset++] = 'd';
        
        // mvhd版本和标志 (1 byte version + 3 bytes flags)
        moovAtom[offset++] = 0x00; // 版本
        moovAtom[offset++] = 0x00; // 标志
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        
        // 创建时间 (4 bytes)
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        
        // 修改时间 (4 bytes)
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        
        // 时间刻度 (4 bytes) - 1000 (0x03e8)
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x03;
        moovAtom[offset++] = (byte) 0xe8;
        
        // 持续时间 (4 bytes) - 0 (未指定)
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        
        // 播放速率 (4 bytes) - 1.0 (0x00010000)
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x01;
        moovAtom[offset++] = 0x00;
        moovAtom[offset++] = 0x00;
        
        // 音量 (2 bytes) - 1.0 (0x0100)
        moovAtom[offset++] = 0x01;
        moovAtom[offset++] = 0x00;
        
        // 保留字段 (10 bytes)
        for (int i = 0; i < 10; i++) {
            moovAtom[offset++] = 0x00;
        }
        
        // 矩阵结构 (36 bytes) - 单位矩阵
        // 这里简化为零矩阵
        for (int i = 0; i < 36; i++) {
            moovAtom[offset++] = 0x00;
        }
        
        // 预览时间和持续时间 (6 bytes)
        for (int i = 0; i < 6; i++) {
            moovAtom[offset++] = 0x00;
        }
        
        // 写入moov原子
        outputStream.write(moovAtom);
        logger.info("写入Python兼容的moov原子，大小: 108 bytes");
    }
    
    private void generateM3u8Playlist(HlsPlaylist playlist, File outputFile, Path tempDir) throws Exception {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("#EXTM3U\n");
            writer.write("#EXT-X-VERSION:3\n");
            writer.write("#EXT-X-TARGETDURATION:10\n");
            writer.write("#EXT-X-MEDIA-SEQUENCE:0\n");
            
            for (int i = 0; i < playlist.getSegments().size(); i++) {
                Path segmentFile = findSegmentFile(tempDir, i);
                if (segmentFile != null) {
                    HlsSegment segment = playlist.getSegments().get(i);
                    writer.write("#EXTINF:" + segment.getDuration() + ",\n");
                    writer.write(segmentFile.getFileName().toString() + "\n");
                }
            }
            
            writer.write("#EXT-X-ENDLIST\n");
        }
    }
    
    private boolean isFfmpegAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean mergeWithFfmpeg(File m3u8File, File outputFile) throws Exception {
        try {
            String[] command = {
                "ffmpeg", "-i", m3u8File.getAbsolutePath(),
                "-c", "copy", "-bsf:a", "aac_adtstoasc",
                outputFile.getAbsolutePath()
            };
            
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && outputFile.exists() && outputFile.length() > 0) {
                logger.info("ffmpeg合并成功，输出文件大小: %s bytes", outputFile.length());
                return true;
            } else {
                logger.error("ffmpeg合并失败，退出码: %s", exitCode);
                return false;
            }
        } catch (Exception e) {
            logger.error("ffmpeg合并异常: " + e.getMessage());
            return false;
        }
    }
    
    private void copyFile(File source, FileOutputStream destination) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(source)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                destination.write(buffer, 0, bytesRead);
            }
        }
    }
    
    private HttpURLConnection createConnection(URL url, VideoFormat format) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        connection.setInstanceFollowRedirects(true);
        
        // 设置请求头
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        connection.setRequestProperty("Connection", "keep-alive");
        
        // 添加全局HTTP头部
        if (params != null) {
            String httpHeaders = params.getHttpHeaders();
            if (httpHeaders != null && !httpHeaders.isEmpty()) {
                String[] headerLines = httpHeaders.split("\n");
                for (String headerLine : headerLines) {
                    String[] parts = headerLine.split(":", 2);
                    if (parts.length == 2) {
                        connection.setRequestProperty(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }
        
        // 添加格式特定的头部
        if (format.getHttpHeaders() != null) {
            for (Map.Entry<String, String> entry : format.getHttpHeaders().entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        
        return connection;
    }
    
    private String resolveUrl(String url, String baseUrl) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        
        try {
            URL base = new URL(baseUrl);
            return new URL(base, url).toString();
        } catch (MalformedURLException e) {
            return url;
        }
    }
    
    private String getSegmentExtension(String segmentUrl) {
        if (segmentUrl == null) {
            return "ts"; // 默认扩展名
        }
        
        // 检查URL中的扩展名
        if (segmentUrl.contains(".m4s")) {
            return "m4s";
        } else if (segmentUrl.contains(".ts")) {
            return "ts";
        } else if (segmentUrl.contains(".mp4")) {
            return "mp4";
        } else {
            return "ts"; // 默认扩展名
        }
    }
    
    private int parseVersion(String line) {
        try {
            return Integer.parseInt(line.substring("#EXT-X-VERSION:".length()).trim());
        } catch (Exception e) {
            return 3; // 默认版本
        }
    }
    
    private int parseTargetDuration(String line) {
        try {
            return Integer.parseInt(line.substring("#EXT-X-TARGETDURATION:".length()).trim());
        } catch (Exception e) {
            return 10; // 默认时长
        }
    }
    
    private long parseMediaSequence(String line) {
        try {
            return Long.parseLong(line.substring("#EXT-X-MEDIA-SEQUENCE:".length()).trim());
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double parseSegmentDuration(String line) {
        try {
            String durationStr = line.substring("#EXTINF:".length()).split(",")[0].trim();
            return Double.parseDouble(durationStr);
        } catch (Exception e) {
            return 10.0; // 默认时长
        }
    }
    
    private String extractAttribute(String line, String attribute) {
        String pattern = attribute + "=";
        int index = line.indexOf(pattern);
        if (index >= 0) {
            int start = index + pattern.length();
            int end = line.indexOf(",", start);
            if (end < 0) {
                end = line.length();
            }
            return line.substring(start, end).trim();
        }
        return null;
    }
    
    private void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    
    /**
     * HLS播放列表类
     */
    public static class HlsPlaylist {
        private int version = 3;
        private int targetDuration = 10;
        private long mediaSequence = 0;
        private boolean endList = false;
        private List<HlsSegment> segments = new ArrayList<>();
        
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
        
        public int getTargetDuration() { return targetDuration; }
        public void setTargetDuration(int targetDuration) { this.targetDuration = targetDuration; }
        
        public long getMediaSequence() { return mediaSequence; }
        public void setMediaSequence(long mediaSequence) { this.mediaSequence = mediaSequence; }
        
        public boolean isEndList() { return endList; }
        public void setEndList(boolean endList) { this.endList = endList; }
        
        public List<HlsSegment> getSegments() { return segments; }
        public void setSegments(List<HlsSegment> segments) { this.segments = segments; }
        
        public void addSegment(HlsSegment segment) {
            this.segments.add(segment);
        }
    }
    
    /**
     * HLS片段类
     */
    public static class HlsSegment {
        private String url;
        private double duration;
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public double getDuration() { return duration; }
        public void setDuration(double duration) { this.duration = duration; }
    }
}
