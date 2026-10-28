package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConfigSettingEntity
import com.example.data.local.ContentPillarEntity
import com.example.data.local.ModerationRuleEntity
import com.example.data.local.PageCommentEntity
import com.example.data.local.PostDraftEntity
import com.example.domain.facebook.FacebookApiService
import com.example.domain.facebook.FacebookCommentsResult
import com.example.domain.facebook.FacebookPublishResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

data class SystemHealthReport(
    val facebookConnected: Boolean,
    val facebookPageName: String?,
    val facebookDetails: String,
    val aiConnected: Boolean,
    val aiModel: String,
    val aiDetails: String,
    val storageHealthy: Boolean,
    val pendingDraftsCount: Int,
    val pendingCommentsCount: Int,
    val postingIntervalHours: Int,
    val isPaused: Boolean,
    val moderationRulesCount: Int
)

class AppRepository(
    private val database: AppDatabase,
    private val facebookApi: FacebookApiService = FacebookApiService()
) {

    val chatMessagesFlow: Flow<List<ChatMessageEntity>> = database.chatDao().getAllMessages()
    val postDraftsFlow: Flow<List<PostDraftEntity>> = database.postDraftDao().getAllDrafts()
    val pageCommentsFlow: Flow<List<PageCommentEntity>> = database.pageCommentDao().getAllComments()
    val auditLogsFlow: Flow<List<AuditLogEntity>> = database.auditLogDao().getAllLogs()
    val contentPillarsFlow: Flow<List<ContentPillarEntity>> = database.contentPillarDao().getAllPillars()
    val moderationRulesFlow: Flow<List<ModerationRuleEntity>> = database.moderationRuleDao().getAllRules()
    val configSettingsFlow: Flow<List<ConfigSettingEntity>> = database.configSettingDao().getAllSettings()

    suspend fun insertChatMessage(message: ChatMessageEntity): Long {
        return database.chatDao().insertMessage(message)
    }

    suspend fun clearChatHistory() {
        database.chatDao().clearHistory()
        logAudit("START", "Historia ya mazungumzo imesafishwa na mtumiaji", "Chat history cleared")
    }

    suspend fun createPostDraft(draft: PostDraftEntity): Long {
        val id = database.postDraftDao().insertDraft(draft)
        val eventType = if (draft.status == "MODERATION BLOCKED") "MODERATION_BLOCKED" else "DRAFT_CREATED"
        logAudit(
            eventType = eventType,
            message = "Chapisho limeandaliwa: [${draft.pillarTitle}] '${draft.content.take(40)}...'",
            safeDetails = "Status: ${draft.status}, Lugha: ${draft.language}"
        )
        return id
    }

    suspend fun updateDraft(draft: PostDraftEntity) {
        database.postDraftDao().updateDraft(draft)
        logAudit("DRAFT_CREATED", "Chapisho limebadilishwa: #${draft.id}", "Status: ${draft.status}")
    }

    suspend fun approveDraft(draftId: Long) {
        val draft = database.postDraftDao().getDraftById(draftId) ?: return
        val updated = draft.copy(status = "AWAITING APPROVAL")
        database.postDraftDao().updateDraft(updated)
        logAudit("AWAITING_APPROVAL", "Chapisho #${draft.id} limeidhinishwa na msimamizi", "Tayari kuchapishwa")
    }

    suspend fun rejectDraft(draftId: Long) {
        val draft = database.postDraftDao().getDraftById(draftId) ?: return
        val updated = draft.copy(status = "REJECTED")
        database.postDraftDao().updateDraft(updated)
        logAudit("DRAFT_CREATED", "Chapisho #${draft.id} limekataliwa na msimamizi", "Status: REJECTED")
    }

    suspend fun deleteDraft(draftId: Long) {
        database.postDraftDao().deleteDraft(draftId)
        logAudit("DRAFT_CREATED", "Chapisho #${draftId} limefutwa", "Deleted from draft queue")
    }

    suspend fun publishDraftToFacebook(draftId: Long): FacebookPublishResult {
        val draft = database.postDraftDao().getDraftById(draftId)
            ?: return FacebookPublishResult(false, error = "Chapisho halikupatikana")

        val pageId = getFacebookPageId()
        val token = getFacebookPageAccessToken()
        val version = getFacebookApiVersion()

        val result = facebookApi.publishPost(pageId, token, draft.content, version)
        if (result.success) {
            val updated = draft.copy(
                status = "PUBLISHED",
                fbPostId = result.postId,
                publishedAt = System.currentTimeMillis()
            )
            database.postDraftDao().updateDraft(updated)
            logAudit("POSTED", "Chapisho limechapishwa kwenye Facebook Page!", "FB Post ID: ${result.postId}")
        } else {
            logAudit("ERROR", "Uchapishaji wa Facebook umefeli kwa chapisho #${draft.id}", "Sababu: ${result.error}")
        }
        return result
    }

    suspend fun fetchAndStoreFacebookComments(): FacebookCommentsResult {
        val pageId = getFacebookPageId()
        val token = getFacebookPageAccessToken()
        val version = getFacebookApiVersion()

        val result = facebookApi.fetchRecentComments(pageId, token, version)
        if (result.success) {
            val entities = result.comments.map { c ->
                PageCommentEntity(
                    id = c.commentId,
                    postId = c.postId,
                    authorName = c.fromName,
                    message = c.message,
                    category = classifyComment(c.message),
                    replyStatus = "PENDING_REVIEW"
                )
            }
            database.pageCommentDao().insertComments(entities)
            logAudit("COMMENT_FETCHED", "Maoni ${result.comments.size} yamepakuliwa kutoka Facebook", "Saved to local database")
        } else {
            // If live credentials are missing, check if test data is needed
            if (pageId.isBlank() || pageId == "UNCONFIGURED") {
                seedSampleCommentsIfEmpty()
            }
            logAudit("ERROR", "Kushindwa kupakua maoni ya Facebook", result.error)
        }
        return result
    }

    suspend fun updateCommentReply(commentId: String, replyText: String, status: String) {
        val comment = database.pageCommentDao().getCommentById(commentId) ?: return
        val updated = comment.copy(draftReply = replyText, replyStatus = status)
        database.pageCommentDao().updateComment(updated)
        logAudit("REPLY_DRAFTED", "Jibu limeandaliwa kwa maoni ya ${comment.authorName}", "Status: $status")
    }

    suspend fun sendApprovedCommentReply(commentId: String): FacebookPublishResult {
        val comment = database.pageCommentDao().getCommentById(commentId)
            ?: return FacebookPublishResult(false, error = "Maoni hayakupatikana")

        val replyText = comment.draftReply ?: return FacebookPublishResult(false, error = "Hakuna jibu lililoandaliwa")

        val token = getFacebookPageAccessToken()
        val version = getFacebookApiVersion()

        val result = facebookApi.replyToComment(commentId, token, replyText, version)
        if (result.success) {
            val updated = comment.copy(
                replyStatus = "REPLY_SENT",
                repliedAt = System.currentTimeMillis(),
                sentReplyText = replyText
            )
            database.pageCommentDao().updateComment(updated)
            logAudit("REPLY_SENT", "Jibu limetumwa kwa ${comment.authorName} kwenye Facebook", "Comment ID: $commentId")
        } else {
            logAudit("ERROR", "Kushindwa kutuma jibu kwa maoni $commentId", result.error)
        }
        return result
    }

    suspend fun performSystemHealthCheck(): SystemHealthReport {
        // 1. Check Facebook
        val pageId = getFacebookPageId()
        val token = getFacebookPageAccessToken()
        val version = getFacebookApiVersion()
        val fbInfo = facebookApi.verifyPageConnection(pageId, token, version)

        // 2. Check AI
        val groqKey = getGroqApiKey()
        val model = getGroqModel()
        val aiConfigured = groqKey.isNotBlank() && groqKey != "UNCONFIGURED"

        // 3. Check Storage & counts
        val drafts = database.postDraftDao().getAllDrafts().firstOrNull() ?: emptyList()
        val comments = database.pageCommentDao().getAllComments().firstOrNull() ?: emptyList()
        val rulesCount = database.moderationRuleDao().getActiveRulesSync().size

        val isPaused = getSetting("AUTO_POSTING_PAUSED") == "true"
        val interval = getPostingIntervalHours()

        val report = SystemHealthReport(
            facebookConnected = fbInfo.isConnected,
            facebookPageName = fbInfo.pageName,
            facebookDetails = fbInfo.errorMessage ?: "Imeunganishwa na ${fbInfo.permissions.joinToString(", ")}",
            aiConnected = aiConfigured,
            aiModel = model,
            aiDetails = if (aiConfigured) "Groq API imesanidiwa na model $model" else "GROQ_API_KEY haijawekwa kwenye Settings au .env",
            storageHealthy = true,
            pendingDraftsCount = drafts.count { it.status == "AWAITING APPROVAL" },
            pendingCommentsCount = comments.count { it.replyStatus == "PENDING_REVIEW" },
            postingIntervalHours = interval,
            isPaused = isPaused,
            moderationRulesCount = rulesCount
        )

        logAudit("START", "Ukaguzi wa mfumo umekamilika (Health Check)", "FB: ${report.facebookConnected}, AI: ${report.aiConnected}")
        return report
    }

    suspend fun logAudit(eventType: String, message: String, safeDetails: String? = null) {
        val entity = AuditLogEntity(
            eventType = eventType,
            message = message,
            safeDetails = safeDetails
        )
        database.auditLogDao().insertLog(entity)
    }

    suspend fun getRecentAuditLogs(limit: Int): List<AuditLogEntity> {
        return database.auditLogDao().getRecentLogs(limit)
    }

    suspend fun clearAuditLogs() {
        database.auditLogDao().clearLogs()
    }

    suspend fun getActiveBlockedWords(): List<String> {
        return database.moderationRuleDao().getActiveRulesSync().map { it.blockedWord }
    }

    suspend fun addBlockedWord(word: String) {
        database.moderationRuleDao().insertRule(ModerationRuleEntity(blockedWord = word.trim().lowercase()))
        logAudit("START", "Neno lililopigwa marufuku limeongezwa: $word", "Moderation rule added")
    }

    suspend fun deleteBlockedWord(id: Long) {
        database.moderationRuleDao().deleteRule(id)
        logAudit("START", "Sheria ya kuzuia maneno imefutwa (#$id)", "Moderation rule deleted")
    }

    suspend fun addContentPillar(pillar: ContentPillarEntity) {
        database.contentPillarDao().insertPillar(pillar)
        logAudit("START", "Nguzo mpya ya maudhui imeongezwa: ${pillar.title}", "Pillar added")
    }

    suspend fun updateContentPillar(pillar: ContentPillarEntity) {
        database.contentPillarDao().updatePillar(pillar)
    }

    suspend fun deleteContentPillar(id: Long) {
        database.contentPillarDao().deletePillar(id)
    }

    suspend fun setSetting(key: String, value: String) {
        database.configSettingDao().setSetting(ConfigSettingEntity(key, value))
    }

    suspend fun getSetting(key: String): String? {
        return database.configSettingDao().getSettingValue(key)
    }

    suspend fun getGroqApiKey(): String {
        val userSetting = getSetting("GROQ_API_KEY")
        if (!userSetting.isNullOrBlank() && userSetting != "UNCONFIGURED") return userSetting
        return BuildConfig.GROQ_API_KEY
    }

    suspend fun getGroqModel(): String {
        return getSetting("AI_MODEL") ?: "llama-3.3-70b-versatile"
    }

    suspend fun getFacebookPageAccessToken(): String {
        val userSetting = getSetting("FB_PAGE_ACCESS_TOKEN")
        if (!userSetting.isNullOrBlank() && userSetting != "UNCONFIGURED") return userSetting
        return BuildConfig.FB_PAGE_ACCESS_TOKEN
    }

    suspend fun getFacebookPageId(): String {
        val userSetting = getSetting("FB_PAGE_ID")
        if (!userSetting.isNullOrBlank() && userSetting != "UNCONFIGURED") return userSetting
        return BuildConfig.FB_PAGE_ID
    }

    suspend fun getFacebookApiVersion(): String {
        return getSetting("FACEBOOK_API_VERSION") ?: BuildConfig.FACEBOOK_API_VERSION
    }

    suspend fun getPostingIntervalHours(): Int {
        return getSetting("POSTING_INTERVAL_HOURS")?.toIntOrNull() ?: 3
    }

    suspend fun setPostingIntervalHours(hours: Int) {
        setSetting("POSTING_INTERVAL_HOURS", hours.toString())
        logAudit("START", "Muda wa ratiba ya kuchapisha umewekwa kuwa kila masaa $hours", "Posting interval changed")
    }

    suspend fun setAutomationPaused(paused: Boolean) {
        setSetting("AUTO_POSTING_PAUSED", paused.toString())
        logAudit(
            eventType = if (paused) "PAUSED" else "START",
            message = if (paused) "Machapisho ya kiotomatiki yamesitishwa" else "Machapisho ya kiotomatiki yamerejeshwa",
            safeDetails = "Scheduler state changed by administrator"
        )
    }

    suspend fun getRecentPublishedPostTexts(): List<String> {
        return database.postDraftDao().getRecentPostsList().map { it.content }
    }

    private fun classifyComment(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("bei") || lower.contains("how much") || lower.contains("wapi") || lower.contains("where") || lower.contains("gharama") ->
                "Business inquiry"
            lower.contains("tatizo") || lower.contains("mbaya") || lower.contains("ubovu") || lower.contains("huduma duni") || lower.contains("complaint") ->
                "Complaint"
            lower.contains("?") || lower.contains("je") || lower.contains("kwa nini") || lower.contains("lini") ->
                "General question"
            lower.contains("safi") || lower.contains("vizuri") || lower.contains("pongezi") || lower.contains("asante") || lower.contains("hongera") ->
                "Positive feedback"
            lower.contains("http") || lower.contains("bit.ly") || lower.contains("crypto") || lower.contains("kamari") ->
                "Spam"
            lower.contains("dawa") || lower.contains("ugonjwa") || lower.contains("mahakama") || lower.contains("polisi") ->
                "Needs human attention"
            else -> "General question"
        }
    }

    private suspend fun seedSampleCommentsIfEmpty() {
        val current = database.pageCommentDao().getAllComments().firstOrNull() ?: emptyList()
        if (current.isEmpty()) {
            val sampleComments = listOf(
                PageCommentEntity(
                    id = "sample_c1",
                    postId = "post_101",
                    authorName = "Otieno Brian",
                    message = "Hongera sana kwa mawaidha mazuri kuhusu utunzaji wa mazingira ya Ziwa Victoria!",
                    category = "Positive feedback",
                    draftReply = "Asante sana Brian kwa kuunga mkono juhudi zetu za kuweka Ziwa Victoria safi!",
                    replyStatus = "PENDING_REVIEW"
                ),
                PageCommentEntity(
                    id = "sample_c2",
                    postId = "post_102",
                    authorName = "Achieng Maureen",
                    message = "Je, mafunzo ya kilimo cha samaki kwenye vizimba yanafanyika wapi hapa Kisumu?",
                    category = "General question",
                    draftReply = "Habari Maureen! Mafunzo hufanyika vituo mbalimbali vya ufukweni ikiwemo Dunga na ushirika wa wavuvi.",
                    replyStatus = "PENDING_REVIEW"
                ),
                PageCommentEntity(
                    id = "sample_c3",
                    postId = "post_103",
                    authorName = "Kariuki Peter",
                    message = "Nahitaji ushauri wa kuanzisha biashara ndogo ya kuuza samaki Kisumu, bei ya kuanzia ni ipi?",
                    category = "Business inquiry",
                    draftReply = "Habari Peter, karibu sana! Unaweza kuanza na mtaji mdogo kutegemea eneo lako.",
                    replyStatus = "PENDING_REVIEW"
                )
            )
            database.pageCommentDao().insertComments(sampleComments)
        }
    }
}
