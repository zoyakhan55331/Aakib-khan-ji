package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.MahiLocalConversationEngine
import com.example.ai.MahiPersonality
import com.example.device.DeviceActionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Mahi AI", appName)
    }

    @Test
    fun `personality system prompt contains core identity and hinglish tone`() {
        val prompt = MahiPersonality.getSystemPrompt(userName = "Akib", sassLevel = 0.8f)
        assertTrue(prompt.contains("Mahi"))
        assertTrue(prompt.contains("Akib"))
        assertTrue(prompt.contains("Hinglish"))
        assertTrue(prompt.contains("open_app"))
        assertTrue(prompt.contains("set_timer"))
    }

    @Test
    fun `casual conversation query is not flagged as device action`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val deviceManager = DeviceActionManager(context)
        val engine = MahiLocalConversationEngine(deviceManager)

        val result = engine.processQuery("Hi Mahi, kya kar rahi ho?", sassLevel = 0.7f)
        assertFalse("Casual greetings must not be device actions", result.isDeviceAction)
        assertNotNull(result.spokenResponse)
        assertTrue(result.spokenResponse.isNotBlank())
    }

    @Test
    fun `device action detected for app opening`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val deviceManager = DeviceActionManager(context)
        val engine = MahiLocalConversationEngine(deviceManager)

        val result = engine.processQuery("YouTube kholo", sassLevel = 0.7f)
        assertTrue("YouTube kholo must trigger device action", result.isDeviceAction)
        assertNotNull(result.actionResult)
    }
}
