package com.mytube.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Video(val title: String, val channel: String, val id: String)
private val sampleVideos = listOf(
    Video("Welcome to MyTube", "MyTube", "M7lc1UVf-VE"),
    Video("Explore videos from YouTube", "YouTube", "jNQXAC9IVRw"),
    Video("Search and discover videos", "MyTube Demo", "dQw4w9WgXcQ")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("mytube_preferences", MODE_PRIVATE)
        setContent {
            var ownAdsEnabled by remember { mutableStateOf(preferences.getBoolean("own_ads_enabled", true)) }
            MaterialTheme(colorScheme = darkColorScheme()) {
                MyTubeScreen(ownAdsEnabled, onOwnAdsChanged = {
                    ownAdsEnabled = it
                    preferences.edit().putBoolean("own_ads_enabled", it).apply()
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTubeScreen(ownAdsEnabled: Boolean, onOwnAdsChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf("Home") }
    var query by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyTube", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { tab = "Search" }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    IconButton(onClick = { tab = "Profile" }) { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") }
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
            "Search" -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Search YouTube") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                Text("Live search will be connected after a YouTube Data API key is configured.")
            }
            "Profile" -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                Text("Your account", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Google sign-in is not configured yet.")
                Spacer(Modifier.height(24.dp))
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Show MyTube ads")
                        Text("Controls only ads owned by this app.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = ownAdsEnabled, onCheckedChange = onOwnAdsChanged)
                }
                Spacer(Modifier.height(12.dp))
                Text("This setting does not hide or alter advertisements served by YouTube.",
                    style = MaterialTheme.typography.bodySmall)
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text(when (tab) {
                        "Shorts" -> "Shorts"
                        "Subscriptions" -> "Subscriptions"
                        "Library" -> "Your library"
                        else -> "Discover"
                    }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Starter preview · live YouTube data is not connected yet.")
                }
                items(sampleVideos) { video ->
                    Card(modifier = Modifier.fillMaxWidth().clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/watch?v=${video.id}")))
                    }, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Surface(Modifier.fillMaxWidth().height(160.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = "Open video", modifier = Modifier.size(58.dp))
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(video.title, fontWeight = FontWeight.SemiBold)
                            Text(video.channel, style = MaterialTheme.typography.bodySmall)
                            Text("Opens with YouTube", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
