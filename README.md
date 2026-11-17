# MyTube Android

Kotlin + Jetpack Compose Android starter.

## Included
- Native dark Material 3 interface
- Home, Shorts, Subscriptions, Library, Search and Profile navigation
- Persistent toggle for MyTube-owned ads only
- Persistent playback preferences: background audio, picture-in-picture, auto-play next, and downloads
- Video links open through the official YouTube app or browser
- GitHub Actions debug APK build

## Playback status
The playback switches are preference controls only at this stage. A MyTube media player/service is not connected yet, so switching them does not start background playback, PiP, auto-play, or downloads. These features can be implemented for media that MyTube owns or is licensed to play/download.

YouTube-served videos continue to open in the official YouTube app/browser. The app does not extract streams, download YouTube videos, bypass YouTube background-play restrictions, or hide/alter YouTube advertisements.

## Not connected yet
Google sign-in, live YouTube search, subscriptions/history sync, AdMob, and authorized in-app media playback are not connected yet.

Review YouTube Developer Policies before adding API features:
https://developers.google.com/youtube/terms/developer-policies
https://developers.google.com/youtube/v3/getting-started

Do not commit API keys or OAuth secrets to this public repository. Configure credentials securely.

## Build
GitHub Actions builds a debug APK on every push to main and on manual dispatch. Download the APK from the workflow run's Artifacts section.
