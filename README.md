# MyTube Android

Kotlin + Jetpack Compose Android starter.

## Included
- Native dark Material 3 interface
- Home, Shorts, Subscriptions, Library, Search and Profile navigation
- Education-only feed preference (saved locally)
- Offline authenticity-signal checker for manually entered video title, channel and description
- Detects educational wording, source links, selected research/public-information domains, clickbait phrases and movie-clip wording
- Shows a transparent metadata signal score with reasons and warnings
- Persistent toggle for MyTube-owned ads only
- Playback preference toggles for background audio, picture-in-picture, auto-play next and downloads
- GitHub Actions debug APK build

## Authenticity checker limitations
The checker is a lightweight, free, on-device text heuristic. It does not fetch YouTube videos, watch video/audio, verify every claim, validate channel identity, or prove factual accuracy. A high score is not a truth guarantee. The education-only setting is stored, but a live filtered YouTube feed is not connected yet.

## YouTube and playback status
Live YouTube search, subscriptions/history sync, and a feed populated from YouTube Data API are not connected yet. YouTube videos are not downloaded or stream-extracted. Background audio, PiP, auto-play and download switches are preferences only; their media services are not connected. YouTube playback must continue through policy-compliant official YouTube experiences.

The app does not hide or alter advertisements served by YouTube.

## Free setup and credentials
The offline checker needs no API key and runs locally. A future live YouTube search integration may use the YouTube Data API, subject to quota and developer policies. Do not commit API keys or OAuth secrets to this public repository; configure credentials securely.

Review:
- https://developers.google.com/youtube/terms/developer-policies
- https://developers.google.com/youtube/v3/getting-started

## Build
GitHub Actions builds a debug APK on every push to main and on manual dispatch. Download the APK from the workflow run's Artifacts section after a successful build.
