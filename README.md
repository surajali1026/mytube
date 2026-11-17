# MyTube Android

Kotlin + Jetpack Compose Android starter.

## Included
- Native dark Material 3 interface
- Home, Shorts, Subscriptions, Library, Search and Profile navigation
- Live YouTube education feed using YouTube Data API v3 (requires user-supplied API key)
- Automatic metadata-based filtering and ranking of educational video search results
- Education-only feed preference (saved locally)
- Offline authenticity-signal checker for manually entered video title, channel and description
- Detects educational wording, source links, selected research/public-information domains, clickbait phrases and movie-clip wording
- Shows a transparent metadata signal score with reasons and warnings
- Persistent toggle for MyTube-owned ads only
- Playback preference toggles for background audio, picture-in-picture, auto-play next and downloads
- GitHub Actions debug APK build

## Authenticity checker limitations
The checker is a lightweight, free, on-device text heuristic. The Home feed fetches public video metadata using YouTube Data API v3 and applies the same heuristic to titles, channel names and descriptions before ranking results. It does not watch video/audio, verify every claim, validate channel identity, or prove factual accuracy. A high score is not a truth guarantee.

## YouTube and playback status
The Home education feed uses one YouTube Data API search request when opened/refreshed. Add a YouTube Data API v3 key in Profile settings; the key is stored only in the app's local preferences. Search quota is limited by Google's project quota. Google account sign-in and subscriptions/history sync are not yet connected. YouTube videos are not downloaded or stream-extracted. Background audio, PiP, auto-play and download switches are preferences only; their media services are not connected. YouTube playback must continue through policy-compliant official YouTube experiences.

The app does not hide or alter advertisements served by YouTube.

## Free setup and credentials
The offline checker needs no API key. The live public-video feed requires a YouTube Data API v3 key: create a Google Cloud project, enable YouTube Data API v3, create an API key, and restrict it to the YouTube Data API. Paste it in MyTube → Profile. Do not commit API keys or OAuth secrets to this public repository. Google Sign-In requires an Android OAuth client for package com.mytube.app and the signing certificate SHA-1, plus secure backend ID-token validation; those credentials/backend are not configured in this repository.

Review:
- https://developers.google.com/youtube/terms/developer-policies
- https://developers.google.com/youtube/v3/getting-started

## Build
GitHub Actions builds a debug APK on every push to main and on manual dispatch. Download the APK from the workflow run's Artifacts section after a successful build.
