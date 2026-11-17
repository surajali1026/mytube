# MyTube Android

Kotlin + Jetpack Compose Android starter.

## Included
- Native dark Material 3 interface
- Home, Shorts, Subscriptions, Library, Search and Profile navigation
- Persistent toggle for MyTube-owned ads only
- Video links open through the official YouTube app or browser
- GitHub Actions debug APK build

## Current limitations
This first build is a starter UI. Google sign-in, live YouTube search, subscriptions/history sync, AdMob, and any supported embedded playback integration are not connected yet.

YouTube-served advertisements are not blocked or modified. The app does not extract streams, download videos, or implement unauthorized background playback. Video links are handed to the official YouTube app/browser.

Review YouTube Developer Policies before adding API features:
https://developers.google.com/youtube/terms/developer-policies
https://developers.google.com/youtube/v3/getting-started

Do not commit API keys or OAuth secrets to this public repository. Configure credentials securely.

## Build
GitHub Actions builds a debug APK on every push to main and on manual dispatch. Download the APK from the workflow run's Artifacts section.
