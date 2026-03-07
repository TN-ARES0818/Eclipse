package com.eclipse.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.Outfit
import com.eclipse.browser.ui.theme.TextPrimary

@Composable
fun EclipseToast(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(tween(280)) { 20 } + fadeIn(tween(280)),
        exit = slideOutVertically(tween(200)) { 20 } + fadeOut(tween(200)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF5141423),
            border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
            tonalElevation = 8.dp
        ) {
            Text(
                text = message ?: "",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                fontSize = 13.sp,
                color = TextPrimary,
                fontFamily = Outfit
            )
        }
    }
}
