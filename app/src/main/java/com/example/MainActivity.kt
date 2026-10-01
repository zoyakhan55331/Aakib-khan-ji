package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.data.MemoryRepository
import com.example.device.DeviceActionManager
import com.example.live.VoiceStateManager
import com.example.ui.MahiMainScreen
import com.example.ui.theme.MahiAITheme

class MainActivity : ComponentActivity() {

    private lateinit var memoryRepo: MemoryRepository
    private lateinit var deviceManager: DeviceActionManager
    private lateinit var voiceStateManager: VoiceStateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        memoryRepo = MemoryRepository(this)
        deviceManager = DeviceActionManager(this)
        voiceStateManager = VoiceStateManager(
            context = this,
            scope = lifecycleScope,
            memoryRepo = memoryRepo,
            deviceManager = deviceManager
        )

        setContent {
            MahiAITheme {
                MahiMainScreen(
                    voiceStateManager = voiceStateManager,
                    memoryRepo = memoryRepo,
                    deviceManager = deviceManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceStateManager.release()
    }
}
