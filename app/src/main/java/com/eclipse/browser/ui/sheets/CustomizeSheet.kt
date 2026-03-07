package com.eclipse.browser.ui.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeSheet(
    accentColor: Color,
    accentHex: String,
    bgTheme: String,
    searchEngine: String,
    uiStyle: String,
    particlesOn: Boolean,
    starsOn: Boolean,
    orbOn: Boolean,
    weatherOn: Boolean,
    onAccentChange: (hex: String, lightHex: String) -> Unit,
    onThemeChange: (String) -> Unit,
    onEngineChange: (String) -> Unit,
    onUiStyleChange: (String) -> Unit,
    onToggleParticles: (Boolean) -> Unit,
    onToggleStars: (Boolean) -> Unit,
    onToggleOrb: (Boolean) -> Unit,
    onToggleWeather: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A0A14),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(EclipseBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Title
            Text(
                text = "CUSTOMIZE",
                fontFamily = Outfit,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = TextPrimary,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Accent Colors
            SectionLabel("ACCENT COLOR")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccentColors.forEach { pair ->
                    val isSelected = pair.hex == accentHex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(pair.primary)
                            .then(
                                if (isSelected) Modifier.border(2.dp, TextPrimary, CircleShape)
                                else Modifier
                            )
                            .clickable {
                                onAccentChange(pair.hex, "#${
                                    Integer.toHexString(pair.light.hashCode()).uppercase().padStart(8, '0')
                                }")
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Background Theme
            SectionLabel("BACKGROUND")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("eclipse" to "Eclipse", "nebula" to "Nebula", "aurora" to "Aurora", "void" to "Void").forEach { (key, label) ->
                    ThemeChip(
                        label = label,
                        selected = bgTheme == key,
                        accentColor = accentColor,
                        onClick = { onThemeChange(key) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Search Engine
            SectionLabel("SEARCH ENGINE")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("duckduckgo" to "DDG", "google" to "Google", "bing" to "Bing", "brave" to "Brave").forEach { (key, label) ->
                    ThemeChip(
                        label = label,
                        selected = searchEngine == key,
                        accentColor = accentColor,
                        onClick = { onEngineChange(key) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // UI Style
            SectionLabel("UI DENSITY")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("compact" to "Compact", "normal" to "Normal", "expanded" to "Expanded").forEach { (key, label) ->
                    ThemeChip(
                        label = label,
                        selected = uiStyle == key,
                        accentColor = accentColor,
                        onClick = { onUiStyleChange(key) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Visual Effects toggles
            SectionLabel("VISUAL EFFECTS")
            Spacer(modifier = Modifier.height(8.dp))
            ToggleRow("Stars", starsOn, accentColor) { onToggleStars(it) }
            ToggleRow("Particles", particlesOn, accentColor) { onToggleParticles(it) }
            ToggleRow("Eclipse Orb", orbOn, accentColor) { onToggleOrb(it) }
            ToggleRow("Weather", weatherOn, accentColor) { onToggleWeather(it) }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = SpaceMono,
        fontSize = 10.sp,
        color = TextMuted,
        letterSpacing = 2.sp
    )
}

@Composable
private fun ThemeChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.15f) else EclipseSurface,
        animationSpec = spring(),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) accentColor else TextMuted,
        animationSpec = spring(),
        label = "chipText"
    )

    Text(
        text = label,
        fontFamily = Outfit,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(
                if (selected) Modifier.border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun ToggleRow(
    label: String,
    enabled: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = Outfit,
            fontSize = 15.sp,
            color = TextPrimary
        )
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.25f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = EclipseSurface
            )
        )
    }
}
