package com.eclipse.browser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.*

@Composable
fun BottomNavBar(
    accentColor: Color,
    tabCount: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isIncognito: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BottomBarBg)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back
        NavButton(
            text = "‹",
            fontSize = 22,
            enabled = canGoBack,
            onClick = onBack
        )

        // Forward
        NavButton(
            text = "›",
            fontSize = 22,
            enabled = canGoForward,
            onClick = onForward
        )

        // Home (golden circle)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (isIncognito) VoidPurple else AccentGold,
                    shape = CircleShape
                )
                .clickable { onHome() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isIncognito) VoidPurple else AccentGold,
                        CircleShape
                    )
            )
        }

        // Tabs with badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable { onTabs() },
            contentAlignment = Alignment.Center
        ) {
            // Tab count box
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(1.5.dp, TextMuted, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabCount.toString(),
                    fontFamily = SpaceMono,
                    fontSize = 10.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Menu
        NavButton(
            text = "⋯",
            fontSize = 20,
            enabled = true,
            onClick = onMenu
        )
    }
}

@Composable
private fun NavButton(
    text: String,
    fontSize: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (enabled) TextPrimary else TextMuted2,
        animationSpec = spring(),
        label = "navColor"
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            color = color,
            fontWeight = FontWeight.Light
        )
    }
}
