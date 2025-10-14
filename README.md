# Android Video and Audio Downloader app with browser, player and custom Bube downloaders

[![F-Droid](https://img.shields.io/f-droid/v/com.myAllVideoBrowser?color=b4eb12&label=F-Droid&logo=fdroid&logoColor=1f78d2)](https://f-droid.org/packages/com.myAllVideoBrowser)

<a href="https://f-droid.org/packages/com.myAllVideoBrowser"><img src="https://f-droid.org/badge/get-it-on.png"></a>

## Features

- Download videos from Youtube, Facebook, Twitter, Instagram, Dailymotion, Vimeo and more
  than [other 1000 sites](http://rg3.github.io/youtube-dl/supportedsites.html), also intercept all
  streams data in browser like m3u8 or mpd links and download them, also intercepts mp4 streaming
  video data
- **Custom Bube Download Engine**: Built-in custom video downloader based on yt-dlp with Java implementation
- **FFmpeg Integration**: Advanced HLS/TS stream processing with real-time progress tracking
- Browse videos and audio with the built-in browser
- Download videos and audio with the built-in download manager
- Play videos and audio offline with the built-in player
- Save your favorite videos and audio online and watch them later without downloading them
- Save history as a real browser
- Bookmarks support
- Live streams download support
- Live MP3 streams support
- Cokies support
- HTTP proxy support

## Technical Details

### Custom Download Engine
This project features a custom-built video download engine called **Bube**, which is a Java implementation based on the popular Python yt-dlp library. The engine includes:

- **Package**: `com.btdlp` (renamed from com.ytdlp)
- **Main Classes**: `BtdJava`, `BubeDL`, `BubeDLRequest`, `BubeDLResponse`
- **FFmpeg Integration**: Real-time HLS/TS stream processing with progress callbacks
- **Multi-platform Support**: 11+ video platforms with dedicated extractors

### Architecture
- **Language**: Kotlin
- **Architecture**: MVVM
- **Custom Download Engine**: Bube (Java-based yt-dlp implementation)
- **Stream Processing**: FFmpegKit integration for advanced formats

Thanks
to [@cuongpm](https://github.com/cuongpm), [@yausername](https://github.com/yausername) and [@JunkFood02](https://github.com/JunkFood02)

Inspired from [cuongpm/youtube-dl-android](https://github.com/cuongpm/youtube-dl-android)

Support for project are welcome:) BTC wallet: [bc1q97xgwurjf2p5at9kzm96fkxymf3rh6gfmfq8fj](bitcoin:BC1Q97XGWURJF2P5AT9KZM96FKXYMF3RH6GFMFQ8FJ)

## Screenshots

<img src="screenshots/screenshot_1.png" width="170"> <img src="screenshots/screenshot_2.png" width="170"> <img src="screenshots/screenshot_3.png" width="170"> <img src="screenshots/screenshot_4.png" width="170">
<img src="screenshots/screenshot_5.png" width="170"> <img src="screenshots/screenshot_6.png" width="170"> <img src="screenshots/screenshot_7.png" width="170"> <img src="screenshots/screenshot_8.png" width="520">

## Translations

Please help with translations using the [Weblate](https://toolate.othing.xyz/projects/super-video-downloader/).

<a href="https://toolate.othing.xyz/projects/super-video-downloader/">
<img alt="Translation status" src="https://toolate.othing.xyz/widget/super-video-downloader/multi-auto.svg"/>
</a>

## Major technologies

- Language: Kotlin
- Architecture: MVVM
- Android architecture components: ViewModel, LiveData, Room
- Dependency injection: Dagger2
- Network: Retrofit, Okhttp
- Testing: JUnit, Espresso, Mockito
- Data layer with repository pattern and Coroutines and RxJava

## License

This package is licensed under the [LICENSE](./LICENSE) for details.
