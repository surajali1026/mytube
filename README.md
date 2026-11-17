# MyTube Android

Kotlin + Jetpack Compose Android starter.

## Included
- Native dark Material 3 interface
- Home, Shorts, Subscriptions, Library, Search and Profile navigation
- Live YouTube education feed using YouTube Data API v3 (requires user-supplied API key)
- Automatic metadata-based filtering and ranking of educational video search results
- Education-only feed preference (saved locally)
- Gemini 3.8 Flash AI comment generation with editable drafts in Bengali, English and Hindi (user supplies Gemini API key)\n- Comment suggestion assistant with editable drafts in Bengali, English and Hindi\n- On-device ML Kit translation for Bengali, English and Hindi\n- Offline authenticity-signal checker for manually entered video title, channel and description
- Detects educational wording, source links, selected research/public-information domains, clickbait phrases and movie-clip wording
- Shows a transparent metadata signal score with reasons and warnings
- Persistent toggle for MyTube-owned ads only
- Playback preference toggles for background audio, picture-in-picture, auto-play next and downloads
- GitHub Actions debug APK build

## Authenticity checker limitations
The checker is a lightweight, free, on-device text heuristic. The Home feed fetches public video metadata using YouTube Data API v3 and applies the same heuristic to titles, channel names and descriptions before ranking results. It does not watch video/audio, verify every claim, validate channel identity, or prove factual accuracy. A high score is not a truth guarantee.

## YouTube and playback status
The Home education feed uses one YouTube Data API search request when opened/refreshed. Add a YouTube Data API v3 key in Profile settings; the key is stored only in the app's local preferences. Restrict the key to YouTube Data API v3 and, after you have a stable app signing certificate, restrict it to the Android package and certificate. Search quota is limited by Google's project quota. Google Credential Manager sign-in UI is implemented, but it requires your Google Cloud Android and Web OAuth client configuration. The app currently stores the returned name/email locally; there is no backend token verification. Private YouTube subscriptions, comments and watch history are not connected. YouTube videos are not downloaded or stream-extracted. Background audio, PiP, auto-play and download switches are preferences only; their media services are not connected. YouTube playback must continue through policy-compliant official YouTube experiences.

The app does not hide or alter advertisements served by YouTube.

## Free setup and credentials
The comment assistant can generate drafts using Gemini 3.8 Flash through the Interactions API when the user supplies a Gemini API key in the Comment Assistant. Without a key it falls back to a local template. The prompt uses only the user's typed reaction and optional video title/context; it does not watch the video or fact-check claims. The key is stored in local app preferences on the device; do not share the key or commit it to GitHub. Review and post comments manually. Translation uses on-device ML Kit models after their first download. The offline checker needs no API key. The live public-video feed requires a YouTube Data API v3 key: create a Google Cloud project, enable YouTube Data API v3, create an API key, and restrict it to the YouTube Data API. Paste it in MyTube → Profile. Do not commit API keys or OAuth secrets to this public repository. Google Sign-In uses Android Credential Manager. In Google Cloud, create (1) an Android OAuth client for package com.mytube.app and the SHA-1 of the certificate used to sign the installed APK, and (2) a Web application OAuth client. Paste the Web client ID in MyTube → Profile → Google account. The ID token is not sent to a backend in this starter app; do not use the local profile label as proof of identity for privileged server actions. Private YouTube data (subscriptions, comments, account library) needs separate YouTube OAuth authorization and consent scopes.

Review:
- https://developers.google.com/youtube/terms/developer-policies
- https://developers.google.com/youtube/v3/getting-started

## Build
GitHub Actions builds a debug APK on every push to main and on manual dispatch. Download the APK from the workflow run's Artifacts section after a successful build.
