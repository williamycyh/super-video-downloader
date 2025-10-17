package com.example.tubedown.rereads;

public class AppCon {
    // 0:宽松策略： 不能下载youtube/soundcloud
    // 1: 中等策略  不能下载 youtube/soundcloud/fb/insta
    // 2:严格策略： 不能下载 fb, instagram,tiktok, twitter,x
    public long f_type;

    public long show_tube;

    public String block_urls;

    public long ph_num;

    public long dialog_type;
    public String dialog_msg;
    public String dialog_pkg;
    public long app_version;
    public int v_version;

    public long force_use;
}
