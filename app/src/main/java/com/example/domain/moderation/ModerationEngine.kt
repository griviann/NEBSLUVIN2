package com.example.domain.moderation

import java.util.Locale
import java.util.regex.Pattern

data class ModerationResult(
    val isApproved: Boolean,
    val violationType: String? = null,
    val reason: String? = null,
    val warnings: List<String> = emptyList()
)

class ModerationEngine {

    // Regex for phone numbers (e.g. +254... or 07...)
    private val phoneRegex = Pattern.compile("(?:\\+?254|0)[17]\\d{8}")
    // Regex for emails
    private val emailRegex = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    // Regex for unapproved URLs
    private val urlRegex = Pattern.compile("https?://\\S+")

    fun evaluateContent(
        text: String,
        blockedWords: List<String>,
        recentPublishedPosts: List<String> = emptyList(),
        maxCharacters: Int = 280
    ): ModerationResult {
        val warnings = mutableListOf<String>()
        val trimmed = text.trim()

        if (trimmed.isEmpty()) {
            return ModerationResult(
                isApproved = false,
                violationType = "EMPTY_CONTENT",
                reason = "Yaliyomo hayawezi kuwa tupu / Content cannot be empty."
            )
        }

        // 1. Check length
        if (trimmed.length > maxCharacters + 40) { // small tolerance before hard block
            return ModerationResult(
                isApproved = false,
                violationType = "LENGTH_EXCEEDED",
                reason = "Chapisho linazidi kikomo cha herufi ($maxCharacters). Herufi za sasa: ${trimmed.length}."
            )
        } else if (trimmed.length > maxCharacters) {
            warnings.add("Urefu wa herufi (${trimmed.length}) unakaribia au unazidi lengo la $maxCharacters.")
        }

        // 2. Sentence count check (prefer <= 2 sentences)
        val sentences = trimmed.split(Regex("[.!?]+\\s+")).filter { it.isNotBlank() }
        if (sentences.size > 2) {
            warnings.add("Chapisho lina zaidi ya sentensi mbili (${sentences.size}). Pendekezo: sentensi zisizozidi mbili.")
        }

        // 3. Blocked words check (case-insensitive with word boundary)
        val lowerText = trimmed.lowercase(Locale.ROOT)
        for (blocked in blockedWords) {
            val cleanBlocked = blocked.trim().lowercase(Locale.ROOT)
            if (cleanBlocked.isEmpty()) continue

            // Word boundary regex: \bword\b or substring if short
            val regex = Regex("\\b${Pattern.quote(cleanBlocked)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(lowerText)) {
                return ModerationResult(
                    isApproved = false,
                    violationType = "PROHIBITED_WORD",
                    reason = "Imegunduliwa neno lililopigwa marufuku ('$cleanBlocked'). Tafadhali rekebisha kabla ya kuchapisha."
                )
            }

            // Check common leetspeak / vowel substitution variations (e.g., m j i n g a with spaces or symbols)
            val compactText = lowerText.replace(Regex("[^a-z0-9]"), "")
            if (compactText.contains(cleanBlocked) && cleanBlocked.length >= 4) {
                return ModerationResult(
                    isApproved = false,
                    violationType = "PROHIBITED_WORD_OBFUSCATION",
                    reason = "Majaribio ya kuficha neno lililopigwa marufuku ('$cleanBlocked')."
                )
            }
        }

        // 4. Hashtag count check (max 2 recommended)
        val hashtagCount = trimmed.count { it == '#' }
        if (hashtagCount > 2) {
            return ModerationResult(
                isApproved = false,
                violationType = "EXCESSIVE_HASHTAGS",
                reason = "Alama za reli (hashtags) zimezidi mbili (ziko $hashtagCount). Mwongozo unaruhusu zisizozidi mbili."
            )
        }

        // 5. Personal information check
        if (phoneRegex.matcher(trimmed).find()) {
            return ModerationResult(
                isApproved = false,
                violationType = "PERSONAL_INFO",
                reason = "Kuna nambari ya simu ya kibinafsi. Haturuhusu data ya faragha kwenye machapisho ya umma."
            )
        }
        if (emailRegex.matcher(trimmed).find()) {
            return ModerationResult(
                isApproved = false,
                violationType = "PERSONAL_INFO",
                reason = "Kuna barua pepe ya kibinafsi iliyogunduliwa."
            )
        }

        // 6. External unapproved links
        if (urlRegex.matcher(trimmed).find()) {
            return ModerationResult(
                isApproved = false,
                violationType = "UNAPPROVED_LINK",
                reason = "Viungo vya wavuti havijaidhinishwa moja kwa moja katika machapisho ya kijamii."
            )
        }

        // 7. Duplicate content check
        for (pastPost in recentPublishedPosts) {
            if (calculateSimilarity(trimmed, pastPost) > 0.85) {
                return ModerationResult(
                    isApproved = false,
                    violationType = "DUPLICATE_CONTENT",
                    reason = "Chapisho hili linafanana sana na chapisho lililokwishachapishwa hivi karibuni."
                )
            }
        }

        return ModerationResult(
            isApproved = true,
            warnings = warnings
        )
    }

    // Jaccard similarity between word sets
    private fun calculateSimilarity(str1: String, str2: String): Double {
        val words1 = str1.lowercase(Locale.ROOT).split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
        val words2 = str2.lowercase(Locale.ROOT).split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        return intersection.toDouble() / union.toDouble()
    }
}
