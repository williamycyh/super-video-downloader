package com.example.util.downloaders.generic_downloader.models;

public class VideoTaskItem implements Cloneable {

    private String mUrl;                 //下载视频的url
    private String mCoverUrl;            //封面图的url
    private String mCoverPath;           //封面图存储的位置
    private String mTitle;               //视频的标题
    private String mGroupName;           //下载分组的名称
    private long mDownloadCreateTime;    //下载创建的时间
    private int mTaskState;              //当前任务的状态
    private String mMimeType;            // 视频url的mime type
    private String mFinalUrl;            //30x跳转之后的url
    private int mErrorCode;              //当前任务下载错误码
    private int mVideoType;              //当前文件类型
    private int mTotalTs;                //当前M3U8的总分片
    private int mCurTs;                  //当前M3U8已缓存的分片
    private float mSpeed;                //当前下载速度, getSpeedString 函数可以将速度格式化
    private float mPercent;              //当前下载百分比, 0 ~ 100,是浮点数
    private long mDownloadSize;          //已下载大小, getDownloadSizeString 函数可以将大小格式化
    private long mTotalSize;             //文件总大小, M3U8文件无法准确获知
    private String mFileHash;            //文件名的md5
    private String mSaveDir;             //保存视频文件的文件目录名
    private boolean mIsCompleted;        //是否下载完成
    private boolean mIsInDatabase;       //是否存到数据库中
    private long mLastUpdateTime;        //上一次更新数据库的时间
    private String mFileName;            //文件名
    private String mFilePath;            //文件完整路径(包括文件名)
    private boolean mPaused;

    private String mErrorMessage;

    private String mId;

    private String lineInfo;

    public VideoTaskItem(String url) {
        this(url, "", "", "");
    }

    public VideoTaskItem(String url, String coverUrl, String title, String groupName) {
        mUrl = url;
        mCoverUrl = coverUrl;
        mTitle = title;
        mGroupName = groupName;
    }

    public float getPercentFromBytes() {
        if (getTotalSize() == 0) return 0;

        return (1F * getDownloadSize() / getTotalSize()) * 100F;
    }

    public float getPercentFromBytes(long downloadSize, long totalSize) {
        if (totalSize == 0) return 0;

        return (1F * downloadSize / totalSize) * 100F;
    }

    public String getLineInfo() {
        return this.lineInfo;
    }

    public void setLineInfo(String info) {
        this.lineInfo = info;
    }

    public String getMId() {
        return mId;
    }

