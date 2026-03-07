package com.eclipse.browser.ui.sheets

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.eclipse.browser.ui.components.EclipseEnterAnimation
import com.eclipse.browser.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSheet(
    accentColor: Color,
    isIncognito: Boolean,
    adBlockOn: Boolean,
    adsBlocked: Int,
    trackersBlocked: Int,
    onToggleAdBlock: (Boolean) -> Unit,
    onShowCustomize: () -> Unit,
    onShowHistory: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShowAdPanel: () -> Unit,
    onShowAbout: () -> Unit,
    onEnterIncognito: () -> Unit,
    onExitIncognito: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "MENU",
                fontFamily = Outfit,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = TextPrimary,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Quick Actions
            EclipseEnterAnimation(index = 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EclipseSurface)
                        .padding(4.dp)
                ) {
                    MenuItem(
                        icon = "🎨",
                        label = "Customize",
                        sublabel = "Theme, colors, effects",
                        accentColor = accentColor
                    ) { onShowCustomize(); onDismiss() }

                    MenuItem(
                        icon = "📝",
                        label = "History",
                        accentColor = accentColor
                    ) { onShowHistory(); onDismiss() }

                    MenuItem(
                        icon = "★",
                        label = "Bookmarks",
                        accentColor = accentColor
                    ) { onShowBookmarks(); onDismiss() }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy Section
            EclipseEnterAnimation(index = 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EclipseSurface)
                        .padding(4.dp)
                ) {
                    if (isIncognito) {
                        MenuItem(
                            icon = "☀️",
                            label = "Exit Void Mode",
                            sublabel = "Return to normal browsing",
                            accentColor = VoidPurple
                        ) { onExitIncognito(); onDismiss() }
                    } else {
                        MenuItem(
                            icon = "👁",
                            label = "Void Mode",
                            sublabel = "Browse without a trace",
                            accentColor = VoidPurple
                        ) { onEnterIncognito(); onDismiss() }
                    }

                    MenuToggleItem(
                        icon = "🛡",
                        label = "Ad Shield",
                        sublabel = "$adsBlocked ads · $trackersBlocked trackers",
                        enabled = adBlockOn,
                        accentColor = accentColor,
                        onToggle = onToggleAdBlock
                    )

                    MenuItem(
                        icon = "📊",
                        label = "Shield Stats",
                        sublabel = "View blocking details",
                        accentColor = accentColor
                    ) { onShowAdPanel(); onDismiss() }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info
            EclipseEnterAnimation(index = 2) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EclipseSurface)
                        .padding(4.dp)
                ) {
                    MenuItem(
                        icon = "◐",
                        label = "About Eclipse",
                        sublabel = "Dawn 0.1",
                        accentColor = accentColor
                    ) { onShowAbout(); onDismiss() }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MenuItem(
    icon: String,
    label: String,
    sublabel: String? = null,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = Outfit,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = TextPrimary
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    fontFamily = SpaceMono,
                    fontSize = 10.sp,
                    color = TextMuted2,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Text(
            text = "›",
            fontSize = 16.sp,
            color = TextMuted2
        )
    }
}

@Composable
private fun MenuToggleItem(
    icon: String,
    label: String,
    sublabel: String,
    enabled: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = Outfit,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Text(
                text = sublabel,
                fontFamily = SpaceMono,
                fontSize = 10.sp,
                color = TextMuted2,
                letterSpacing = 0.5.sp
            )
        }
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
