package com.eclipse.browser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.*

@Composable
fun SearchBar(
    accentColor: Color,
    isIncognito: Boolean = false,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }

    val focusColor = if (isIncognito) VoidPurple else accentColor

    val borderColor by animateColorAsState(
        targetValue = if (focused) focusColor else Color.White.copy(alpha = 0.09f),
        animationSpec = tween(300),
        label = "searchBorder"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (focused) 0.12f else 0f,
        animationSpec = tween(300),
        label = "searchGlow"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(EclipseSurface)
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .drawBehind {
                    if (glowAlpha > 0f) {
                        drawRoundRect(
                            color = focusColor.copy(alpha = glowAlpha),
                            cornerRadius = CornerRadius(18.dp.toPx()),
                            style = Stroke(width = 12.dp.toPx())
                        )
                    }
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = Outfit
                ),
                cursorBrush = SolidColor(accentColor),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) onSearch(query)
                }),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = if (isIncognito) "Search in void..." else "Search or enter address...",
                                color = TextMuted2,
                                fontSize = 14.sp,
                                fontFamily = Outfit
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // Search button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isIncognito) VoidPurple.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.15f))
                .border(
                    1.dp,
                    if (isIncognito) VoidPurple.copy(alpha = 0.4f) else accentColor.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .clickable {
                    if (query.isNotBlank()) onSearch(query)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Search",
                tint = if (isIncognito) VoidPurpleLight else accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
