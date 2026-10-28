package com.example.domain.ai

import com.example.data.local.AuditLogEntity
import com.example.data.local.ContentPillarEntity
import com.example.data.local.PageCommentEntity
import com.example.data.local.PostDraftEntity
import com.example.data.repository.AppRepository
import com.example.domain.facebook.FacebookPublishResult
import com.example.domain.moderation.ModerationEngine
import com.example.domain.moderation.ModerationResult
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale

data class CommandExecutionResult(
    val replyText: String,
    val statusLabel: String,
    val actionType: String? = null,
    val actionPayload: String? = null
)

class AiCommandEngine(
    private val repository: AppRepository,
    private val groqService: GroqApiService = GroqApiService(),
    private val moderationEngine: ModerationEngine = ModerationEngine()
) {

    suspend fun processCommand(instruction: String): CommandExecutionResult {
        val trimmed = instruction.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Health check command
        if (isHealthCheckCommand(lower)) {
            return executeHealthCheck()
        }

        // 2. Audit logs / actions command
        if (isAuditLogCommand(lower)) {
            return executeAuditLogQuery()
        }

        // 3. Pause / Resume automation commands
        if (isPauseCommand(lower)) {
            return executePauseAutomation()
        }
        if (isResumeCommand(lower)) {
            return executeResumeAutomation()
        }

        // 4. Change schedule command
        if (isScheduleChangeCommand(lower)) {
            return executeScheduleChange(lower)
        }

        // 5. Add / Edit content topic command
        if (isAddTopicCommand(lower)) {
            return executeAddTopic(trimmed)
        }

        // 6. Comments command: Show comments
        if (isShowCommentsCommand(lower)) {
            return executeShowComments()
        }

        // 7. Comments command: Draft response
        if (isDraftReplyCommand(lower)) {
            return executeDraftReply(trimmed)
        }

        // 8. Publish approved post command
        if (isPublishApprovedCommand(lower)) {
            return executePublishApprovedPost()
        }

        // 9. Post preparation / creation command
        if (isCreatePostCommand(lower)) {
            return executeCreatePost(trimmed, lower)
        }

        // If not a recognized direct command, use Groq AI (or smart fallback) to interpret instruction
        return executeGenericAiInstruction(trimmed)
    }

    private fun isHealthCheckCommand(cmd: String): Boolean {
        return cmd.contains("health check") || cmd.contains("kagua hali") ||
                cmd.contains("system check") || cmd.contains("ukaguzi wa mfumo")
    }

    private fun isAuditLogCommand(cmd: String): Boolean {
        return cmd.contains("last ten actions") || cmd.contains("vitendo kumi") ||
                cmd.contains("audit log") || cmd.contains("shughuli za hivi karibuni") ||
                cmd.contains("latest activity") || cmd.contains("matukio ya mwisho")
    }

    private fun isPauseCommand(cmd: String): Boolean {
        return cmd.contains("pause") || cmd.contains("sitisha") || cmd.contains("simamisha")
    }

    private fun isResumeCommand(cmd: String): Boolean {
        return cmd.contains("resume") || cmd.contains("endeleza") || cmd.contains("rejesha")
    }

    private fun isScheduleChangeCommand(cmd: String): Boolean {
        return (cmd.contains("schedule") || cmd.contains("ratiba")) &&
                (cmd.contains("hour") || cmd.contains("masaa") || cmd.contains("saa") || cmd.contains("every") || cmd.contains("kila"))
    }

    private fun isAddTopicCommand(cmd: String): Boolean {
        return (cmd.contains("add") || cmd.contains("ongeza") || cmd.contains("weka mada")) &&
                (cmd.contains("topic") || cmd.contains("mada") || cmd.contains("pillar") || cmd.contains("nguzo"))
    }

    private fun isShowCommentsCommand(cmd: String): Boolean {
        return (cmd.contains("comment") || cmd.contains("maoni")) &&
                (cmd.contains("show") || cmd.contains("nionyeshe") || cmd.contains("angalia") || cmd.contains("check") || cmd.contains("fetch") || cmd.contains("new"))
    }

    private fun isDraftReplyCommand(cmd: String): Boolean {
        return (cmd.contains("reply") || cmd.contains("jibu")) &&
                (cmd.contains("draft") || cmd.contains("andaa") || cmd.contains("andika"))
    }

    private fun isPublishApprovedCommand(cmd: String): Boolean {
        return (cmd.contains("publish") || cmd.contains("chapisha")) &&
                (cmd.contains("approved") || cmd.contains("iliyoidhinishwa") || cmd.contains("tayari") || cmd.contains("post"))
    }

    private fun isCreatePostCommand(cmd: String): Boolean {
        return (cmd.contains("create") || cmd.contains("prepare") || cmd.contains("make") || cmd.contains("andaa") || cmd.contains("tengeneza") || cmd.contains("chapisho") || cmd.contains("post")) &&
                (cmd.contains("post") || cmd.contains("chapisho") || cmd.contains("ujumbe") || cmd.contains("biashara") || cmd.contains("victoria") || cmd.contains("mazingira") || cmd.contains("kilimo") || cmd.contains("afya") || cmd.contains("elimu") || cmd.contains("utalii"))
    }

    private suspend fun executeHealthCheck(): CommandExecutionResult {
        val health = repository.performSystemHealthCheck()
        val summary = buildString {
            append("🩺 UKAGUZI WA MFUMO (HEALTH CHECK RESULT):\n")
            append("• Meta Facebook: ${if (health.facebookConnected) "IMEUNGANISHWA (${health.facebookPageName})" else "HAIJAUNGANISHWA - ${health.facebookDetails}"}\n")
            append("• Groq AI Engine: ${if (health.aiConnected) "IMEUNGANISHWA (${health.aiModel})" else "HAIJAUNGANISHWA - ${health.aiDetails}"}\n")
            append("• Hifadhi ya Data (Room): ${if (health.storageHealthy) "SAFI (${health.pendingDraftsCount} drafts, ${health.pendingCommentsCount} comments)" else "HITILAFU"}\n")
            append("• Ratiba (Scheduler): Kila masaa ${health.postingIntervalHours} | ${if (health.isPaused) "IMESITISHWA (PAUSED)" else "INAENDELEA (ACTIVE)"}\n")
            append("• Ulinzi wa Maudhui: Sheria ${health.moderationRulesCount} zinalinda mfumo.")
        }
        val status = if (health.facebookConnected && health.aiConnected) "READY" else "NEEDS REVIEW"
        return CommandExecutionResult(summary, status, "HEALTH_CHECK")
    }

    private suspend fun executeAuditLogQuery(): CommandExecutionResult {
        val logs = repository.getRecentAuditLogs(10)
        val text = if (logs.isEmpty()) {
            "Hakuna kumbukumbu za vitendo bado. Mfumo upo tayari kuanza kazi."
        } else {
            buildString {
                append("📋 VITENDO 10 VYA MWISHO VYA MFUMO:\n\n")
                logs.forEachIndexed { i, log ->
                    append("${i + 1}. [${log.eventType}] ${log.message}\n")
                }
            }
        }
        return CommandExecutionResult(text, "READY", "AUDIT_QUERY")
    }

    private suspend fun executePauseAutomation(): CommandExecutionResult {
        repository.setAutomationPaused(true)
        val msg = "Machapisho ya kiotomatiki yamesitishwa (Posting paused). Unaweza kurejesha wakati wowote kwa kusema 'Endeleza machapisho' au kupitia skrini ya Ratiba."
        return CommandExecutionResult(msg, "PAUSED", "PAUSE_AUTOMATION")
    }

    private suspend fun executeResumeAutomation(): CommandExecutionResult {
        repository.setAutomationPaused(false)
        val interval = repository.getPostingIntervalHours()
        val msg = "Machapisho ya kiotomatiki yamerejeshwa (Posting resumed). Ratiba inaendelea kila masaa $interval."
        return CommandExecutionResult(msg, "READY", "RESUME_AUTOMATION")
    }

    private suspend fun executeScheduleChange(cmd: String): BooleanMatchInterval {
        val hours = when {
            cmd.contains("six") || cmd.contains("6") || cmd.contains("sita") -> 6
            cmd.contains("twelve") || cmd.contains("12") || cmd.contains("kumi na mbili") -> 12
            cmd.contains("twenty-four") || cmd.contains("24") || cmd.contains("ishirini na nne") || cmd.contains("daily") || cmd.contains("kila siku") -> 24
            cmd.contains("one") || cmd.contains("1") || cmd.contains("moja") -> 1
            cmd.contains("three") || cmd.contains("3") || cmd.contains("tatu") -> 3
            else -> 3
        }
        repository.setPostingIntervalHours(hours)
        val msg = "Ratiba ya kuchapisha imebadilishwa kuwa kila masaa $hours (UTC / EAT UTC+3). Mipangilio imehifadhiwa salama."
        return CommandExecutionResult(msg, "READY", "SCHEDULE_CHANGE", hours.toString())
    }

    private suspend fun executeAddTopic(cmd: String): CommandExecutionResult {
        // Extract topic name
        val topicName = cmd.replace(Regex("(?i)(add|ongeza|weka|mada|topic|pillar|to our content topics|kwenye mada|kama nguzo ya maudhui)"), "")
            .trim(' ', '.', '"', '\'')

        if (topicName.isBlank()) {
            return CommandExecutionResult(
                "Tafadhali taja jina la mada unayotaka kuongeza. Mfano: 'Ongeza ajira ya vijana kwenye mada'",
                "NEEDS REVIEW"
            )
        }

        val newPillar = ContentPillarEntity(
            title = topicName.replaceFirstChar { it.uppercase() },
            description = "Community focused topic added by administrator for Kisumu audience.",
            priority = 7
        )
        repository.addContentPillar(newPillar)
        val msg = "Mada mpya imeongezwa: '$topicName'. Sasa mfumo utaitumia kuandaa machapisho ya kijamii."
        return CommandExecutionResult(msg, "READY", "ADD_TOPIC", topicName)
    }

    private suspend fun executeShowComments(): CommandExecutionResult {
        val commentsResult = repository.fetchAndStoreFacebookComments()
        if (!commentsResult.success) {
            // Check if we have cached comments in Room
            val cached = repository.pageCommentsFlow.firstOrNull() ?: emptyList()
            if (cached.isNotEmpty()) {
                val pending = cached.filter { it.replyStatus == "PENDING_REVIEW" }
                val summary = buildString {
                    append("Maoni yaliyohifadhiwa (${pending.size} yanayosubiri ukaguzi):\n\n")
                    pending.take(3).forEach { c ->
                        append("👤 ${c.authorName}: \"${c.message}\"\n")
                        append("🏷️ Jamii: ${c.category}\n")
                        if (!c.draftReply.isNullOrBlank()) {
                            append("✍️ Jibu lililoandaliwa: \"${c.draftReply}\"\n\n")
                        }
                    }
                }
                return CommandExecutionResult(
                    summary + "\n(Kumbuka: Uunganisho wa Facebook API: ${commentsResult.error})",
                    "NEEDS REVIEW",
                    "FETCH_COMMENTS"
                )
            }
            return CommandExecutionResult(
                "Imeshindwa kupakua maoni kutoka Meta Facebook: ${commentsResult.error}. Hakikisha FB_PAGE_ACCESS_TOKEN na FB_PAGE_ID zimeidhinishwa kwenye ukurasa wa Mipangilio.",
                "CONNECTION ERROR",
                "FETCH_COMMENTS"
            )
        }

        val count = commentsResult.comments.size
        val msg = "Maoni mapya $count yamepatikana kutoka kwenye Ukurasa wa Facebook na kuainishwa. Unaweza kuyaangalia na kuidhinisha majibu katika kichupo cha 'Maoni na Majibu'."
        return CommandExecutionResult(msg, "READY", "FETCH_COMMENTS")
    }

    private suspend fun executeDraftReply(cmd: String): CommandExecutionResult {
        val pending = repository.pageCommentsFlow.firstOrNull()?.filter { it.replyStatus == "PENDING_REVIEW" } ?: emptyList()
        if (pending.isEmpty()) {
            return CommandExecutionResult(
                "Hakuna maoni mapya yanayosubiri majibu kwa sasa.",
                "READY"
            )
        }
        val target = pending.first()
        val draftReply = generateReplyText(target.message, target.category)
        repository.updateCommentReply(target.id, draftReply, "PENDING_REVIEW")

        val response = buildString {
            append("✍️ Jibu limeandaliwa kwa maoni ya ${target.authorName}:\n")
            append("Maoni: \"${target.message}\"\n")
            append("Pendekezo la Jibu: \"$draftReply\"\n\n")
            append("Je, unaruhusu jibu hili litumwe kwenye Facebook?")
        }
        return CommandExecutionResult(response, "AWAITING APPROVAL", "DRAFT_REPLY", target.id)
    }

    private suspend fun executePublishApprovedPost(): CommandExecutionResult {
        val awaitingDrafts = repository.postDraftsFlow.firstOrNull()?.filter { it.status == "AWAITING APPROVAL" } ?: emptyList()
        if (awaitingDrafts.isEmpty()) {
            return CommandExecutionResult(
                "Hakuna chapisho lililoidhinishwa linalosubiri kuchapishwa (No post awaiting approval). Unaweza kuandaa chapisho kwanza kwa kusema 'Andaa chapisho kuhusu...'",
                "NEEDS REVIEW"
            )
        }

        val draft = awaitingDrafts.first()
        val publishResult: FacebookPublishResult = repository.publishDraftToFacebook(draft.id)
        if (publishResult.success) {
            val msg = "✅ Chapisho limechapishwa kwa ufanisi kwenye Ukurasa wa Facebook! (Post ID: ${publishResult.postId ?: "FB_OK"})\n\n\"${draft.content}\""
            return CommandExecutionResult(msg, "PUBLISHED", "PUBLISH_POST", draft.id.toString())
        } else {
            val msg = "❌ Imeshindwa kuchapisha kwenye Facebook: ${publishResult.error}\n\nTafadhali hakikisha ruhusa za Meta Graph API (pages_manage_posts) zimeidhinishwa."
            return CommandExecutionResult(msg, "ACTION FAILED", "PUBLISH_POST")
        }
    }

    private suspend fun executeCreatePost(instruction: String, lower: String): CommandExecutionResult {
        // Detect pillar and language
        val language = if (lower.contains("english") || lower.contains("kiingereza")) "English" else "Kiswahili"
        val pillar = detectPillar(lower)

        // Generate content via Groq or smart local generator
        val generated = generatePostText(pillar, language, instruction)

        // Run moderation pipeline
        val blockedWords = repository.getActiveBlockedWords()
        val recentPosts = repository.getRecentPublishedPostTexts()
        val moderationResult = moderationEngine.evaluateContent(generated.first, blockedWords, recentPosts)

        val status = if (moderationResult.isApproved) {
            if (lower.contains("show it to me") || lower.contains("nionyeshe") || lower.contains("before publishing") || lower.contains("kabla ya kuchapisha") || lower.contains("prepare") || lower.contains("andaa")) {
                "AWAITING APPROVAL"
            } else {
                "DRAFT READY"
            }
        } else {
            "MODERATION BLOCKED"
        }

        val draft = PostDraftEntity(
            pillarTitle = pillar,
            content = generated.first,
            language = language,
            hashtags = generated.second,
            status = status,
            moderationReason = moderationResult.reason
        )

        val draftId = repository.createPostDraft(draft)

        val reply = buildString {
            if (moderationResult.isApproved) {
                append("📝 Chapisho limeandaliwa na kufaulu ukaguzi wa usalama:\n\n")
                append("\"${draft.content}\"\n\n")
                append("• Nguzo: $pillar\n")
                append("• Lugha: $language\n")
                append("• Herufi: ${draft.content.length}/280\n")
                if (moderationResult.warnings.isNotEmpty()) {
                    append("⚠️ Tahadhari: ${moderationResult.warnings.joinToString("; ")}\n")
                }
                append("\nJe, ungependa nilichapishe sasa au ukague mabadiliko kwanza?")
            } else {
                append("🚫 Chapisho limezuiwa na kichujio cha usalama (Moderation Blocked):\n\n")
                append("\"${draft.content}\"\n\n")
                append("Sababu: ${moderationResult.reason}\n")
                append("Tafadhali rekebisha agizo lako uondoe lugha isiyoruhusiwa au viungo.")
            }
        }

        return CommandExecutionResult(reply, status, "DRAFT_POST", draftId.toString())
    }

    private suspend fun executeGenericAiInstruction(instruction: String): CommandExecutionResult {
        val apiKey = repository.getGroqApiKey()
        val model = repository.getGroqModel()

        if (apiKey.isNotBlank() && apiKey != "UNCONFIGURED") {
            val systemPrompt = """
                Wewe ni RealSatisfaction AI, msaidizi mwerevu wa usimamizi wa Ukurasa wa Facebook hapa Kisumu, Kenya.
                Unajibu kwa Kiswahili fasaha na Kiingereza cha heshima.
                Unasaidia msimamizi kuandaa machapisho ya Biashara Ndogo, Mazingira ya Ziwa Victoria, Afya, Elimu, Utalii wa Kisumu, na Kilimo.
                Huwezi kutoa takwimu za uongo au lugha ya matusi. Jibu kwa ufupi na unyenyekevu.
            """.trimIndent()

            val groqResult = groqService.generateCompletion(apiKey, model, systemPrompt, instruction)
            if (groqResult.success && !groqResult.content.isNullOrBlank()) {
                return CommandExecutionResult(groqResult.content, "READY")
            }
        }

        // Helpful response explaining system capabilities
        val text = buildString {
            append("Nimepokea agizo lako: \"$instruction\"\n\n")
            append("Kama msaidizi wa RealSatisfaction AI hapa Kisumu, ninaweza kufanya haya:\n")
            append("1. 'Tengeneza chapisho kuhusu biashara ndogo Kisumu'\n")
            append("2. 'Andaa chapisho kuhusu kulinda Ziwa Victoria na unionyeshe kabla ya kuchapisha'\n")
            append("3. 'Nionyeshe maoni mapya kwenye Ukurasa wa Facebook'\n")
            append("4. 'Chapisha chapisho lililoidhinishwa'\n")
            append("5. 'Sitisha machapisho ya kiotomatiki'\n")
            append("6. 'Badilisha ratiba iwe kila masaa sita'\n")
            append("7. 'Kagua hali ya mfumo (Health Check)'")
        }
        return CommandExecutionResult(text, "READY")
    }

    private fun detectPillar(lower: String): String {
        return when {
            lower.contains("victoria") || lower.contains("lake") || lower.contains("ziwa") || lower.contains("mazingira") || lower.contains("environment") || lower.contains("taka") ->
                "Mazingira na Ziwa Victoria"
            lower.contains("biashara") || lower.contains("business") || lower.contains("duka") || lower.contains("market") || lower.contains("uchumi") ->
                "Biashara Ndogo"
            lower.contains("afya") || lower.contains("health") || lower.contains("lishe") || lower.contains("nutrition") || lower.contains("wellness") ->
                "Afya na Lishe"
            lower.contains("elimu") || lower.contains("education") || lower.contains("vijana") || lower.contains("youth") || lower.contains("ajira") || lower.contains("kazi") ->
                "Elimu na Vijana"
            lower.contains("utalii") || lower.contains("tourism") || lower.contains("dunga") || lower.contains("sunset") || lower.contains("safari") ->
                "Utalii Kisumu"
            lower.contains("kilimo") || lower.contains("farming") || lower.contains("samaki") || lower.contains("fish") || lower.contains("agribusiness") ->
                "Kilimo Kisumu"
            else -> "Biashara Ndogo"
        }
    }

    private suspend fun generatePostText(pillar: String, language: String, userPrompt: String): Pair<String, String> {
        val apiKey = repository.getGroqApiKey()
        val model = repository.getGroqModel()

        if (apiKey.isNotBlank() && apiKey != "UNCONFIGURED") {
            val systemPrompt = """
                You are the social media copywriter for RealSatisfaction AI, managing a Facebook Page in Kisumu, Kenya.
                Rules:
                - Pillar: $pillar
                - Language: $language
                - Maximum 2 sentences.
                - Approximately 200-260 characters.
                - Uplifting, practical, respectful, truthful community advice for Kisumu.
                - At most 2 hashtags at the end.
                - Do not include markdown or quotation marks in the output.
            """.trimIndent()

            val groqResult = groqService.generateCompletion(apiKey, model, systemPrompt, userPrompt)
            if (groqResult.success && !groqResult.content.isNullOrBlank()) {
                val cleaned = groqResult.content.replace("\"", "").trim()
                return Pair(cleaned, extractHashtags(cleaned))
            }
        }

        // High-quality localized Kisumu templates for reliable generation
        return when (pillar) {
            "Mazingira na Ziwa Victoria" -> {
                if (language == "English") {
                    Pair("Protecting Lake Victoria starts with each of us keeping our shores clean and disposing of plastics responsibly. A cleaner lake means better livelihoods for Kisumu families. #CleanKisumu #LakeVictoria", "#CleanKisumu #LakeVictoria")
                } else {
                    Pair("Kulinda Ziwa Victoria huanzia na usafi wetu wa ufukweni na kupunguza matumizi ya plastiki. Ziwa safi huleta afya na uvuvi wenye mafanikio Kisumu. #MazingiraKisumu #ZiwaVictoria", "#MazingiraKisumu #ZiwaVictoria")
                }
            }
            "Biashara Ndogo" -> {
                if (language == "English") {
                    Pair("Consistency and attentive customer service are the cornerstones of growing any small enterprise in Kisumu. Keep your finances organized and value every client. #KisumuBusiness #Entrepreneurs", "#KisumuBusiness #Entrepreneurs")
                } else {
                    Pair("Huduma bora kwa wateja na utunzaji mzuri wa hesabu ndiyo nguzo kuu ya kukuza biashara ndogo hapa Kisumu. Thamani kila mteja anayeingia kibandani mwako. #BiasharaKisumu #KisumuUchumi", "#BiasharaKisumu #KisumuUchumi")
                }
            }
            "Afya na Lishe" -> {
                if (language == "English") {
                    Pair("Prioritizing balanced traditional foods and regular hydration strengthens your family's health throughout the season. Take care of your body today. #HealthyKisumu #Nutrition", "#HealthyKisumu #Nutrition")
                } else {
                    Pair("Ulaji wa vyakula asilia vilivyo na virutubisho na unywaji wa maji ya kutosha hudumisha afya dhabiti ya familia yako. Jali afya yako kila siku. #AfyaBora #LisheKisumu", "#AfyaBora #LisheKisumu")
                }
            }
            "Elimu na Vijana" -> {
                if (language == "English") {
                    Pair("Empowering Kisumu youth with practical digital skills and vocational training unlocks countless job opportunities. Never stop learning new abilities. #KisumuYouth #SkillsForLife", "#KisumuYouth #SkillsForLife")
                } else {
                    Pair("Kujifunza ujuzi wa vitendo na ufundi huwafungulia vijana wetu milango mipya ya ajira na biashara hapa Kisumu. Elimu na maarifa huleta maendeleo. #VijanaKisumu #AjiraNaUjuzi", "#VijanaKisumu #AjiraNaUjuzi")
                }
            }
            "Utalii Kisumu" -> {
                if (language == "English") {
                    Pair("From the calm sunsets at Dunga Beach to the rich heritage of Kisumu Museum, our city is full of warmth and wonders. Welcome visitors with pride. #VisitKisumu #LakeBreeze", "#VisitKisumu #LakeBreeze")
                } else {
                    Pair("Kuanzia machweo tulivu ya Dunga Beach hadi vivutio vya Impala Park, jiji letu la Kisumu limejaa uzuri wa kipekee. Tukaribishe wageni kwa bashasha. #UtaliiKisumu #ZiwaVictoria", "#UtaliiKisumu #ZiwaVictoria")
                }
            }
            "Kilimo Kisumu" -> {
                if (language == "English") {
                    Pair("Modern cage fish farming and drip irrigation are transforming agricultural yields across Kisumu County. Sustainable farming sustains our markets. #KisumuAgri #FoodSecurity", "#KisumuAgri #FoodSecurity")
                } else {
                    Pair("Ufugaji wa kisasa wa samaki kwenye vizimba na kilimo cha umwagiliaji huongeza mavuno na mapato kwa wakulima wa Kisumu. Kilimo ndiyo uti wa mgongo. #KilimoKisumu #UfugajiSamaki", "#KilimoKisumu #UfugajiSamaki")
                }
            }
            else -> {
                Pair("Tuungane kukuza uchumi na mshikamano wa jamii yetu hapa Kisumu kupitia kazi yenye nidhamu na ubunifu. Pamoja tunaendelea. #KisumuYetu #Pamoja", "#KisumuYetu #Pamoja")
            }
        }
    }

    private fun extractHashtags(text: String): String {
        val matches = Regex("#[A-Za-z0-9_]+").findAll(text).map { it.value }.toList()
        return matches.joinToString(" ")
    }

    private fun generateReplyText(comment: String, category: String): String {
        return when (category) {
            "Positive feedback" -> "Asante sana kwa maoni yako chanya na ushirikiano wako na ukurasa wetu wa Kisumu! Karibu tena."
            "General question" -> "Habari, asante kwa kuuliza. Tutaendelea kutoa ufafanuzi zaidi kupitia machapisho yetu ya kila siku. Karibu sana."
            "Complaint" -> "Pole sana kwa changamoto uliyokumbana nayo. Tumeipokea taarifa yako kwa heshima na msimamizi wetu ataikagua mara moja."
            "Business inquiry" -> "Asante kwa nia yako ya kibiashara. Tafadhali tutumie ujumbe wa faragha (Direct Message) ili msimamizi awasiliane nawe rasmi."
            else -> "Habari, asante kwa ujumbe wako. Tuko pamoja katika kuijenga jamii yetu ya Kisumu."
        }
    }
}

typealias BooleanMatchInterval = CommandExecutionResult
