package com.mytube.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

private data class CommentLanguage(val label: String, val code: String)

private val commentLanguages = listOf(
    CommentLanguage("বাংলা", TranslateLanguage.BENGALI),
    CommentLanguage("English", TranslateLanguage.ENGLISH),
    CommentLanguage("हिन्दी", TranslateLanguage.HINDI)
)

@Composable
fun CommentAssistant(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val preferences = remember { context.getSharedPreferences("mytube_preferences", Context.MODE_PRIVATE) }
    var geminiApiKey by remember { mutableStateOf(preferences.getString("gemini_api_key", "") ?: "") }
    val scope = rememberCoroutineScope()
    var reaction by remember { mutableStateOf("") }
    var videoContext by remember { mutableStateOf("") }
    var sourceLanguage by remember { mutableStateOf(commentLanguages[0]) }
    var targetLanguage by remember { mutableStateOf(commentLanguages[1]) }
    var firmTone by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var translated by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun translateDraft(text: String) {
        if (text.isBlank()) return
        if (sourceLanguage.code == targetLanguage.code) {
            translated = text
            status = "Source and target languages are the same."
            return
        }
        busy = true
        status = "Downloading the free language model if needed…"
        translateOnDevice(
            context = context,
            text = text,
            source = sourceLanguage.code,
            target = targetLanguage.code,
            onSuccess = {
                translated = it
                status = "Translation ready. Review it before copying."
                busy = false
            },
            onError = {
                status = "Translation failed: $it"
                busy = false
            }
        )
    }

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("AI Comment Assistant", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("তোমার প্রতিক্রিয়া থেকে ভদ্র, যুক্তিসহ কমেন্টের খসড়া তৈরি করো এবং অন্য ভাষায় অনুবাদ করো।")
        }
        item {
            OutlinedTextField(
                value = reaction,
                onValueChange = { reaction = it; draft = ""; translated = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("তোমার প্রতিক্রিয়া / Your reaction") },
                placeholder = { Text("যেমন: এই ভিডিও দেখে আমার রাগ হচ্ছে, কারণ তথ্যগুলো ভুল মনে হচ্ছে…") },
                minLines = 3
            )
        }
        item {
            OutlinedTextField(
                value = videoContext,
                onValueChange = { videoContext = it; draft = ""; translated = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ভিডিওর বিষয় বা শিরোনাম (ঐচ্ছিক)") },
                minLines = 2
            )
        }
        item {
            OutlinedTextField(
                value = geminiApiKey,
                onValueChange = {
                    geminiApiKey = it
                    preferences.edit().putString("gemini_api_key", it.trim()).apply()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini API key") },
                placeholder = { Text("Google AI Studio থেকে API key বসাও") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Text(
                "Gemini AI দিয়ে comment তৈরি হবে। API key এই ডিভাইসে রাখা হবে; public GitHub-এ দিও না।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text("কমেন্টের ভাষা", style = MaterialTheme.typography.titleMedium)
            LanguagePicker("তুমি যে ভাষায় লিখেছ", sourceLanguage) {
                sourceLanguage = it
                translated = ""
            }
            Spacer(Modifier.height(8.dp))
            LanguagePicker("অনুবাদের ভাষা", targetLanguage) {
                targetLanguage = it
                translated = ""
            }
        }
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("কমেন্টের ধরন", modifier = Modifier.weight(1f))
                FilterChip(selected = !firmTone, onClick = { firmTone = false }, label = { Text("ভদ্র") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = firmTone, onClick = { firmTone = true }, label = { Text("দৃঢ়") })
            }
        }
        item {
            Button(
                onClick = {
                    draft = CommentDraftEngine.create(reaction, videoContext, sourceLanguage.code, firmTone)
                    translated = ""
                    translateDraft(draft)
                },
                enabled = reaction.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Translate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("কমেন্ট তৈরি ও অনুবাদ")
            }
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (status.isNotBlank()) item {
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (draft.isNotBlank()) {
            item {
                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Original comment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(draft)
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Edit original comment") },
                            minLines = 3
                        )
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(draft)) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Copy original")
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { translateDraft(draft) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("আবার অনুবাদ করো") }
            }
        }
        if (translated.isNotBlank()) {
            item {
                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Translated comment — ${targetLanguage.label}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = translated,
                            onValueChange = { translated = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Edit translated comment") },
                            minLines = 3
                        )
                        Button(onClick = { clipboard.setText(AnnotatedString(translated)) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Copy translated comment")
                        }
                    }
                }
            }
        }
        item {
            Text(
                "এটি প্রথম offline draft assistant; কোনো ভিডিও নিজে দেখে fact-check করে না। অনুবাদের জন্য ভাষার মডেল প্রথমবার ইন্টারনেট থেকে নামতে পারে, এরপর ডিভাইসেই অনুবাদ হয়। পোস্ট করার আগে দুই ভাষার খসড়া যাচাই করে YouTube-এ নিজে Post করো।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanguagePicker(
    label: String,
    selected: CommentLanguage,
    onSelected: (CommentLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(selected.label) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                commentLanguages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.label) },
                        onClick = { onSelected(language); expanded = false }
                    )
                }
            }
        }
    }
}

