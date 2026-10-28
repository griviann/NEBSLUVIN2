package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ChatMessageEntity::class,
        PostDraftEntity::class,
        PageCommentEntity::class,
        AuditLogEntity::class,
        ContentPillarEntity::class,
        ModerationRuleEntity::class,
        ConfigSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun postDraftDao(): PostDraftDao
    abstract fun pageCommentDao(): PageCommentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun contentPillarDao(): ContentPillarDao
    abstract fun moderationRuleDao(): ModerationRuleDao
    abstract fun configSettingDao(): ConfigSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "realsatisfaction_db"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default values on background thread
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            seedInitialData(database)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(db: AppDatabase) {
            // Seed 6 Content Pillars
            if (db.contentPillarDao().getCount() == 0) {
                db.contentPillarDao().insertPillars(
                    listOf(
                        ContentPillarEntity(
                            title = "Biashara Ndogo",
                            description = "Small businesses, entrepreneurship, customer service, marketing and financial management in Kisumu.",
                            priority = 1
                        ),
                        ContentPillarEntity(
                            title = "Mazingira na Ziwa Victoria",
                            description = "Environmental conservation, waste management, lake protection, beach cleanups and clean communities.",
                            priority = 2
                        ),
                        ContentPillarEntity(
                            title = "Afya na Lishe",
                            description = "General health awareness, nutrition education, preventative wellness and community wellbeing.",
                            priority = 3
                        ),
                        ContentPillarEntity(
                            title = "Elimu na Vijana",
                            description = "Education, practical skills development, learning opportunities and youth employment in Kisumu County.",
                            priority = 4
                        ),
                        ContentPillarEntity(
                            title = "Utalii Kisumu",
                            description = "Kisumu tourism, cultural heritage, Dunga Beach, Impala Sanctuary, sunsets and local hospitality.",
                            priority = 5
                        ),
                        ContentPillarEntity(
                            title = "Kilimo Kisumu",
                            description = "Modern farming, cage fish farming, sustainable crop production, agribusiness and agricultural markets.",
                            priority = 6
                        )
                    )
                )
            }

            // Seed initial blocked words: mjinga, punda, matako, umbwa
            if (db.moderationRuleDao().getCount() == 0) {
                db.moderationRuleDao().insertRules(
                    listOf(
                        ModerationRuleEntity(blockedWord = "mjinga"),
                        ModerationRuleEntity(blockedWord = "punda"),
                        ModerationRuleEntity(blockedWord = "matako"),
                        ModerationRuleEntity(blockedWord = "umbwa")
                    )
                )
            }

            // Seed default settings
            db.configSettingDao().setSetting(ConfigSettingEntity("AI_MODEL", "llama-3.3-70b-versatile"))
            db.configSettingDao().setSetting(ConfigSettingEntity("POSTING_INTERVAL_HOURS", "3"))
            db.configSettingDao().setSetting(ConfigSettingEntity("AUTO_POSTING_PAUSED", "false"))
            db.configSettingDao().setSetting(ConfigSettingEntity("AUTO_REPLY_ENABLED", "false"))
            db.configSettingDao().setSetting(ConfigSettingEntity("HUMAN_APPROVAL_MODE", "true"))
            db.configSettingDao().setSetting(ConfigSettingEntity("FACEBOOK_API_VERSION", "v20.0"))

            // Seed initial Welcome Chat Message
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    role = "assistant",
                    content = "Habari! Mimi ni RealSatisfaction AI, msaidizi wako wa usimamizi wa Ukurasa wa Facebook hapa Kisumu. Unaweza kunituma kuandaa machapisho kwa Kiswahili au Kiingereza, kuangalia maoni, kupanga ratiba, au kufanya ukaguzi wa mfumo. Nipo tayari!",
                    statusLabel = "READY"
                )
            )

            // Seed initial Audit Log
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    eventType = "START",
                    message = "RealSatisfaction AI system initialized in Kisumu, Kenya",
                    safeDetails = "Database created, 6 pillars active, moderation filters armed"
                )
            )
        }
    }
}
