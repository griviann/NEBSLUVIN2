package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface PostDraftDao {
    @Query("SELECT * FROM post_drafts ORDER BY createdAt DESC")
    fun getAllDrafts(): Flow<List<PostDraftEntity>>

    @Query("SELECT * FROM post_drafts WHERE id = :id")
    suspend fun getDraftById(id: Long): PostDraftEntity?

    @Query("SELECT * FROM post_drafts WHERE status = 'AWAITING APPROVAL' ORDER BY createdAt DESC")
    fun getAwaitingApprovalDrafts(): Flow<List<PostDraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: PostDraftEntity): Long

    @Update
    suspend fun updateDraft(draft: PostDraftEntity)

    @Query("DELETE FROM post_drafts WHERE id = :id")
    suspend fun deleteDraft(id: Long)

    @Query("SELECT * FROM post_drafts ORDER BY createdAt DESC LIMIT 10")
    suspend fun getRecentPostsList(): List<PostDraftEntity>
}

@Dao
interface PageCommentDao {
    @Query("SELECT * FROM page_comments ORDER BY timestamp DESC")
    fun getAllComments(): Flow<List<PageCommentEntity>>

    @Query("SELECT * FROM page_comments WHERE id = :id")
    suspend fun getCommentById(id: String): PageCommentEntity?

    @Query("SELECT * FROM page_comments WHERE replyStatus = 'PENDING_REVIEW' ORDER BY timestamp DESC")
    fun getPendingComments(): Flow<List<PageCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertComments(comments: List<PageCommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: PageCommentEntity)

    @Update
    suspend fun updateComment(comment: PageCommentEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}

@Dao
interface ContentPillarDao {
    @Query("SELECT * FROM content_pillars ORDER BY priority ASC")
    fun getAllPillars(): Flow<List<ContentPillarEntity>>

    @Query("SELECT * FROM content_pillars WHERE enabled = 1 ORDER BY priority ASC")
    suspend fun getEnabledPillarsSync(): List<ContentPillarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillars(pillars: List<ContentPillarEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillar(pillar: ContentPillarEntity): Long

    @Update
    suspend fun updatePillar(pillar: ContentPillarEntity)

    @Query("DELETE FROM content_pillars WHERE id = :id")
    suspend fun deletePillar(id: Long)

    @Query("SELECT COUNT(*) FROM content_pillars")
    suspend fun getCount(): Int
}

@Dao
interface ModerationRuleDao {
    @Query("SELECT * FROM moderation_rules")
    fun getAllRules(): Flow<List<ModerationRuleEntity>>

    @Query("SELECT * FROM moderation_rules WHERE enabled = 1")
    suspend fun getActiveRulesSync(): List<ModerationRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<ModerationRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ModerationRuleEntity): Long

    @Query("DELETE FROM moderation_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("SELECT COUNT(*) FROM moderation_rules")
    suspend fun getCount(): Int
}

@Dao
interface ConfigSettingDao {
    @Query("SELECT * FROM config_settings")
    fun getAllSettings(): Flow<List<ConfigSettingEntity>>

    @Query("SELECT value FROM config_settings WHERE `key` = :key")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: ConfigSettingEntity)
}
