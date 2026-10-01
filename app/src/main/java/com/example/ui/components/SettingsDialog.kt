package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemoryRepository
import com.example.ui.theme.CutePink
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    sheetState: SheetState,
    memoryRepo: MemoryRepository,
    hasRecordAudioPerm: Boolean,
    hasContactsPerm: Boolean,
    hasCallPerm: Boolean,
    hasNotificationPerm: Boolean,
    onRequestPermission: (String) -> Unit,
    onThemeChanged: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(memoryRepo.selectedTheme) }
    var sassLevel by remember { mutableFloatStateOf(memoryRepo.sassLevel) }
    var speechPitch by remember { mutableFloatStateOf(memoryRepo.speechPitch) }
    var speechRate by remember { mutableFloatStateOf(memoryRepo.speechRate) }
    var bgVoiceEnabled by remember { mutableStateOf(memoryRepo.backgroundVoiceEnabled) }
    var langPref by remember { mutableStateOf(memoryRepo.languagePreference) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Mahi AI Settings",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            // Visual Theme Selection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceCard, RoundedCornerShape(16.dp))
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Visual Theme",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themes = listOf(
                            Triple("cute_neon", "Cute Neon ♡", CutePink),
                            Triple("original", "Futuristic Orb", NeonCyan),
                            Triple("minimal_dark", "Dark", TextSecondary)
                        )
                        themes.forEach { (key, label, color) ->
                            val isSelected = selectedTheme == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) color.copy(alpha = 0.2f) else DarkSurface)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) color else DarkSurfaceBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedTheme = key
                                        memoryRepo.selectedTheme = key
                                        onThemeChanged(key)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Personality / Sass Slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceCard, RoundedCornerShape(16.dp))
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Personality & Sass Level",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = when {
                                sassLevel < 0.35f -> "Gentle & Sweet"
                                sassLevel < 0.75f -> "Witty & Sassy 😏"
                                else -> "Savage Mode 🔥"
                            },
                            color = NeonMagenta,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = sassLevel,
                        onValueChange = {
                            sassLevel = it
                            memoryRepo.sassLevel = it
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonMagenta,
                            activeTrackColor = NeonMagenta,
                            inactiveTrackColor = DarkSurfaceBorder
                        )
                    )
                    Text(
                        text = "Adjusts how playfully Mahi teases and banters with you in Hindi/Hinglish.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Voice Pitch & Rate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceCard, RoundedCornerShape(16.dp))
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Voice Tone & Speed",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Voice Pitch", color = TextSecondary, fontSize = 13.sp)
                        Text(String.format("%.2fx", speechPitch), color = NeonCyan, fontSize = 13.sp)
                    }
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            speechPitch = it
                            memoryRepo.speechPitch = it
                        },
                        valueRange = 0.8f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Speed", color = TextSecondary, fontSize = 13.sp)
                        Text(String.format("%.2fx", speechRate), color = NeonCyan, fontSize = 13.sp)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            memoryRepo.speechRate = it
                        },
                        valueRange = 0.8f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceBorder
                        )
                    )
                }
            }

            // Background Voice Assistant
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceCard, RoundedCornerShape(16.dp))
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Voice Service",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Maintains persistent voice assistant notification and ready status.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = bgVoiceEnabled,
                        onCheckedChange = {
                            bgVoiceEnabled = it
                            memoryRepo.backgroundVoiceEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceBorder
                        )
                    )
                }
            }

            // Android Permissions Checklist
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceCard, RoundedCornerShape(16.dp))
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Device Permissions",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )

                    PermissionItem(
                        icon = Icons.Default.Mic,
                        title = "Microphone (Voice)",
                        isGranted = hasRecordAudioPerm,
                        onClick = { onRequestPermission(android.Manifest.permission.RECORD_AUDIO) }
                    )

                    PermissionItem(
                        icon = Icons.Default.Contacts,
                        title = "Contacts (Search & Match)",
                        isGranted = hasContactsPerm,
                        onClick = { onRequestPermission(android.Manifest.permission.READ_CONTACTS) }
                    )

                    PermissionItem(
                        icon = Icons.Default.Call,
                        title = "Phone Calling",
                        isGranted = hasCallPerm,
                        onClick = { onRequestPermission(android.Manifest.permission.CALL_PHONE) }
                    )

                    PermissionItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        isGranted = hasNotificationPerm,
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                onRequestPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceBorder,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Android App Permissions")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isGranted) NeonEmerald else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                color = if (isGranted) TextPrimary else TextSecondary,
                fontSize = 13.sp
            )
        }
        if (isGranted) {
            Text(
                text = "✓ Granted",
                color = NeonEmerald,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.2f),
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Grant", fontSize = 11.sp)
            }
        }
    }
}