private fun translateOnDevice(
    context: Context,
    text: String,
    source: String,
    target: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val options = TranslatorOptions.Builder()
        .setSourceLanguage(source)
        .setTargetLanguage(target)
        .build()
    val translator: Translator = Translation.getClient(options)
    translator.downloadModelIfNeeded()
        .addOnSuccessListener {
            translator.translate(text)
                .addOnSuccessListener { result -> translator.close(); onSuccess(result) }
                .addOnFailureListener { error -> translator.close(); onError(error.localizedMessage ?: "Unknown error") }
        }
        .addOnFailureListener { error ->
            translator.close()
            onError(error.localizedMessage ?: "Could not download language model")
        }
}

private object CommentDraftEngine {
    fun create(reaction: String, context: String, language: String, firm: Boolean): String {
        val text = reaction.trim()
        val subject = context.trim()
        val lower = text.lowercase()
        val concern = when {
            listOf("fake", "ভুল", "মিথ্যা", "ভুয়ো", "ভুয়া", "misleading", "ভ্রান্ত", "गलत", "झूठ").any { lower.contains(it) } ->
                when (language) {
                    TranslateLanguage.HINDI -> "कुछ दावों के समर्थन में पर्याप्त प्रमाण या विश्वसनीय स्रोत नहीं दिख रहे हैं"
                    TranslateLanguage.ENGLISH -> "some claims do not appear to have enough evidence or reliable sources"
                    else -> "কিছু বক্তব্যের পক্ষে যথেষ্ট প্রমাণ বা নির্ভরযোগ্য সূত্র দেখা যাচ্ছে না"
                }
            listOf("source", "সূত্র", "প্রমাণ", "reference", "তথ্য", "स्रोत", "सबूत").any { lower.contains(it) } ->
                when (language) {
                    TranslateLanguage.HINDI -> "वीडियो में दिए गए दावों के स्रोत और प्रमाण स्पष्ट नहीं हैं"
                    TranslateLanguage.ENGLISH -> "the sources and evidence behind the claims are not clear"
                    else -> "ভিডিওর দাবিগুলোর সূত্র ও প্রমাণ পরিষ্কার নয়"
                }
            listOf("angry", "রাগ", "বিরক্ত", "খারাপ", "upset", "রাগ হচ্ছে").any { lower.contains(it) } ->
                when (language) {
                    TranslateLanguage.HINDI -> "कुछ बातें बिना पर्याप्त संदर्भ या स्पष्ट प्रमाण के प्रस्तुत की गई हैं"
                    TranslateLanguage.ENGLISH -> "some points seem to be presented without enough context or clear evidence"
                    else -> "কিছু বিষয় যথেষ্ট প্রেক্ষাপট বা স্পষ্ট প্রমাণ ছাড়াই তুলে ধরা হয়েছে"
                }
            else -> when (language) {
                TranslateLanguage.HINDI -> "कुछ बातें और स्पष्ट उदाहरणों तथा विश्वसनीय स्रोतों के साथ समझाई जा सकती थीं"
                TranslateLanguage.ENGLISH -> "some points could be explained more clearly with examples and reliable sources"
                else -> "কিছু বিষয় আরও পরিষ্কারভাবে উদাহরণ ও নির্ভরযোগ্য সূত্রসহ ব্যাখ্যা করা যেত"
            }
        }
        return when (language) {
            TranslateLanguage.ENGLISH -> {
                val opening = if (firm) "I respectfully disagree with parts of this video." else "Thank you for sharing this video."
                val about = if (subject.isBlank()) "" else " Regarding \"$subject\","
                "$opening$about $concern. Please add clear references and context so viewers can evaluate the information fairly. My reaction: $text"
            }
            TranslateLanguage.HINDI -> {
                val opening = if (firm) "मैं इस वीडियो की कुछ बातों से सम्मानपूर्वक असहमत हूँ।" else "यह वीडियो साझा करने के लिए धन्यवाद।"
                val about = if (subject.isBlank()) "" else " \"$subject\" के बारे में,"
                "$opening$about $concern। कृपया दर्शकों की सही समझ के लिए स्पष्ट संदर्भ और विश्वसनीय स्रोत जोड़ें। मेरी प्रतिक्रिया: $text"
            }
            else -> {
                val opening = if (firm) "ভিডিওটির কিছু বক্তব্যের সঙ্গে আমি সম্মানের সঙ্গে দ্বিমত পোষণ করছি।" else "ভিডিওটি শেয়ার করার জন্য ধন্যবাদ।"
                val about = if (subject.isBlank()) "" else " \"$subject\" বিষয়ে,"
                "$opening$about $concern। দর্শকদের সঠিকভাবে বোঝার জন্য অনুগ্রহ করে স্পষ্ট প্রেক্ষাপট ও নির্ভরযোগ্য সূত্র যোগ করুন। আমার প্রতিক্রিয়া: $text"
            }
        }
    }
}
