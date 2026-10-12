package com.example.chopsticks.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.ui.game.ThemedHand
import com.example.chopsticks.ui.theme.*

/**
 * A theme in the picker, shown as a miniature of itself: the theme's own backdrop, one of its real
 * hands holding up two fingers, and a nameplate in the theme's own style. The selected card sits
 * forward with a border in its theme's colour; the others sit back slightly.
 */
@Composable
fun ThemeCard(theme: HandTheme, selected: Boolean, currentTheme: HandTheme, onClick: () -> Unit) {
    val lift by animateFloatAsState(if (selected) 1f else 0.93f, tween(200), label = "card_lift")
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .width(112.dp)
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
                alpha = if (selected) 1f else 0.88f
            }
            .clip(shape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) theme.primaryColor else currentTheme.textColor.copy(alpha = 0.25f),
                shape = shape
            )
            .clickable { onClick() }
    ) {
        Box(Modifier.fillMaxWidth().height(118.dp)) {
            ThemeBackdrop(theme, Modifier.fillMaxSize())
            ThemedHand(
                fingers = 2,
                maxFingers = 5,
                isDead = false,
                theme = theme,
                isLeftHand = false,
                isActive = true,
                modifier = Modifier.align(Alignment.Center).height(96.dp)
            )
        }
        ThemeNameplate(theme)
    }
}

/** The theme's world in miniature, drawn with the same code as the game board. */
@Composable
private fun ThemeBackdrop(theme: HandTheme, modifier: Modifier) {
    val pixels = remember { PixelBuffer() }
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        when (theme) {
            is HandTheme.Ombromanie -> drawLampLitScreen(Offset(size.width / 2, size.height * 0.45f), 0.5f)
            is HandTheme.PaperCutout -> {
                // Painted sky, wooden stage, and the valance with its gold scallops
                drawRect(Brush.verticalGradient(listOf(PaperInk.SkyTop, PaperInk.SkyLow)), size = Size(size.width, size.height * 0.62f))
                val floor = size.height * 0.62f
                var y = floor
                var row = 0
                while (y < size.height) {
                    drawRect(if (row % 2 == 0) PaperInk.Plank else PaperInk.PlankDark, Offset(0f, y), Size(size.width, 12.dp.toPx()))
                    y += 12.dp.toPx(); row++
                }
                drawRect(PaperInk.Red, size = Size(size.width, 9.dp.toPx()))
                var x = 0f
                while (x < size.width) {
                    drawCircle(PaperInk.Gold, 5.dp.toPx(), Offset(x + 5.dp.toPx(), 9.dp.toPx()))
                    x += 10.dp.toPx()
                }
            }
            is HandTheme.TerraFirma -> drawPixelated(pixels, terraPixel()) {
                drawTerraWorld(day = 1f, time = 0.35f, horizon = 0.7f)
            }
            is HandTheme.Marginalia -> {
                drawManuscriptPage()
                drawIvyBorder(7.dp.toPx(), 0f, size.height)
            }
            is HandTheme.NeonRain -> drawNeonCity(time = 0.4f, textMeasurer = textMeasurer, showSign = false)
        }
    }
}

/** The theme's name, set the way that theme sets its own text. */
@Composable
private fun ThemeNameplate(theme: HandTheme) {
    val (background, textColor) = when (theme) {
        is HandTheme.Ombromanie -> ShadowInk.Ink to ShadowInk.Parchment
        is HandTheme.PaperCutout -> PaperInk.PaperWhite to PaperInk.Red
        is HandTheme.TerraFirma -> TerraInk.PanelFill to Color.White
        is HandTheme.Marginalia -> Folio.VellumLight to Folio.Ink
        is HandTheme.NeonRain -> Color(0xFF0B0E15) to NeonInk.Text
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .drawBehind {
                when (theme) {
                    is HandTheme.NeonRain -> drawRect(NeonInk.Red, size = Size(size.width, 2.dp.toPx()))
                    is HandTheme.PaperCutout -> drawRect(PaperInk.Outline, size = Size(size.width, 2.dp.toPx()))
                    is HandTheme.Marginalia -> drawRect(Folio.Vermilion.copy(alpha = 0.5f), Offset(0f, 2.dp.toPx()), Size(size.width, 1.dp.toPx()))
                    is HandTheme.TerraFirma -> drawRect(TerraInk.PanelRim, size = Size(size.width, 2.dp.toPx()))
                    is HandTheme.Ombromanie -> drawRect(ShadowInk.Gold, size = Size(size.width, 1.dp.toPx()))
                }
            }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        val name = when (theme) {
            is HandTheme.NeonRain -> theme.name.uppercase()
            else -> theme.name
        }
        Text(
            text = if (theme is HandTheme.Marginalia) {
                // A rubricated initial, as the theme writes its own labels
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Folio.Vermilion, fontWeight = FontWeight.Black)) { append(name.take(1)) }
                    append(name.drop(1))
                }
            } else buildAnnotatedString { append(name) },
            color = textColor,
            fontSize = 12.sp,
            fontFamily = theme.typeface,
            fontWeight = if (theme is HandTheme.PaperCutout || theme is HandTheme.TerraFirma) FontWeight.Black else FontWeight.Bold,
            fontStyle = if (theme is HandTheme.Ombromanie) FontStyle.Italic else FontStyle.Normal,
            letterSpacing = if (theme is HandTheme.NeonRain) 1.5.sp else 0.sp
        )
    }
}
