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
                refreshToken = feedRefreshToken,
                onOpenSettings = { tab = "Profile" },
                onRefresh = onRefreshFeed,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            "Profile" -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Your account", style = MaterialTheme.typography.headlineSmall) }
                item {
                    Text("Google sign-in setup", style = MaterialTheme.typography.titleLarge)
                    Text("Google login is not connected yet. It requires an Android OAuth client configured for package com.mytube.app and this APK's SHA-1 signing certificate. Authentication also needs secure server-side ID-token verification.")
                    Text("YouTube account access (subscriptions/comments) is a separate permission request; signing in alone does not grant it.")
                }
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
                item { SettingSwitch("Education-only feed", "Hide entertainment and movie-clip content when a live feed is connected.", educationOnly, onEducationOnlyChanged) }
                item { Text("Playback settings", style = MaterialTheme.typography.titleLarge) }
                item { SettingSwitch("Background audio", "For MyTube-owned or licensed audio only.", backgroundAudio, onBackgroundAudioChanged) }
                item { SettingSwitch("Picture-in-picture", "Floating video window for supported MyTube playback.", pictureInPicture, onPictureInPictureChanged) }
                item { SettingSwitch("Auto-play next", "Automatically continue supported MyTube playlists.", autoPlayNext, onAutoPlayNextChanged) }
                item { SettingSwitch("Allow downloads", "Only for videos you own or are licensed to download.", downloadsEnabled, onDownloadsEnabledChanged) }
                item { HorizontalDivider() }
                item { Text("App settings", style = MaterialTheme.typography.titleLarge) }
                item { SettingSwitch("Show MyTube ads", "Controls only ads owned by this app.", ownAdsEnabled, onOwnAdsChanged) }
                item {
                    Text(
                        "The authenticity checker currently evaluates title, channel name and description text entered by you. It does not watch the video, verify every claim, or prove that a channel is trustworthy. Live YouTube search/feed is not connected yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Text(
                        "Playback and download controls are saved preferences only; their media services are not connected. YouTube links will use the official YouTube app/browser. MyTube will not extract or download YouTube streams or bypass background-play restrictions.",
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
                            Text("The live YouTube feed is not connected yet. No random demo videos are shown.")
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
                        "Next integration: connect YouTube Data API search, then run these filters on each result before it enters this feed. API credentials must be configured securely and must not be committed to this public repository.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
