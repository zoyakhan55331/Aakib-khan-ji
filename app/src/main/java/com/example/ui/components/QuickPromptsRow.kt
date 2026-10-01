package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary

@Composable
fun QuickPromptsRow(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        "Kaisi ho Mahi?",
        "YouTube kholo",
        "Kya kar rahi ho?",
        "Timer 5 min",
        "Bluetooth settings",
        "Ek joke sunao",
        "Call Mummy",
        "Mood kharab hai",
        "Chrome kholo"
    )

    LazyRow(
        modifier = modifier.testTag("quick_prompts_row"),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(prompts) { prompt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkSurfaceCard.copy(alpha = 0.7f))
                    .border(
                        width = 1.dp,
                        color = DarkSurfaceBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onPromptSelected(prompt) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = prompt,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
