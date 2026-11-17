package com.mytube.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val YOUTUBE_READONLY_SCOPE = "https://www.googleapis.com/auth/youtube.readonly"

private data class PersonalVideo(
    val id: String, val title: String, val channel: String,
    val thumbnail: String, val publishedAt: String, val source: String
)
private data class PersonalFeedData(val videos: List<PersonalVideo>, val subscriptions: List<String>)

@Composable
fun PersonalYouTubeFeed(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity
    val preferences = remember { context.getSharedPreferences("mytube_preferences", 0) }
    var interests by remember { mutableStateOf(preferences.getString("personal_feed_interests", "science, technology, engineering, trading, education") ?: "") }
    var videos by remember { mutableStateOf<List<PersonalVideo>>(emptyList()) }
    var subscriptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var status by remember { mutableStateOf("Connect your YouTube account to load subscriptions and personalized videos.") }
    var busy by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadWithToken(token: String) {
        scope.launch {
            busy = true
            status = "Loading your subscriptions and personal feed…"
            try {
                val data = withContext(Dispatchers.IO) { PersonalYouTubeRepository.load(token, interests) }
                videos = data.videos
                subscriptions = data.subscriptions
                connected = true
                status = if (data.videos.isEmpty()) "No videos found. Change your interests or refresh." else "Loaded ${{data.videos.size} videos from your subscriptions and interests."
            } catch (e: Exception) {
                status = e.localizedMessage ?: "Could not load your YouTube feed."
            } finally { busy = false }
        }
    }

    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val intent = result.data ?: throw IllegalStateException("Google authorization returned no result.")
            val auth = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(intent)
            val token = auth.accessToken
            if (token.isNullOrBlank()) throw IllegalStateException("Google did not return a YouTube access token.")
            loadWithToken(token)
        } catch (e: Exception) {
            busy = false
            status = e.localizedMessage ?: "YouTube authorization failed or was cancelled."
        }
    }

    fun connectAndLoad() {
        if (activity == null) { status = "Could not open Google authorization."; return }
        busy = true
        status = "Requesting permission to view your YouTube account…"
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(YOUTUBE_READONLY_SCOPE)))
            .build()
        Identity.getAuthorizationClient(activity).authorize(request)
            .addOnSuccessListener { auth ->
                if (auth.hasResolution()) {
                    val pending = auth.pendingIntent
                    if (pending == null) {
                        busy = false
                        status = "Google needs to show the consent screen again. Please retry."
                    } else authorizationLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    val token = auth.accessToken
                    if (token.isNullOrBlank()) {
                        busy = false
                        status = "Google did not return a YouTube access token."
                    } else loadWithToken(token)
                }
            }
            .addOnFailureListener { error ->
                busy = false
                status = error.localizedMessage ?: "Could not start YouTube authorization."
            }
    }

    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Your YouTube", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Subscriptions + videos selected from your interests")
        }
        item {
            OutlinedTextField(
                value = interests,
                onValueChange = { interests = it; preferences.edit().putString("personal_feed_interests", it).apply() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your interests") },
                placeholder = { Text("e.g. science, coding, trading, history") },
                minLines = 2
            )
        }
        item {
            Button(onClick = { connectAndLoad() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (connected) "Refresh my personal feed" else "Connect YouTube & load my feed")
            }
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (status.isNotBlank()) item { Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (subscriptions.isNotEmpty()) {
            item { Text("Your subscriptions (${{subscriptions.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            items(subscriptions) { channel -> AssistChip(onClick = {}, label = { Text(channel) }) }
        }
        if (videos.isNotEmpty()) {
            item { Text("Videos for you", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            items(videos, key = { it.id }) { video ->
                Card(Modifier.fillMaxWidth().clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${{video.id}")))
                }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (video.thumbnail.isNotBlank()) coil.compose.AsyncImage(
                            model = video.thumbnail, contentDescription = video.title,
                            modifier = Modifier.fillMaxWidth().height(190.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Text(video.title, fontWeight = FontWeight.SemiBold)
                        Text(video.channel, style = MaterialTheme.typography.bodySmall)
                        Text(video.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            Text("This combines recent uploads from your subscribed channels with YouTube search results based on your interests. It is not YouTube's proprietary Home recommendation algorithm. Videos open in the official YouTube app or browser.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private object PersonalYouTubeRepository {
    private const val API = "https://www.googleapis.com/youtube/v3"

    fun load(token: String, interests: String): PersonalFeedData {
        val subs = get("$API/subscriptions?part=snippet,contentDetails&mine=true&maxResults=50&order=unread", token)
            .optJSONArray("items")
        val channelIds = mutableListOf<String>()
        val channelNames = mutableListOf<String>()
        if (subs != null) for (i in 0 until subs.length()) {
            val snippet = subs.optJSONObject(i)?.optJSONObject("snippet") ?: continue
            val id = snippet.optJSONObject("resourceId")?.optString("channelId").orEmpty()
            if (id.isNotBlank()) { channelIds += id; channelNames += snippet.optString("title") }
        }

        val uploads = mutableMapOf<String, String>()
        channelIds.chunked(50).forEach { chunk ->
            val ids = enc(chunk.joinToString(","))
            val items = get("$API/channels?part=contentDetails&id=$ids", token).optJSONArray("items")
            if (items != null) for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val playlist = item.optJSONObject("contentDetails")?.optJSONObject("relatedPlaylists")?.optString("uploads").orEmpty()
                if (playlist.isNotBlank()) uploads[item.optString("id")] = playlist
            }
        }

        val result = linkedMapOf<String, PersonalVideo>()
        // Limit channel playlist calls to 25 per refresh to keep API quota and loading time reasonable.
        channelIds.take(25).forEach { channelId ->
            val playlist = uploads[channelId] ?: return@forEach
            val items = get("$API/playlistItems?part=snippet&playlistId=${{enc(playlist)}&maxResults=3", token).optJSONArray("items") ?: return@forEach
            for (i in 0 until items.length()) {
                val snippet = items.optJSONObject(i)?.optJSONObject("snippet") ?: continue
                val id = snippet.optJSONObject("resourceId")?.optString("videoId").orEmpty()
                if (id.isBlank()) continue
                val thumbs = snippet.optJSONObject("thumbnails")
                val thumb = thumbs?.optJSONObject("high")?.optString("url") ?: thumbs?.optJSONObject("medium")?.optString("url").orEmpty()
                result[id] = PersonalVideo(id, snippet.optString("title"), snippet.optString("channelTitle"), thumb, snippet.optString("publishedAt"), "From your subscriptions")
            }
        }

        val query = interests.trim().ifBlank { "science technology education" }
        val found = get("$API/search?part=snippet&type=video&maxResults=20&order=relevance&safeSearch=strict&regionCode=IN&relevanceLanguage=en&q=${{enc(query)}", token).optJSONArray("items")
        if (found != null) for (i in 0 until found.length()) {
            val item = found.optJSONObject(i) ?: continue
            val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
            val snippet = item.optJSONObject("snippet") ?: continue
            if (id.isBlank()) continue
            val thumbs = snippet.optJSONObject("thumbnails")
            val thumb = thumbs?.optJSONObject("high")?.optString("url") ?: thumbs?.optJSONObject("medium")?.optString("url").orEmpty()
            result.putIfAbsent(id, PersonalVideo(id, snippet.optString("title"), snippet.optString("channelTitle"), thumb, snippet.optString("publishedAt"), "Based on your interests"))
        }
        return PersonalFeedData(result.values.sortedByDescending { it.publishedAt }, channelNames.distinct())
    }

    private fun get(url: String, token: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 15000; readTimeout = 20000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val message = runCatching { JSONObject(body).getJSONObject("error").getString("message") }
                    .getOrDefault("YouTube API request failed ($code)")
                throw IllegalStateException(message)
            }
            return JSONObject(body)
        } finally { connection.disconnect() }
    }
    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}
