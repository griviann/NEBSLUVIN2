package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConfigSettingEntity
import com.example.data.local.ContentPillarEntity
import com.example.data.local.ModerationRuleEntity
import com.example.data.local.PageCommentEntity
import com.example.data.local.PostDraftEntity
import com.example.data.repository.AppRepository
import com.example.data.repository.SystemHealthReport
import com.example.domain.ai.AiCommandEngine
import com.example.domain.facebook.FacebookApiService
import com.example.domain.facebook.FacebookPageInfo
import com.example.domain.facebook.FacebookPublishResult
import com.example.domain.moderation.ModerationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val facebookApi = FacebookApiService()
    private val repository = AppRepository(database, facebookApi)
    private val aiCommandEngine = AiCommandEngine(repository)
    private val moderationEngine = ModerationEngine()

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val postDrafts: StateFlow<List<PostDraftEntity>> = repository.postDraftsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pageComments: StateFlow<List<PageCommentEntity>> = repository.pageCommentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contentPillars: StateFlow<List<ContentPillarEntity>> = repository.contentPillarsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moderationRules: StateFlow<List<ModerationRuleEntity>> = repository.moderationRulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val configSettings: StateFlow<List<ConfigSettingEntity>> = repository.configSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _healthReport = MutableStateFlow<SystemHealthReport?>(null)
    val healthReport: StateFlow<SystemHealthReport?> = _healthReport.asStateFlow()

    private val _facebookPageInfo = MutableStateFlow<FacebookPageInfo?>(null)
    val facebookPageInfo: StateFlow<FacebookPageInfo?> = _facebookPageInfo.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        viewModelScope.launch {
            AppDatabase.seedInitialData(database)
            refreshHealthCheck()
            checkFacebookConnection()
        }
    }

    fun clearFeedback() {
        _userFeedbackMessage.value = null
    }

    fun sendUserInstruction(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            // 1. Add user message
            repository.insertChatMessage(
                ChatMessageEntity(
                    role = "user",
                    content = trimmed,
                    statusLabel = "PROCESSING"
                )
            )

            _isAiProcessing.value = true

            try {
                // 2. Process command via AI command engine
                val result = aiCommandEngine.processCommand(trimmed)

                // 3. Add assistant response
                repository.insertChatMessage(
                    ChatMessageEntity(
                        role = "assistant",
                        content = result.replyText,
                        statusLabel = result.statusLabel,
                        actionType = result.actionType,
                        actionPayload = result.actionPayload
                    )
                )
            } catch (e: Exception) {
                repository.insertChatMessage(
                    ChatMessageEntity(
                        role = "assistant",
                        content = "Hitilafu imetokea wakati wa kuchakata agizo: ${e.localizedMessage}. Mfumo uko salama.",
                        statusLabel = "ACTION FAILED"
                    )
                )
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            repository.clearChatHistory()
            repository.insertChatMessage(
                ChatMessageEntity(
                    role = "assistant",
                    content = "Mazungumzo mapya yameanzishwa. Habari! Mimi ni RealSatisfaction AI, msaidizi wako wa Ukurasa wa Facebook hapa Kisumu. Nipo tayari kwa agizo lako jipya.",
                    statusLabel = "READY"
                )
            )
        }
    }

    fun approveDraft(draftId: Long) {
        viewModelScope.launch {
            repository.approveDraft(draftId)
            _userFeedbackMessage.value = "Chapisho limeidhinishwa na liko tayari kuchapishwa."
        }
    }

    fun rejectDraft(draftId: Long) {
        viewModelScope.launch {
            repository.rejectDraft(draftId)
            _userFeedbackMessage.value = "Chapisho limekataliwa."
        }
    }

    fun deleteDraft(draftId: Long) {
        viewModelScope.launch {
            repository.deleteDraft(draftId)
            _userFeedbackMessage.value = "Chapisho limefutwa kwenye orodha."
        }
    }

    fun publishDraft(draftId: Long) {
        viewModelScope.launch {
            val res: FacebookPublishResult = repository.publishDraftToFacebook(draftId)
            if (res.success) {
                _userFeedbackMessage.value = "✅ Chapisho limechapishwa kwenye Facebook! (ID: ${res.postId ?: "FB_OK"})"
            } else {
                _userFeedbackMessage.value = "❌ Hitilafu ya kuchapisha: ${res.error}"
            }
            refreshHealthCheck()
        }
    }

    fun createManualDraft(pillar: String, content: String, language: String) {
        viewModelScope.launch {
            val blockedWords = repository.getActiveBlockedWords()
            val recentPosts = repository.getRecentPublishedPostTexts()
            val mod = moderationEngine.evaluateContent(content, blockedWords, recentPosts)

            val status = if (mod.isApproved) "AWAITING APPROVAL" else "MODERATION BLOCKED"
            val draft = PostDraftEntity(
                pillarTitle = pillar,
                content = content,
                language = language,
                status = status,
                moderationReason = mod.reason
            )
            repository.createPostDraft(draft)
            if (mod.isApproved) {
                _userFeedbackMessage.value = "Chapisho limeundwa na linasubiri idhini."
            } else {
                _userFeedbackMessage.value = "🚫 Chapisho limezuiwa: ${mod.reason}"
            }
        }
    }

    fun editDraft(draftId: Long, newContent: String) {
        viewModelScope.launch {
            val blockedWords = repository.getActiveBlockedWords()
            val recentPosts = repository.getRecentPublishedPostTexts()
            val mod = moderationEngine.evaluateContent(newContent, blockedWords, recentPosts)

            val draft = postDrafts.value.firstOrNull { it.id == draftId } ?: return@launch
            val updated = draft.copy(
                content = newContent,
                status = if (mod.isApproved) "DRAFT READY" else "MODERATION BLOCKED",
                moderationReason = mod.reason
            )
            repository.updateDraft(updated)
            _userFeedbackMessage.value = if (mod.isApproved) "Chapisho limerekebishwa kikamilifu." else "Tahadhari: ${mod.reason}"
        }
    }

    fun fetchComments() {
        viewModelScope.launch {
            val res = repository.fetchAndStoreFacebookComments()
            if (res.success) {
                _userFeedbackMessage.value = "Maoni ${res.comments.size} mapya yamepakuliwa."
            } else {
                _userFeedbackMessage.value = "Hitilafu ya kupakua maoni: ${res.error}"
            }
        }
    }

    fun updateCommentDraftReply(commentId: String, reply: String) {
        viewModelScope.launch {
            repository.updateCommentReply(commentId, reply, "PENDING_REVIEW")
            _userFeedbackMessage.value = "Jibu limehifadhiwa."
        }
    }

    fun sendCommentReply(commentId: String) {
        viewModelScope.launch {
            val res = repository.sendApprovedCommentReply(commentId)
            if (res.success) {
                _userFeedbackMessage.value = "✅ Jibu limetumwa kwa ufanisi kwenye Facebook!"
            } else {
                _userFeedbackMessage.value = "❌ Hitilafu ya kutuma jibu: ${res.error}"
            }
        }
    }

    fun toggleAutomationPaused() {
        viewModelScope.launch {
            val current = repository.getSetting("AUTO_POSTING_PAUSED") == "true"
            repository.setAutomationPaused(!current)
            refreshHealthCheck()
        }
    }

    fun updatePostingInterval(hours: Int) {
        viewModelScope.launch {
            repository.setPostingIntervalHours(hours)
            refreshHealthCheck()
            _userFeedbackMessage.value = "Muda wa ratiba umewekwa: kila masaa $hours."
        }
    }

    fun refreshHealthCheck() {
        viewModelScope.launch {
            _healthReport.value = repository.performSystemHealthCheck()
        }
    }

    fun checkFacebookConnection() {
        viewModelScope.launch {
            val pageId = repository.getFacebookPageId()
            val token = repository.getFacebookPageAccessToken()
            val version = repository.getFacebookApiVersion()
            _facebookPageInfo.value = facebookApi.verifyPageConnection(pageId, token, version)
        }
    }

    fun saveFacebookCredentials(pageId: String, token: String, version: String) {
        viewModelScope.launch {
            repository.setSetting("FB_PAGE_ID", pageId.trim())
            repository.setSetting("FB_PAGE_ACCESS_TOKEN", token.trim())
            repository.setSetting("FACEBOOK_API_VERSION", version.trim())
            _userFeedbackMessage.value = "Taarifa za Facebook zimehifadhiwa salama."
            checkFacebookConnection()
            refreshHealthCheck()
        }
    }

    fun saveGroqCredentials(apiKey: String, model: String) {
        viewModelScope.launch {
            repository.setSetting("GROQ_API_KEY", apiKey.trim())
            repository.setSetting("AI_MODEL", model.trim())
            _userFeedbackMessage.value = "Mipangilio ya Groq AI imehifadhiwa."
            refreshHealthCheck()
        }
    }

    fun addBlockedWord(word: String) {
        if (word.isBlank()) return
        viewModelScope.launch {
            repository.addBlockedWord(word)
            _userFeedbackMessage.value = "Neno '$word' limeongezwa kwenye orodha ya marufuku."
        }
    }

    fun deleteBlockedWord(id: Long) {
        viewModelScope.launch {
            repository.deleteBlockedWord(id)
            _userFeedbackMessage.value = "Sheria imefutwa."
        }
    }

    fun addContentPillar(title: String, desc: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val entity = ContentPillarEntity(
                title = title.trim(),
                description = desc.trim(),
                priority = contentPillars.value.size + 1
            )
            repository.addContentPillar(entity)
            _userFeedbackMessage.value = "Nguzo '$title' imeongezwa."
        }
    }

    fun toggleContentPillar(pillar: ContentPillarEntity) {
        viewModelScope.launch {
            repository.updateContentPillar(pillar.copy(enabled = !pillar.enabled))
        }
    }

    fun deleteContentPillar(id: Long) {
        viewModelScope.launch {
            repository.deleteContentPillar(id)
            _userFeedbackMessage.value = "Nguzo imefutwa."
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearAuditLogs()
            _userFeedbackMessage.value = "Kumbukumbu za matukio zimesafishwa."
        }
    }
}
