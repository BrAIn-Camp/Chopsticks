package com.example.chopsticks.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.chopsticks.ui.theme.HandTheme
import com.example.chopsticks.ui.theme.PaperPillButton
import com.example.chopsticks.ui.theme.RubricButton
import com.example.chopsticks.ui.theme.TerraPanelButton
import com.example.chopsticks.ui.theme.ShadowTicketButton
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*

@Composable
fun SectionHeader(text: String, theme: HandTheme) {
    Text(
        text = "▼ $text",
        color = theme.textColor.copy(alpha = 0.7f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
fun RowScope.ModeButton(text: String, selected: Boolean, theme: HandTheme, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clip(RoundedCornerShape(theme.cornerRadius))
            .background(if (selected) theme.primaryColor else theme.backgroundColor)
            .border(1.dp, if (selected) theme.primaryColor else theme.secondaryColor.copy(alpha = 0.5f), shape = RoundedCornerShape(theme.cornerRadius))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) (if (theme.isDark) Color.Black else Color.White) else theme.textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ThemeCard(theme: HandTheme, selected: Boolean, currentTheme: HandTheme, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(currentTheme.cornerRadius))
            .background(theme.backgroundColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) currentTheme.primaryColor else currentTheme.secondaryColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(currentTheme.cornerRadius)
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(theme.primaryColor, shape = RoundedCornerShape(theme.cornerRadius))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = theme.name,
            color = theme.textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RuleToggle(
    label: String,
    checked: Boolean,
    theme: HandTheme,
    description: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = label, color = theme.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, color = theme.textColor.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.dp, theme.primaryColor, shape = RoundedCornerShape(4.dp))
                .background(if (checked) theme.primaryColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (theme.isDark) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ArtisticActionButton(
    text: String,
    onClick: () -> Unit,
    theme: HandTheme,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val buttonAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        label = "button_alpha"
    )

    if (theme is HandTheme.AnalogSynth) {
        val infiniteTransition = rememberInfiniteTransition(label = "led_glow")
        val alphaGlow by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "led_glow_alpha"
        )
        
        Box(
            modifier = modifier
                .height(48.dp)
                .graphicsLayer { alpha = buttonAlpha }
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF252525))
                .border(
                    width = 2.dp,
                    color = theme.primaryColor.copy(alpha = if (enabled) 0.8f else 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val ledColor = if (enabled) theme.fingerColor else Color.Gray
                Box(contentAlignment = Alignment.Center, modifier = Modifier.width(16.dp).height(16.dp)) {
                    if (enabled) {
                        Box(
                            modifier = Modifier.width(16.dp).height(16.dp)
                                .background(ledColor.copy(alpha = 0.3f * alphaGlow), shape = CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier.width(8.dp).height(8.dp)
                            .background(ledColor, shape = CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text.uppercase(),
                    color = if (enabled) theme.textColor else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    } else if (theme is HandTheme.Ombromanie) {
        ShadowTicketButton(text, onClick, modifier, enabled, buttonAlpha)
    } else if (theme is HandTheme.Marginalia) {
        RubricButton(text, onClick, modifier, enabled, buttonAlpha)
    } else if (theme is HandTheme.TerraFirma) {
        TerraPanelButton(text, onClick, modifier, enabled, buttonAlpha)
    } else {
        PaperPillButton(text, onClick, modifier, enabled, buttonAlpha)
    }
}
