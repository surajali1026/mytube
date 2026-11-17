package com.mytube.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("mytube_preferences", MODE_PRIVATE)
        setContent {
            var ownAdsEnabled by remember { mutableStateOf(preferences.getBoolean("own_ads_enabled", true)) }
            var backgroundAudio by remember { mutableStateOf(preferences.getBoolean("background_audio", false)) }
            var pictureInPicture by remember { mutableStateOf(preferences.getBoolean("picture_in_picture", true)) }
            var autoPlayNext by remember { mutableStateOf(preferences.getBoolean("auto_play_next", false)) }
            var downloadsEnabled by remember { mutableStateOf(preferences.getBoolean("downloads_enabled", false)) }
            var educationOnly by remember { mutableStateOf(preferences.getBoolean("education_only", true)) }
            var youtubeApiKey by remember { mutableStateOf(preferences.getString("youtube_api_key", "") ?: "") }
            var feedRefreshToken by remember { mutableIntStateOf(0) }

            MaterialTheme(colorScheme = darkColorScheme()) {
                MyTubeScreen(
                    educationOnly = educationOnly,
                    youtubeApiKey = youtubeApiKey,
                    onYoutubeApiKeyChanged = {
                        youtubeApiKey = it
                        preferences.edit().putString("youtube_api_key", it).apply()
                    },
                    feedRefreshToken = feedRefreshToken,
                    onRefreshFeed = { feedRefreshToken++ },
                    onEducationOnlyChanged = {
                        educationOnly = it
                        preferences.edit().putBoolean("education_only", it).apply()
                    },
                    ownAdsEnabled = ownAdsEnabled,
                    onOwnAdsChanged = { ownAdsEnabled = it; preferences.edit().putBoolean("own_ads_enabled", it).apply() },
                    backgroundAudio = backgroundAudio,
                    onBackgroundAudioChanged = { backgroundAudio = it; preferences.edit().putBoolean("background_audio", it).apply() },
                    pictureInPicture = pictureInPicture,
                    onPictureInPictureChanged = { pictureInPicture = it; preferences.edit().putBoolean("picture_in_picture", it).apply() },
                    autoPlayNext = autoPlayNext,
                    onAutoPlayNextChanged = { autoPlayNext = it; preferences.edit().putBoolean("auto_play_next", it).apply() },
                    downloadsEnabled = downloadsEnabled,
                    onDownloadsEnabledChanged = { downloadsEnabled = it; preferences.edit().putBoolean("downloads_enabled", it).apply() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTubeScreen(
    educationOnly: Boolean,
    youtubeApiKey: String,
    onYoutubeApiKeyChanged: (String) -> Unit,
    feedRefreshToken: Int,
    onRefreshFeed: () -> Unit,
    onEducationOnlyChanged: (Boolean) -> Unit,
    ownAdsEnabled: Boolean,
    onOwnAdsChanged: (Boolean) -> Unit,
    backgroundAudio: Boolean,
    onBackgroundAudioChanged: (Boolean) -> Unit,
    pictureInPicture: Boolean,
    onPictureInPictureChanged: (Boolean) -> Unit,
    autoPlayNext: Boolean,
    onAutoPlayNextChanged: (Boolean) -> Unit,
    downloadsEnabled: Boolean,
    onDownloadsEnabledChanged: (Boolean) -> Unit
) {
    var tab by remember { mutableStateOf("Home") }
    var playingQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var playingIndex by remember { mutableIntStateOf(0) }
    var playerOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyTube", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { tab = "Comment" }) {
                        Icon(Icons.Default.Translate, contentDescription = "AI Comment Assistant")
                    }
                    IconButton(onClick = { tab = "Verify" }) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "Authenticity checker")
                    }
                    IconButton(onClick = { tab = "Profile" }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf("Home" to Icons.Default.Home, "Shorts" to Icons.Default.SlowMotionVideo,
                    "Subscriptions" to Icons.Default.Subscriptions, "Library" to Icons.Default.VideoLibrary
                ).forEach { (label, icon) ->
                    NavigationBarItem(selected = tab == label, onClick = { tab = label },
                        icon = { Icon(icon, contentDescription = label) }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        when (tab) {
            "Verify" -> AuthenticityChecker(Modifier.fillMaxSize().padding(padding))
            "Comment" -> CommentAssistant(Modifier.fillMaxSize().padding(padding))
            "Home" -> EducationFeed(
                apiKey = youtubeApiKey,
                educationOnly = educationOnly,
                refreshToken = feedRefreshToken,
                onOpenSettings = { tab = "Profile" },
                onRefresh = onRefreshFeed,
                onPlayVideo = { videos, index ->
                    playingQueue = videos.map { it.id }
                    playingIndex = index.coerceIn(videos.indices)
                    playerOpen = true
                },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            "Subscriptions" -> PersonalYouTubeFeed(
                onPlayVideo = { ids, index ->
                    playingQueue = ids
                    playingIndex = index.coerceIn(ids.indices)
                    playerOpen = true
                },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            "Profile" -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Your account", style = MaterialTheme.typography.headlineSmall) }
                item { GoogleLoginCard() }
                item { HorizontalDivider() }
                item { Text("YouTube feed connection", style = MaterialTheme.typography.titleLarge) }
                item {
                    OutlinedTextField(
                        value = youtubeApiKey,
                        onValueChange = onYoutubeApiKeyChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("YouTube Data API v3 key") },
                        placeholder = { Text("Paste your API key") },
                        singleLine = true
                    )
                    Text("Create a Google Cloud project, enable YouTube Data API v3, create an API key and restrict it to this API. The key is saved only in this app's local preferences; do not put it in GitHub.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onRefreshFeed, enabled = youtubeApiKey.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text("Save key and refresh education feed")
                    }
                }
                item { HorizontalDivider() }
                item { Text("Feed preferences", style = MaterialTheme.typography.titleLarge) }
                item { SettingSwitch("Education-only feed", "ON: rank and filter educational videos. OFF: show broader safe-search results.", educationOnly, onEducationOnlyChanged) }
                item { Text("Playback settings", style = MaterialTheme.typography.titleLarge) }
                item { SettingSwitch("Background play", "Only provider-permitted playback can continue in background. YouTube embedded videos may pause when app is backgrounded.", backgroundAudio, onBackgroundAudioChanged) }
                item { SettingSwitch("Picture-in-picture", "Floating video window for supported MyTube playback.", pictureInPicture, onPictureInPictureChanged) }
                item { SettingSwitch("Auto-play next", "Play the next video in the current feed when the current video ends.", autoPlayNext, onAutoPlayNextChanged) }
                item { SettingSwitch("Allow downloads", "Only for videos you own or are licensed to download.", downloadsEnabled, onDownloadsEnabledChanged) }
                item { HorizontalDivider() }
                item { Text("App settings", style = MaterialTheme.typography.titleLarge) }
                item { SettingSwitch("Show MyTube ads", "Controls only ads owned by this app.", ownAdsEnabled, onOwnAdsChanged) }
                item {
                    Text(
                        "The feed is connected to YouTube Data API v3 when you add your own API key. The checker and feed use title, channel and description metadata only; they do not watch videos or verify factual claims.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Text(
                        "MyTube ads toggle controls only ads owned by this app; it does not remove YouTube ads. Background play is subject to YouTube embedded-player restrictions. MyTube does not extract or download YouTube streams.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        when (tab) {
                            "Shorts" -> "Shorts"
                            "Subscriptions" -> "Subscriptions"
                            "Library" -> "Your library"
                            else -> if (educationOnly) "Education feed" else "Discover"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                            Text(
                                if (educationOnly) "Education-only mode is ON" else "Discover mode",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("This section is not connected to private YouTube account data yet. Open the official YouTube app to use this feature.")
                            Button(onClick = { tab = "Verify" }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Shield, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Check a video before watching")
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Shorts, subscriptions and personal library sync require additional playback and YouTube OAuth integration. They are not active in this build.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    if (playerOpen && playingQueue.isNotEmpty()) {
        YouTubeFullscreenPlayer(
            videoId = playingQueue[playingIndex],
            onClose = { playerOpen = false },
            onEnded = {
                if (autoPlayNext && playingIndex < playingQueue.lastIndex) playingIndex++
                else playerOpen = false
            }
        )
    }
    }
}

@Composable
private fun AuthenticityChecker(modifier: Modifier = Modifier) {
    var title by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var report by remember { mutableStateOf<AuthenticityReport?>(null) }

    LazyColumn(
        modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Authenticity check", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Paste the video's public details. This free offline check looks for educational, source and clickbait signals.")
        }
        item {
            OutlinedTextField(title, { title = it; report = null }, Modifier.fillMaxWidth(),
                label = { Text("Video title") }, minLines = 2)
        }
        item {
            OutlinedTextField(channel, { channel = it; report = null }, Modifier.fillMaxWidth(),
                label = { Text("Channel name") }, singleLine = true)
        }
        item {
            OutlinedTextField(description, { description = it; report = null }, Modifier.fillMaxWidth(),
                label = { Text("Video description / source links") }, minLines = 5)
        }
        item {
            Button(
                onClick = { report = AuthenticityEngine.evaluate(title, channel, description) },
                enabled = title.isNotBlank() && channel.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FactCheck, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Check authenticity signals")
            }
        }
        report?.let { result ->
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Metadata signal score", style = MaterialTheme.typography.titleMedium)
                        Text("${result.score}/100", style = MaterialTheme.typography.displaySmall,
                            color = when {
                                result.score >= 75 -> MaterialTheme.colorScheme.primary
                                result.score >= 55 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }, fontWeight = FontWeight.Bold)
                        Text(result.verdict, fontWeight = FontWeight.SemiBold)
                        HorizontalDivider()
                        Text("Positive signals", fontWeight = FontWeight.SemiBold)
                        if (result.reasons.isEmpty()) Text("No positive signals detected.")
                        result.reasons.forEach { Text("• $it") }
                        Text("Warnings", fontWeight = FontWeight.SemiBold)
                        if (result.warnings.isEmpty()) Text("No metadata warnings detected.")
                        result.warnings.forEach { Text("• $it") }
                    }
                }
            }
            item {
                Text(
                    "Important: this is a simple text-based heuristic, not a fact-checking verdict. A high score is not proof that the video is true; open and verify the cited sources yourself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