    public void setMId(String id) {
        mId = id;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public void setErrorMessage(String message) {
        mErrorMessage = message;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getCoverUrl() {
        return mCoverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        mCoverUrl = coverUrl;
    }

    public String getCoverPath() {
        return mCoverPath;
    }

    public void setCoverPath(String coverPath) {
        mCoverPath = coverPath;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getGroupName() {
        return mGroupName;
    }

    public void setGroupName(String groupName) {
        mGroupName = groupName;
    }

    public long getDownloadCreateTime() {
        return mDownloadCreateTime;
    }

    public void setDownloadCreateTime(long time) {
        mDownloadCreateTime = time;
    }

    public int getTaskState() {
        return mTaskState;
    }

    public void setTaskState(int state) {
        mTaskState = state;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    public String getFinalUrl() {
        return mFinalUrl;
    }

    public void setFinalUrl(String finalUrl) {
        mFinalUrl = finalUrl;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public void setErrorCode(int errorCode) {
        mErrorCode = errorCode;
    }

    public int getVideoType() {
        return mVideoType;
    }

    public void setVideoType(int type) {
        mVideoType = type;
    }

    public int getTotalTs() {
        return mTotalTs;
    }

    public void setTotalTs(int count) {
        mTotalTs = count;
    }

    public int getCurTs() {
        return mCurTs;
    }

    public void setCurTs(int count) {
        mCurTs = count;
    }

    public float getSpeed() {
        return mSpeed;
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
    }

    public float getPercent() {
        return mPercent;
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    public long getDownloadSize() {
        return mDownloadSize;
    }

    public void setDownloadSize(long size) {
        mDownloadSize = size;
    }

    public long getTotalSize() {
        return mTotalSize;
    }

    public void setTotalSize(long size) {
        mTotalSize = size;
    }

    public String getFileHash() {
        return mFileHash;
    }

    public void setFileHash(String md5) {
        mFileHash = md5;
    }

    public String getSaveDir() {
        return mSaveDir;
    }

    public void setSaveDir(String path) {
        mSaveDir = path;
    }

    public void setIsCompleted(boolean completed) {
        mIsCompleted = completed;
    }

    public boolean isCompleted() {
        return mIsCompleted;
    }

    public void setIsInDatabase(boolean in) {
        mIsInDatabase = in;
    }

    public boolean isInDatabase() {
        return mIsInDatabase;
    }

    public long getLastUpdateTime() {
        return mLastUpdateTime;
    }

    public void setLastUpdateTime(long time) {
        mLastUpdateTime = time;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String name) {
        mFileName = name;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String path) {
        mFilePath = path;
    }

    public boolean isPaused() {
        return mPaused;
    }

    public void setPaused(boolean paused) {
        mPaused = paused;
    }

    public boolean isRunningTask() {
        return mTaskState == VideoTaskState.DOWNLOADING;
    }

    public boolean isPendingTask() {
        return mTaskState == VideoTaskState.PENDING || mTaskState == VideoTaskState.PREPARE;
    }

    public boolean isErrorState() {
        return mTaskState == VideoTaskState.ERROR;
    }

    public boolean isSuccessState() {
        return mTaskState == VideoTaskState.SUCCESS;
    }

    public boolean isInterruptTask() {
        return mTaskState == VideoTaskState.PAUSE || mTaskState == VideoTaskState.ERROR;
    }

    public boolean isInitialTask() {
        return mTaskState == VideoTaskState.DEFAULT;
    }

    public boolean isHlsType() {
        return mVideoType == Video.Type.HLS_TYPE;
    }

    public void reset() {
        mDownloadCreateTime = 0L;
        mMimeType = null;
        mErrorCode = 0;
        mVideoType = Video.Type.DEFAULT;
        mTaskState = VideoTaskState.DEFAULT;
        mSpeed = 0.0f;
        mPercent = 0.0f;
        mDownloadSize = 0;
        mTotalSize = 0;
        mFileName = "";
        mFilePath = "";
        mCoverUrl = "";
        mCoverPath = "";
        mTitle = "";
        mGroupName = "";
    }

    @Override
    public Object clone() {
        VideoTaskItem taskItem = new VideoTaskItem(mUrl);
        taskItem.setDownloadCreateTime(mDownloadCreateTime);
        taskItem.setTaskState(mTaskState);
        taskItem.setMimeType(mMimeType);
        taskItem.setErrorCode(mErrorCode);
        taskItem.setVideoType(mVideoType);
        taskItem.setPercent(mPercent);
        taskItem.setDownloadSize(mDownloadSize);
        taskItem.setSpeed(mSpeed);
        taskItem.setTotalSize(mTotalSize);
        taskItem.setFileHash(mFileHash);
        taskItem.setFilePath(mFilePath);
        taskItem.setFileName(mFileName);
        taskItem.setCoverUrl(mCoverUrl);
        taskItem.setCoverPath(mCoverPath);
        taskItem.setTitle(mTitle);
        taskItem.setGroupName(mGroupName);
        return taskItem;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof VideoTaskItem) {
            String objUrl = ((VideoTaskItem) obj).getUrl();
            if (mUrl.equals(objUrl)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return "VideoTaskItem[Url=" + mUrl +
                ", Type=" + mVideoType +
                ", Percent=" + mPercent +
                ", DownloadSize=" + mDownloadSize +
                ", State=" + mTaskState +
                ", FilePath=" + mFileName +
                ", LocalFile=" + mFilePath +
                ", CoverUrl=" + mCoverUrl +
                ", CoverPath=" + mCoverPath +
                ", Title=" + mTitle +
                "]";
    }
}
