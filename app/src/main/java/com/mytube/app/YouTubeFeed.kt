package com.mytube.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class FeedVideo(
    val id: String,
    val title: String,
    val channel: String,
    val description: String,
    val thumbnail: String,
    val publishedAt: String,
    val score: Int,
    val verdict: String,
    val reasons: List<String>,
    val warnings: List<String>
)

object YouTubeFeedRepository {
    private const val SEARCH_URL = "https://www.googleapis.com/youtube/v3/search"
    private const val EDUCATION_QUERY =
        "education|science|technology|programming|history|engineering|mathematics|documentary"

    fun load(apiKey: String): List<FeedVideo> {
        require(apiKey.isNotBlank()) { "Add a YouTube Data API key in Profile settings first." }
        val query = listOf(
            "part" to "snippet",
            "type" to "video",
            "q" to EDUCATION_QUERY,
            "maxResults" to "50",
            "regionCode" to "IN",
            "relevanceLanguage" to "en",
            "safeSearch" to "strict",
            "order" to "relevance",
            "key" to apiKey.trim()
        ).joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

        val connection = (URL("$SEARCH_URL?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 20000
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(body).getJSONObject("error").getString("message")
                }.getOrDefault("YouTube API request failed ($status)")
                throw IllegalStateException(message)
            }

            val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
            val results = mutableListOf<FeedVideo>()
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
                val snippet = item.optJSONObject("snippet") ?: continue
                if (id.isBlank()) continue
                val title = snippet.optString("title")
                val channel = snippet.optString("channelTitle")
                val description = snippet.optString("description")
                val report = AuthenticityEngine.evaluate(title, channel, description)
                if (report.score < 55 || report.verdict.startsWith("Likely entertainment")) continue
                val thumbnails = snippet.optJSONObject("thumbnails")
                val thumbnail = thumbnails?.optJSONObject("high")?.optString("url")
                    ?: thumbnails?.optJSONObject("medium")?.optString("url").orEmpty()
                results += FeedVideo(
                    id, title, channel, description, thumbnail,
                    snippet.optString("publishedAt"), report.score,
                    report.verdict, report.reasons, report.warnings
                )
            }
            return results.sortedByDescending { it.score }
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
fun EducationFeed(
    apiKey: String,
    refreshToken: Int,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videos by remember { mutableStateOf<List<FeedVideo>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(apiKey, refreshToken) {
        if (apiKey.isBlank()) {
            videos = emptyList()
            error = ""
            return@LaunchedEffect
        }
        loading = true
        error = ""
        try {
            videos = withContext(Dispatchers.IO) { YouTubeFeedRepository.load(apiKey) }
            if (videos.isEmpty()) error = "No videos passed the current education metadata filter. Try refreshing later."
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Could not load the YouTube feed."
            videos = emptyList()
        } finally {
            loading = false
        }
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Authentic learning feed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Education and knowledge videos ranked by metadata signals")
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh education feed")
                }
            }
        }
        if (apiKey.isBlank()) {
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Connect YouTube Data API", style = MaterialTheme.typography.titleLarge)
                        Text("A free API key is needed to fetch public YouTube videos. Add it in Profile → YouTube API key. The key stays on this device and is not committed to GitHub.")
                        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                            Text("Open feed settings")
                        }
                    }
                }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error.isNotBlank()) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(error, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        items(videos, key = { it.id }) { video ->
            Card(Modifier.fillMaxWidth().clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.id}"))
                context.startActivity(intent)
            }) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(video.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(video.channel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        AssistChip(onClick = {}, label = { Text("Metadata score ${video.score}/100") })
                        Spacer(Modifier.width(8.dp))
                        Text("Tap to watch on YouTube", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(video.verdict, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    if (video.reasons.isNotEmpty()) Text("Signals: ${video.reasons.joinToString(" • ")}", style = MaterialTheme.typography.bodySmall)
                    if (video.warnings.isNotEmpty()) Text("Review: ${video.warnings.take(2).joinToString(" • ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text(
                "Authenticity limitation: this feed automatically filters and ranks YouTube metadata only. It does not watch the video, verify its claims, or guarantee that a creator is truthful. Always check original sources.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
