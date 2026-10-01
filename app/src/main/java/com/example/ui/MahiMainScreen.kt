package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.MemoryRepository
import com.example.device.DeviceActionManager
import com.example.live.VoiceState
import com.example.live.VoiceStateManager
import com.example.service.MahiVoiceService
import com.example.ui.components.ActionBanner
import com.example.ui.components.AudioWaveform
import com.example.ui.components.CuteFloatingParticles
import com.example.ui.components.CuteMahiAvatar
import com.example.ui.components.CuteMicButton
import com.example.ui.components.FuturisticOrb
import com.example.ui.components.MemoryBottomSheet
import com.example.ui.components.QuickPromptsRow
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.components.TranscriptCard
import com.example.ui.theme.CuteCyan
import com.example.ui.theme.CuteGlassBorder
import com.example.ui.theme.CuteGlassSurface
import com.example.ui.theme.CutePink
import com.example.ui.theme.CutePinkSoft
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanLight
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahiMainScreen(
    voiceStateManager: VoiceStateManager,
    memoryRepo: MemoryRepository,
    deviceManager: DeviceActionManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val voiceState by voiceStateManager.voiceState.collectAsState()
    val isSessionActive by voiceStateManager.isSessionActive.collectAsState()
    val userTranscript by voiceStateManager.userTranscript.collectAsState()
    val mahiResponse by voiceStateManager.mahiResponse.collectAsState()
    val audioAmplitude by voiceStateManager.audioAmplitude.collectAsState()
    val actionBannerData by voiceStateManager.actionBanner.collectAsState()

    val memories by memoryRepo.getAllMemories().collectAsState(initial = emptyList())

    var currentTheme by remember { mutableStateOf(memoryRepo.selectedTheme) }

    // Bottom sheet states
    val memorySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMemorySheet by remember { mutableStateOf(false) }

    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSettingsSheet by remember { mutableStateOf(false) }

    var showTextInput by remember { mutableStateOf(false) }
    var textInputQuery by remember { mutableStateOf("") }

    // Permission tracking
    var hasRecordAudioPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var hasContactsPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasCallPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
    }
    var hasNotificationPerm by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasRecordAudioPerm = results[Manifest.permission.RECORD_AUDIO] ?: hasRecordAudioPerm
        hasContactsPerm = results[Manifest.permission.READ_CONTACTS] ?: hasContactsPerm
        hasCallPerm = results[Manifest.permission.CALL_PHONE] ?: hasCallPerm
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPerm = results[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPerm
        }

        if (hasRecordAudioPerm && !isSessionActive) {
            voiceStateManager.startSession()
        }
    }

    // Launch initial audio permission if not granted
    LaunchedEffect(Unit) {
        if (!hasRecordAudioPerm) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE
                )
            )
        }
    }

    // Manage Background Voice Service
    LaunchedEffect(memoryRepo.backgroundVoiceEnabled) {
        if (memoryRepo.backgroundVoiceEnabled) {
            MahiVoiceService.startService(context)
        } else {
            MahiVoiceService.stopService(context)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Layer
            if (currentTheme == "cute_neon") {
                // Cozy Anime Room Artwork Background
                Image(
                    painter = painterResource(id = R.drawable.mahi_theme_bg),
                    contentDescription = "Mahi AI Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark Glass Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xCC090814),
                                    Color(0xAA0C0A1B),
                                    Color(0xEE070611)
                                )
                            )
                        )
                )
                // Floating ambient hearts and neon particles
                CuteFloatingParticles(modifier = Modifier.fillMaxSize())
            } else {
                // Original / Dark Radial Glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    DarkSurfaceCard.copy(alpha = 0.5f),
                                    DarkBackground
                                ),
                                radius = 1200f
                            )
                        )
                )
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (currentTheme == "cute_neon") "Mahi AI ♡" else "Mahi AI",
                                color = if (currentTheme == "cute_neon") CutePink else TextPrimary,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            // Live Status Pill
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSessionActive) (if (currentTheme == "cute_neon") CuteCyan.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.15f)) else DarkSurfaceBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSessionActive) (if (currentTheme == "cute_neon") CuteCyan.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.5f)) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isSessionActive) "LIVE" else "STANDBY",
                                    color = if (isSessionActive) (if (currentTheme == "cute_neon") CuteCyan else NeonCyan) else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (currentTheme == "cute_neon") {
                            Text(
                                text = "Your Personal Voice Companion",
                                color = CutePinkSoft.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showMemorySheet = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (currentTheme == "cute_neon") CuteGlassSurface else DarkSurfaceCard)
                                .border(1.dp, if (currentTheme == "cute_neon") CuteGlassBorder else DarkSurfaceBorder, CircleShape)
                                .size(40.dp)
                                .testTag("memory_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Memory",
                                tint = if (currentTheme == "cute_neon") CutePink else NeonCyanLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (currentTheme == "cute_neon") CuteGlassSurface else DarkSurfaceCard)
                                .border(1.dp, if (currentTheme == "cute_neon") CuteGlassBorder else DarkSurfaceBorder, CircleShape)
                                .size(40.dp)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 2. Action Banner (Subtle, only appears for actual device actions)
                ActionBanner(
                    data = actionBannerData,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // 3. Central Character / Orb
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentTheme == "cute_neon") {
                        CuteMahiAvatar(
                            state = voiceState,
                            amplitude = audioAmplitude,
                            onClick = {
                                if (!hasRecordAudioPerm) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                } else {
                                    voiceStateManager.toggleSession()
                                }
                            }
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(DarkSurfaceCard.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                val (stateText, stateColor) = when (voiceState) {
                                    VoiceState.LISTENING -> "Listening to you..." to NeonCyan
                                    VoiceState.SPEAKING -> "Mahi is speaking" to NeonMagenta
                                    VoiceState.THINKING -> "Thinking..." to NeonAmber
                                    VoiceState.INTERRUPTED -> "Interrupted" to NeonAmber
                                    VoiceState.CONNECTING -> "Connecting session..." to NeonViolet
                                    VoiceState.ERROR -> "Connection issue" to Color(0xFFFF4081)
                                    else -> "Tap orb to speak" to TextSecondary
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(stateColor, CircleShape)
                                    )
                                    Text(
                                        text = stateText,
                                        color = stateColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            FuturisticOrb(
                                state = voiceState,
                                amplitude = audioAmplitude,
                                onClick = {
                                    if (!hasRecordAudioPerm) {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    } else {
                                        voiceStateManager.toggleSession()
                                    }
                                }
                            )
                        }
                    }
                }

                // 4. Audio Waveform
                AudioWaveform(
                    state = voiceState,
                    amplitude = audioAmplitude,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )

                // 5. Spoken Transcript Card (Mahi's subtitle + user speech)
                TranscriptCard(
                    userTranscript = userTranscript,
                    mahiResponse = mahiResponse,
                    state = voiceState,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // 6. Quick Prompts Row
                QuickPromptsRow(
                    onPromptSelected = { prompt ->
                        voiceStateManager.submitTextQuery(prompt)
                    },
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // 7. Text Input Bar (when expanded)
                AnimatedVisibility(
                    visible = showTextInput,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInputQuery,
                            onValueChange = { textInputQuery = it },
                            placeholder = { Text("Ask or tell Mahi anything...", color = TextMuted, fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_input_field"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (currentTheme == "cute_neon") CutePink else NeonCyan,
                                unfocusedBorderColor = if (currentTheme == "cute_neon") CuteGlassBorder else DarkSurfaceBorder,
                                focusedContainerColor = if (currentTheme == "cute_neon") CuteGlassSurface else DarkSurfaceCard,
                                unfocusedContainerColor = if (currentTheme == "cute_neon") CuteGlassSurface else DarkSurfaceCard,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textInputQuery.isNotBlank()) {
                                        voiceStateManager.submitTextQuery(textInputQuery)
                                        textInputQuery = ""
                                        showTextInput = false
                                    }
                                }
                            )
                        )
                        IconButton(
                            onClick = {
                                if (textInputQuery.isNotBlank()) {
                                    voiceStateManager.submitTextQuery(textInputQuery)
                                    textInputQuery = ""
                                    showTextInput = false
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (currentTheme == "cute_neon") CutePink else NeonCyan, CircleShape)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black)
                        }
                    }
                }

                // 8. Bottom Microphone & Action Controls
                if (currentTheme == "cute_neon") {
                    // Cute Theme Circular Voice Button with Glass Navigation Dock
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        CuteMicButton(
                            state = voiceState,
                            amplitude = audioAmplitude,
                            isSessionActive = isSessionActive,
                            onClick = {
                                if (!hasRecordAudioPerm) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                } else {
                                    voiceStateManager.toggleSession()
                                }
                            }
                        )

                        // Glass Navigation Dock matching artwork
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .background(CuteGlassSurface, RoundedCornerShape(24.dp))
                                .border(1.dp, CuteGlassBorder, RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Voice icon button
                                IconButton(
                                    onClick = {
                                        if (voiceState == VoiceState.SPEAKING) {
                                            voiceStateManager.interrupt()
                                        } else {
                                            voiceStateManager.toggleSession()
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (voiceState == VoiceState.SPEAKING) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = "Voice Control",
                                        tint = CutePink,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Settings icon button
                                IconButton(
                                    onClick = { showSettingsSheet = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = CuteCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Memory icon button
                                IconButton(
                                    onClick = { showMemorySheet = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = "Memory",
                                        tint = CutePinkSoft,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Keyboard text expander
                                IconButton(
                                    onClick = { showTextInput = !showTextInput },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showTextInput) Icons.Default.Close else Icons.Default.Keyboard,
                                        contentDescription = "Type",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Original Controls Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Keyboard toggle button
                        IconButton(
                            onClick = { showTextInput = !showTextInput },
                            modifier = Modifier
                                .size(50.dp)
                                .background(DarkSurfaceCard, CircleShape)
                                .border(1.dp, DarkSurfaceBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (showTextInput) Icons.Default.Close else Icons.Default.Keyboard,
                                contentDescription = "Type",
                                tint = if (showTextInput) NeonCyan else TextSecondary
                            )
                        }

                        // Large glowing Microphone Session Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = when (voiceState) {
                                        VoiceState.LISTENING -> Brush.linearGradient(listOf(NeonCyan, Color(0xFF0072FF)))
                                        VoiceState.SPEAKING -> Brush.linearGradient(listOf(NeonMagenta, NeonViolet))
                                        VoiceState.THINKING -> Brush.linearGradient(listOf(NeonAmber, NeonRose))
                                        else -> Brush.linearGradient(listOf(DarkSurfaceCard, DarkSurface))
                                    }
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isSessionActive) NeonCyan else DarkSurfaceBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (!hasRecordAudioPerm) {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    } else {
                                        voiceStateManager.toggleSession()
                                    }
                                }
                                .testTag("mic_toggle_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    !isSessionActive -> Icons.Default.MicOff
                                    voiceState == VoiceState.SPEAKING -> Icons.Default.Stop
                                    else -> Icons.Default.Mic
                                },
                                contentDescription = "Toggle Session",
                                tint = if (isSessionActive) Color.White else TextSecondary,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // Interrupt / Stop speech button
                        IconButton(
                            onClick = {
                                if (voiceState == VoiceState.SPEAKING) {
                                voiceStateManager.interrupt()
                            } else if (isSessionActive) {
                                voiceStateManager.stopSession()
                            } else {
                                voiceStateManager.startSession()
                            }
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .background(DarkSurfaceCard, CircleShape)
                            .border(1.dp, DarkSurfaceBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (voiceState == VoiceState.SPEAKING) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Interrupt",
                            tint = if (voiceState == VoiceState.SPEAKING) NeonRose else TextSecondary
                        )
                    }
                }
            }
        }
    }

    // Memory Bottom Sheet
    if (showMemorySheet) {
        MemoryBottomSheet(
            sheetState = memorySheetState,
            memories = memories,
            isMemoryEnabled = memoryRepo.isMemoryEnabled,
            onToggleMemoryEnabled = { memoryRepo.isMemoryEnabled = it },
            onAddMemory = { cat, k, v ->
                scope.launch { memoryRepo.saveMemory(cat, k, v) }
            },
            onDeleteMemory = { memory ->
                scope.launch { memoryRepo.deleteMemory(memory) }
            },
            onClearAllMemories = {
                scope.launch { memoryRepo.clearAllMemories() }
            },
            onDismiss = { showMemorySheet = false }
        )
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        SettingsBottomSheet(
            sheetState = settingsSheetState,
            memoryRepo = memoryRepo,
            hasRecordAudioPerm = hasRecordAudioPerm,
            hasContactsPerm = hasContactsPerm,
            hasCallPerm = hasCallPerm,
            hasNotificationPerm = hasNotificationPerm,
            onRequestPermission = { perm ->
                permissionLauncher.launch(arrayOf(perm))
            },
            onThemeChanged = { newTheme ->
                currentTheme = newTheme
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}
}

