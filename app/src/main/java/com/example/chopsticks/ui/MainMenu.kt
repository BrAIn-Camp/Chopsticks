package com.example.chopsticks.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.domain.model.Difficulty
import com.example.chopsticks.domain.model.GameMode
import com.example.chopsticks.domain.model.RulesetConfig
import com.example.chopsticks.ui.screens.DynamicBackground
import com.example.chopsticks.ui.theme.*

@Composable
fun MainMenu(
    currentRuleset: RulesetConfig,
    currentTheme: HandTheme?,
    onThemeChange: (HandTheme) -> Unit,
    onStartGame: (GameMode) -> Unit,
    onGameRules: () -> Unit,
    onHowToPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeTheme = currentTheme ?: HandTheme.Default
    var gameMode by remember { mutableStateOf(GameMode.PvE) }
    
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        DynamicBackground(theme = safeTheme)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Spacer(modifier = Modifier.height(60.dp))
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                val scale = (maxWidth.value / 320f).coerceAtMost(1f)
                
                // Stylized Chopsticks Background
                ChopsticksBackground(
                    theme = safeTheme,
                    scale = scale,
                    modifier = Modifier.size(width = 320.dp * scale, height = 120.dp * scale)
                )

                // Title Text
                Box(
                    modifier = Modifier
                        .border(3.dp * scale, safeTheme.primaryColor)
                        .background(safeTheme.backgroundColor)
                        .padding(horizontal = 32.dp * scale, vertical = 16.dp * scale)
                ) {
                    Text(
                        text = "CHOPSTICKS",
                        color = safeTheme.textColor,
                        fontSize = 40.sp * scale,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp * scale,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))

            // Mode Selection
            SectionHeader("GAME MODE", safeTheme)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeButton("VS COMPUTER", gameMode == GameMode.PvE, safeTheme) { gameMode = GameMode.PvE }
                ModeButton("LOCAL PVP", gameMode == GameMode.PvP, safeTheme) { gameMode = GameMode.PvP }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Theme Selection
            SectionHeader("VISUAL THEME", safeTheme)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(HandTheme.allThemes) { theme ->
                    ThemeCard(theme, safeTheme == theme, safeTheme) { onThemeChange(theme) }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onStartGame(gameMode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(safeTheme.cornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = safeTheme.primaryColor,
                        contentColor = if (safeTheme.isDark) Color.Black else Color.White
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START GAME", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onGameRules,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(safeTheme.cornerRadius),
                        border = BorderStroke(1.dp, safeTheme.textColor.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = safeTheme.textColor)
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GAME RULES", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onHowToPlay,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(safeTheme.cornerRadius),
                        border = BorderStroke(1.dp, safeTheme.textColor.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = safeTheme.textColor)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("HOW TO PLAY", fontSize = 13.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ChopsticksBackground(
    theme: HandTheme,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    val neonTime = if (theme is HandTheme.NeonRain) rememberNeonTime() else null
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        when (theme) {
            is HandTheme.NeonRain -> drawNeonChopsticks(scale, neonTime?.value ?: 0f)

            is HandTheme.PaperCutout -> drawPaperChopsticks(scale)

            is HandTheme.Ombromanie -> drawShadowChopsticks(scale)
            is HandTheme.Marginalia -> drawManuscriptChopsticks(scale)
            is HandTheme.TerraFirma -> drawTerraChopsticks(scale)
        }
    }
}

// Shared components moved to CommonComponents.kt



@Preview(device = "id:pixel_7", showBackground = true)
@Composable
fun MainMenuPreview() {
    MainMenu(
        currentRuleset = RulesetConfig(),
        currentTheme = HandTheme.Default,
        onThemeChange = {},
        onStartGame = { },
        onGameRules = {},
        onHowToPlay = {}
    )
}

@Preview(name = "Small Screen", widthDp = 280, showBackground = true)
@Composable
fun MainMenuSmallPreview() {
    MainMenu(
        currentRuleset = RulesetConfig(),
        currentTheme = HandTheme.Default,
        onThemeChange = {},
        onStartGame = { },
        onGameRules = {},
        onHowToPlay = {}
    )
}
