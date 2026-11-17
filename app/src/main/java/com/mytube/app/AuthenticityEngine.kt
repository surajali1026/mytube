package com.mytube.app

data class AuthenticityReport(
    val score: Int,
    val verdict: String,
    val reasons: List<String>,
    val warnings: List<String>
)

/**
 * Offline metadata heuristic only. It does not watch the video or prove factual accuracy.
 * Scores describe signals in user-supplied title/channel/description text.
 */
object AuthenticityEngine {
    private val educationTerms = listOf(
        "education", "educational", "lecture", "tutorial", "course", "explained",
        "science", "history", "engineering", "technology", "programming", "coding",
        "mathematics", "physics", "chemistry", "biology", "economics", "research",
        "documentary", "lesson", "শিক্ষা", "বিজ্ঞান", "ইতিহাস", "প্রযুক্তি", "পাঠ"
    )
    private val clickbaitTerms = listOf(
        "shocking", "you won't believe", "must watch", "secret trick", "100% guaranteed",
        "exposed", "unbelievable", "viral", "সবাই অবাক", "গোপন রহস্য"
    )
    private val entertainmentTerms = listOf(
        "movie clip", "film scene", "trailer", "movie scene", "fan edit", "status video",
        "song video", "comedy scene", "web series clip", "shorts edit"
    )
    private val referenceDomains = listOf(
        "who.int", "un.org", "nasa.gov", "nature.com",
        "science.org", "pubmed.ncbi.nlm.nih.gov", "arxiv.org", "britannica.com"
    )

    fun evaluate(title: String, channel: String, description: String): AuthenticityReport {
        val all = "$title $channel $description".lowercase()
        val reasons = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var score = 45

        if (educationTerms.any { all.contains(it) }) {
            score += 20
            reasons += "Educational topic words found"
        } else {
            warnings += "Clear educational topic was not detected"
        }

        val urls = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE).findAll(description).map { it.value }.toList()
        if (urls.isNotEmpty()) {
            score += 10
            reasons += "Description contains source links"
        } else {
            warnings += "No source link found in description"
        }

        val linkedHosts = urls.mapNotNull { url ->
            runCatching { java.net.URI(url).host?.lowercase()?.removePrefix("www.") }.getOrNull()
        }
        val hasInstitutionalDomain = linkedHosts.any { host ->
            host.endsWith(".edu") || host.endsWith(".gov") || host.endsWith(".ac.in")
        }
        val hasKnownReference = linkedHosts.any { host ->
            referenceDomains.any { domain -> host == domain || host.endsWith(".$domain") }
        }
        if (hasInstitutionalDomain || hasKnownReference) {
            score += 15
            reasons += "Link points to a recognized education, research or public-information domain"
        } else {
            warnings += "No recognized institutional or research source URL detected"
        }

        val clickbait = clickbaitTerms.filter { all.contains(it) }
        if (clickbait.isNotEmpty()) {
            score -= 20
            warnings += "Possible clickbait wording: ${clickbait.take(3).joinToString()}"
        }

        val entertainment = entertainmentTerms.filter { all.contains(it) }
        if (entertainment.isNotEmpty()) {
            score -= 35
            warnings += "Entertainment/movie-clip wording detected"
        }

        if (channel.isBlank()) warnings += "Channel name is missing"
        if (description.length < 40) warnings += "Description is too short for meaningful source review"

        score = score.coerceIn(0, 100)
        val verdict = when {
            entertainment.isNotEmpty() -> "Likely entertainment — keep out of learning feed"
            score >= 75 -> "Promising educational signals — review sources"
            score >= 55 -> "Needs manual review"
            else -> "Insufficient evidence"
        }
        return AuthenticityReport(score, verdict, reasons, warnings)
    }
}
