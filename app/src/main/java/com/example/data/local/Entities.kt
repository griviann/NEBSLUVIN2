package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String = "default",
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val statusLabel: String? = null, // READY, PROCESSING, DRAFT READY, etc.
    val actionType: String? = null, // DRAFT_POST, PUBLISH, FETCH_COMMENTS, REPLY_COMMENT, PAUSE, RESUME, HEALTH_CHECK, SCHEDULE_CHANGE
    val actionPayload: String? = null
)

@Entity(tableName = "post_drafts")
data class PostDraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pillarTitle: String,
    val content: String,
    val language: String = "Kiswahili", // "Kiswahili" or "English"
    val hashtags: String = "",
    val status: String = "DRAFT READY", // "DRAFT READY", "AWAITING APPROVAL", "PUBLISHED", "REJECTED", "MODERATION BLOCKED"
    val moderationReason: String? = null,
    val fbPostId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null
)

@Entity(tableName = "page_comments")
data class PageCommentEntity(
    @PrimaryKey val id: String, // FB Comment ID
    val postId: String,
    val authorName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "General question", // "General question", "Positive feedback", "Complaint", "Business inquiry", "Spam", "Sensitive issue", "Needs human attention"
    val draftReply: String? = null,
    val replyStatus: String = "PENDING_REVIEW", // "PENDING_REVIEW", "APPROVED", "REPLY_SENT", "SKIPPED"
    val repliedAt: Long? = null,
    val sentReplyText: String? = null
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String, // "START", "DRAFT_CREATED", "MODERATION_PASSED", "MODERATION_BLOCKED", "AWAITING_APPROVAL", "POSTED", "COMMENT_FETCHED", "REPLY_DRAFTED", "REPLY_SENT", "PAUSED", "ERROR"
    val message: String,
    val safeDetails: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "content_pillars")
data class ContentPillarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val enabled: Boolean = true,
    val priority: Int = 1
)

@Entity(tableName = "moderation_rules")
data class ModerationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockedWord: String,
    val isRegex: Boolean = false,
    val enabled: Boolean = true
)

@Entity(tableName = "config_settings")
data class ConfigSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
