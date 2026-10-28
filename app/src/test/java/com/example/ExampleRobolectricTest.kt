package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.moderation.ModerationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private val moderationEngine = ModerationEngine()
    private val blockedWords = listOf("mjinga", "punda", "matako", "umbwa")

    @Test
    fun `read string from context matches RealSatisfaction`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RealSatisfaction", appName)
    }

    @Test
    fun `moderation blocks prohibited Kiswahili words`() {
        val result = moderationEngine.evaluateContent(
            text = "Huyu mtu ni mjinga sana hafai kuja dukani",
            blockedWords = blockedWords
        )
        assertFalse(result.isApproved)
        assertEquals("PROHIBITED_WORD", result.violationType)
    }

    @Test
    fun `moderation approves positive clean Kisumu post`() {
        val result = moderationEngine.evaluateContent(
            text = "Kulinda Ziwa Victoria huanzia na usafi wetu wa ufukweni. Pamoja tunaweka Kisumu safi. #CleanKisumu #ZiwaVictoria",
            blockedWords = blockedWords
        )
        assertTrue(result.isApproved)
    }

    @Test
    fun `moderation blocks phone number leak`() {
        val result = moderationEngine.evaluateContent(
            text = "Wasiliana nami kwa nambari ya simu +254712345678 kwa maelezo zaidi ya biashara.",
            blockedWords = blockedWords
        )
        assertFalse(result.isApproved)
        assertEquals("PERSONAL_INFO", result.violationType)
    }

    @Test
    fun `moderation blocks excessive hashtags`() {
        val result = moderationEngine.evaluateContent(
            text = "Tembelea Dunga Beach Kisumu ufurahie upepo na samaki wabichi. #Kisumu #Tourism #Beach #Sunset",
            blockedWords = blockedWords
        )
        assertFalse(result.isApproved)
        assertEquals("EXCESSIVE_HASHTAGS", result.violationType)
    }
}
