package com.example.chopsticks.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThemedHand(
    fingers: Int,
    maxFingers: Int,
    isDead: Boolean,
    theme: HandTheme,
    isLeftHand: Boolean,
    modifier: Modifier = Modifier,
    isInvalid: Boolean = false,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hand_animations")
    val textMeasurer = rememberTextMeasurer()
    val pixelBuffer = remember { PixelBuffer() }
    // Terra Firma: the selected inventory slot pops up slightly, as the selected hotbar slot does
    val slotScale by animateFloatAsState(
        targetValue = if (theme is HandTheme.TerraFirma && isActive) 1.05f else 1f,
        animationSpec = tween(250),
        label = "slot_scale"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val activeAlpha by animateFloatAsState(
        // Paper sprites and inventory slots barely dim: spotlights / slot highlights show whose turn it is
        targetValue = if (isActive) 1f else if (theme is HandTheme.PaperCutout || theme is HandTheme.TerraFirma) 0.82f else 0.4f,
        animationSpec = tween(500),
        label = "active_alpha"
    )

    val invalidPulse by animateFloatAsState(
        targetValue = if (isInvalid) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "invalid_pulse"
    )

    val shakeX by animateFloatAsState(
        targetValue = if (isInvalid) (sin(System.currentTimeMillis() / 20f) * 6f) else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "shake"
    )

    // Paper Cutout: when it becomes this hand's turn it flips like a sheet of paper, with a little hop
    val flip = remember { Animatable(0f) }
    LaunchedEffect(isActive, theme) {
        if (theme is HandTheme.PaperCutout && isActive) {
            flip.snapTo(0f)
            flip.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(0.75f)
            .graphicsLayer {
                translationX = shakeX
                alpha = activeAlpha
                if (theme is HandTheme.TerraFirma) {
                    scaleX = slotScale
                    scaleY = slotScale
                }
                if (theme is HandTheme.PaperCutout) {
                    scaleX = cos(flip.value * 2f * PI.toFloat())
                    translationY = -sin(flip.value * PI.toFloat()) * 14.dp.toPx()
                }
            }
            .drawWithCache {
                onDrawWithContent {
                    val w = size.width
                    val h = size.height
                    val jitter = if (theme.organicEdges) 4f else 0f

                    if (isDead && theme is HandTheme.TerraFirma) {
                        drawPixelated(pixelBuffer, terraPixel()) {
                            drawTerraSlot(selected = isActive)
                            drawTombstone(width / 2f, height * 0.8f)
                        }
                        return@onDrawWithContent
                    }

                    if (isDead && theme is HandTheme.PaperCutout) {
                        drawCrumpledPaper(Offset(w / 2, h * 0.7f), w * 0.2f)
                        return@onDrawWithContent
                    }

                    if (isDead && theme is HandTheme.Marginalia) {
                        val vacatRect = Rect(Offset(w * 0.14f, h * 0.48f), Size(w * 0.72f, h * 0.48f))
                        drawManuscriptVacat(vacatRect, isLeftHand, h, textMeasurer)
                        return@onDrawWithContent
                    }

                    if (isDead && theme is HandTheme.Ombromanie) {
                        val ghostRect = Rect(Offset(w * 0.14f, h * 0.48f), Size(w * 0.72f, h * 0.48f))
                        drawShadowGhost(ghostRect, isLeftHand, h, pulse)
                        return@onDrawWithContent
                    }

                    if (isDead) {
                        drawRect(Color.Gray.copy(alpha = 0.1f))
                        drawLine(
                            theme.textColor.copy(alpha = 0.4f),
                            Offset(0f, 0f),
                            Offset(w, h),
                            4.dp.toPx()
                        )
                        drawLine(
                            theme.textColor.copy(alpha = 0.4f),
                            Offset(w, 0f),
                            Offset(0f, h),
                            4.dp.toPx()
                        )
                        return@onDrawWithContent
                    }

                    // --- ANATOMICAL DIMENSIONS ---
                    val palmWidth = w * 0.72f
                    val palmHeight = h * 0.48f
                    val palmTop = h * 0.48f
                    val palmRect = Rect(
                        offset = Offset((w - palmWidth) / 2, palmTop),
                        size = Size(palmWidth, palmHeight)
                    )

                    // --- DRAW PALM ---
                    val palmPath = createAnatomicalPalmPath(palmRect, isLeftHand, jitter)
                    
                    if (invalidPulse > 0.01f) {
                        drawPath(
                            palmPath, 
                            Color.Red.copy(alpha = 0.4f * invalidPulse),
                            style = Stroke(6.dp.toPx())
                        )
                    }
                    
                    when (theme) {
                        is HandTheme.AnalogSynth -> {
                            val plateBrush = Brush.linearGradient(
                                colors = listOf(theme.handColor, theme.handColor.copy(alpha = 0.85f)),
                                start = Offset(palmRect.left, palmRect.top),
                                end = Offset(palmRect.right, palmRect.bottom)
                            )
                            drawPath(palmPath, plateBrush)
                            drawPath(palmPath, theme.secondaryColor.copy(alpha = 0.3f), style = Stroke(2.dp.toPx()))
                            
                            val gridPath = Path()
                            for (gx in 0..palmRect.width.toInt() step 40) {
                                gridPath.moveTo(palmRect.left + gx, palmRect.top + 20.dp.toPx())
                                gridPath.lineTo(palmRect.left + gx, palmRect.bottom - 10.dp.toPx())
                            }
                            for (gy in 0..palmRect.height.toInt() step 40) {
                                gridPath.moveTo(palmRect.left + 10.dp.toPx(), palmRect.top + gy)
                                gridPath.lineTo(palmRect.right - 10.dp.toPx(), palmRect.top + gy)
                            }
                            drawPath(gridPath, theme.secondaryColor.copy(alpha = 0.4f), style = Stroke(1.dp.toPx()))

                            // OSCILLOSCOPE: Flat if not active
                            val oscPath = Path()
                            val amplitude = if (isActive) 25.dp.toPx() else 0f
                            for (x in 0..palmRect.width.toInt() step 5) {
                                val y = sin((x + pulse * 600) * 0.08f) * amplitude
                                if (x == 0) oscPath.moveTo(palmRect.left + x, palmRect.center.y + y)
                                else oscPath.lineTo(palmRect.left + x, palmRect.center.y + y)
                            }
                            drawPath(oscPath, theme.primaryColor, style = Stroke(4.dp.toPx()))
                        }
                        // Drawn as a whole glove once the fingers are laid out, below
                        is HandTheme.PaperCutout -> Unit
                        // Drawn as pixel art inside its slot once the fingers are laid out, below
                        is HandTheme.TerraFirma -> Unit
                        is HandTheme.Ombromanie -> drawShadowPalm(palmRect, isLeftHand, bottom = h)
                        is HandTheme.Marginalia -> drawManuscriptHand(palmRect, isLeftHand, bottom = h, fingers = fingers, textMeasurer = textMeasurer)
                    }

                    // --- DRAW FINGERS ---
                    val thumbSide = if (isLeftHand) 1f else 0f
                    val pinkySide = if (isLeftHand) 0f else 1f
                    
                    val fingerBases = mutableListOf<Offset>()
                    val fingerAngles = mutableListOf<Float>()
                    val fingerLengths = mutableListOf<Float>()
                    val fingerWidths = mutableListOf<Float>()

                    if (maxFingers <= 5) {
                        // Maintain original anatomical proportions for standard play
                        fingerBases.addAll(listOf(
                            Offset(palmRect.left + palmWidth * (pinkySide * 0.9f + thumbSide * 0.1f), palmRect.top + 14.dp.toPx()), // Pinky
                            Offset(palmRect.left + palmWidth * (pinkySide * 0.7f + thumbSide * 0.3f), palmRect.top + 4.dp.toPx()),  // Ring
                            Offset(palmRect.left + palmWidth * (pinkySide * 0.5f + thumbSide * 0.5f), palmRect.top + 0.dp.toPx()),  // Middle
                            Offset(palmRect.left + palmWidth * (pinkySide * 0.3f + thumbSide * 0.7f), palmRect.top + 6.dp.toPx()),  // Index
                            Offset(palmRect.left + palmWidth * (pinkySide * 0.05f + thumbSide * 0.95f), palmRect.top + palmHeight * 0.45f) // Thumb
                        ))
                        fingerAngles.addAll(listOf(
                            if (isLeftHand) -15f else 15f,
                            if (isLeftHand) -6f else 6f,
                            0f,
                            if (isLeftHand) 8f else -8f,
                            if (isLeftHand) 42f else -42f
                        ))
                        fingerLengths.addAll(listOf(h * 0.36f, h * 0.44f, h * 0.48f, h * 0.42f, h * 0.30f))
                        fingerWidths.addAll(listOf(w * 0.11f, w * 0.13f, w * 0.14f, w * 0.13f, w * 0.18f))
                    } else {
                        // Dynamic scaling for up to 10 fingers
                        val numKnuckleFingers = maxFingers - 1
                        val knuckleStart = 0.1f
                        val knuckleEnd = 0.8f

                        for (i in 0 until numKnuckleFingers) {
                            val t = i.toFloat() / (numKnuckleFingers - 1).coerceAtLeast(1)
                            val xRel = knuckleStart + t * (knuckleEnd - knuckleStart)
                            val xPos = if (isLeftHand) xRel else (1f - xRel)
                            
                            val arcT = t
                            val yOffset = (1f - sin(arcT * Math.PI.toFloat())) * 14.dp.toPx()
                            
                            fingerBases.add(Offset(palmRect.left + palmWidth * xPos, palmRect.top + yOffset))
                            
                            // Anatomical angle arc
                            val baseAngle = -15f + t * 23f
                            fingerAngles.add(if (isLeftHand) baseAngle else -baseAngle)
                            
                            // Anatomical length curve (Middle longest at t~0.6)
                            val length = h * (0.48f - 0.3f * (t - 0.6f) * (t - 0.6f))
                            fingerLengths.add(length)
                            
                            val width = (knuckleEnd - knuckleStart) * palmWidth / numKnuckleFingers * 0.85f
                            fingerWidths.add(width)
                        }
                        
                        // Thumb always at the side
                        val thumbX = if (isLeftHand) 0.95f else 0.05f
                        fingerBases.add(Offset(palmRect.left + palmWidth * thumbX, palmRect.top + palmHeight * 0.45f))
                        fingerAngles.add(if (isLeftHand) 42f else -42f)
                        fingerLengths.add(h * 0.30f)
                        fingerWidths.add(w * 0.18f * (5f / maxFingers).coerceAtLeast(0.6f))
                    }

                    // Map finger count to active indices
                    val activeIndices = if (maxFingers <= 5) {
                        when (fingers) {
                            1 -> listOf(3)
                            2 -> listOf(3, 2)
                            3 -> listOf(3, 2, 1)
                            4 -> listOf(3, 2, 1, 0)
                            else -> if (fingers >= 5) listOf(0, 1, 2, 3, 4) else emptyList()
                        }
                    } else {
                        if (fingers >= maxFingers) {
                            (0 until maxFingers).toList()
                        } else {
                            val numKnuckleFingers = maxFingers - 1
                            val indexPosition = numKnuckleFingers - 1
                            (0 until fingers).map { i -> 
                                (indexPosition - i).coerceAtLeast(0)
                            }
                        }
                    }

                    if (theme is HandTheme.TerraFirma) {
                        // The hand sits inside its slot, a little smaller than the slot itself
                        val inset = android.graphics.Matrix().apply { setScale(0.8f, 0.8f, w / 2f, h * 0.46f) }
                        drawPixelated(pixelBuffer, terraPixel(), inset) {
                            drawTerraSlot(selected = isActive)
                            drawTerraHand(palmRect, isLeftHand, h, fingerBases, fingerAngles, fingerLengths, fingerWidths, activeIndices)
                        }
                        // The finger count as the item's stack count, bottom-left of the slot
                        drawTerraText(textMeasurer, fingers.toString(), Offset(9.dp.toPx(), h - 32.dp.toPx()), Color.White, 20.sp)
                        return@onDrawWithContent
                    }

                    if (theme is HandTheme.PaperCutout) {
                        drawPaperGlove(palmRect, isLeftHand, fingerBases, fingerAngles, fingerLengths, fingerWidths, activeIndices, fingers, textMeasurer)
                        return@onDrawWithContent
                    }

                    for (i in 0 until maxFingers) {
                        val isActive = i in activeIndices
                        if (!isActive) continue

                        drawAnatomicalFinger(
                            base = fingerBases[i],
                            angle = fingerAngles[i],
                            length = fingerLengths[i],
                            width = fingerWidths[i],
                            theme = theme,
                            jitter = jitter
                        )

                        // --- THEME ADAPTATION OVERLAYS ---
                        if (theme is HandTheme.AnalogSynth) {
                            val fx = fingerBases[i].x
                            val fy = fingerBases[i].y - fingerLengths[i] * 0.6f
                            val cable = Path().apply {
                                moveTo(palmRect.center.x, palmRect.center.y)
                                cubicTo(
                                    palmRect.center.x - 30.dp.toPx(), palmRect.top - 20.dp.toPx(),
                                    fx + 20.dp.toPx(), palmRect.top - 40.dp.toPx(),
                                    fx, fy
                                )
                            }
                            drawPath(cable, theme.primaryColor, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                            
                            val lightColor = if (pulse > 0.5f) theme.primaryColor else theme.secondaryColor
                            val tipPos = fingerBases[i] + Offset(
                                (sin(Math.toRadians(fingerAngles[i].toDouble())) * -fingerLengths[i]).toFloat(),
                                (-Math.cos(Math.toRadians(fingerAngles[i].toDouble())) * fingerLengths[i]).toFloat()
                            )
                            drawCircle(lightColor, 6.dp.toPx(), tipPos + Offset(0f, 10.dp.toPx()))
                        }
                    }
                }
            }
    )
}

private fun createAnatomicalPalmPath(rect: Rect, isLeft: Boolean, jitter: Float): Path {
    val path = Path()
    val w = rect.width
    val h = rect.height
    val l = rect.left
    val t = rect.top

    val tX = if (isLeft) 1f else 0f // Thumb side
    val pX = if (isLeft) 0f else 1f // Pinky side

    // Start at bottom center (wrist)
    path.moveTo(l + w * 0.5f, t + h * 0.95f)

    // Wrist area - slight inward curve for anatomical realism
    path.quadraticTo(l + w * (0.5f + (pX - 0.5f) * 0.7f), t + h * 0.98f, l + w * (0.5f + (pX - 0.5f) * 0.85f), t + h * 0.9f)

    // Heel of the palm (pinky side) - fleshy bulge (Hypothenar eminence)
    path.quadraticTo(l + w * (pX * 1.08f + tX * -0.08f), t + h * 0.75f, l + w * pX, t + h * 0.55f)
    
    // Pinky side up to knuckle
    path.lineTo(l + w * pX, t + h * 0.3f)
    
    // Knuckle arc - fleshy and pronounced
    path.cubicTo(
        l + w * (pX * 0.7f + tX * 0.3f), t + h * 0.02f,
        l + w * (pX * 0.3f + tX * 0.7f), t - h * 0.05f, // Middle finger knuckle is highest
        l + w * (pX * 0.1f + tX * 0.9f), t + h * 0.2f
    )
    
    // Thumb web space - inward curve
    path.quadraticTo(
        l + w * (tX * 0.8f + pX * 0.2f), t + h * 0.38f,
        l + w * (tX * 0.92f + pX * 0.08f), t + h * 0.55f
    )

    // Thenar eminence (thumb mound) - large fleshy bulge
    path.quadraticTo(
        l + w * (tX * 1.25f + pX * -0.25f), t + h * 0.85f,
        l + w * 0.5f, t + h * 0.95f
    )
    
    path.close()
    
    if (jitter > 0) {
        return applyJitterToPath(path, jitter)
    }
    return path
}

private fun DrawScope.drawAnatomicalFinger(
    base: Offset,
    angle: Float,
    length: Float,
    width: Float,
    theme: HandTheme,
    jitter: Float
) {
    val segments = 3
    val segLength = length / segments
    
    rotate(angle, base) {
        var currentBase = base
        for (seg in 0 until segments) {
            val isTip = seg == segments - 1
            val segW = width * (1f - seg * 0.1f) // Fleshy tapering
            val nextW = width * (1f - (seg + 1) * 0.1f)
            
            val segPath = Path().apply {
                val leftBase = currentBase.x - segW / 2
                val rightBase = currentBase.x + segW / 2
                val leftTop = currentBase.x - nextW / 2
                val rightTop = currentBase.x + nextW / 2
                val topY = currentBase.y - segLength
                
                moveTo(leftBase, currentBase.y)
                // Left fleshy bulge
                quadraticTo(leftBase - 3.dp.toPx(), currentBase.y - segLength * 0.5f, leftTop, topY)
                
                if (isTip) {
                    // Soft rounded tip
                    quadraticTo(currentBase.x, topY - 10.dp.toPx(), rightTop, topY)
                } else {
                    lineTo(rightTop, topY)
                }
                
                // Right fleshy bulge
                quadraticTo(rightBase + 3.dp.toPx(), currentBase.y - segLength * 0.5f, rightBase, currentBase.y)
                close()
            }
            
            val finalPath = if (jitter > 0) applyJitterToPath(segPath, jitter) else segPath
            
            val segRect = Rect(Offset(currentBase.x - segW/2, currentBase.y - segLength), Size(segW, segLength))
            
            when (theme) {
                is HandTheme.AnalogSynth -> {
                    val plateBrush = Brush.verticalGradient(
                        colors = listOf(theme.fingerColor.copy(alpha=0.9f), theme.fingerColor),
                        startY = segRect.bottom,
                        endY = segRect.top
                    )
                    drawPath(finalPath, plateBrush)
                    // Ball-and-socket joint
                    drawCircle(theme.secondaryColor, radius = segW * 0.45f, center = currentBase)
                    drawCircle(Color.White.copy(alpha=0.25f), radius = segW * 0.2f, center = currentBase - Offset(2.dp.toPx(), 2.dp.toPx()))
                }
                // The glove is drawn whole by drawPaperGlove, never segment by segment
                is HandTheme.PaperCutout -> Unit
                is HandTheme.TerraFirma -> Unit
                // A silhouette has no inner detail: the shape is everything
                is HandTheme.Ombromanie -> {
                    drawPath(finalPath, theme.fingerColor)
                    // Same-colour outline seals the hairline seams between segments
                    drawPath(finalPath, theme.fingerColor, style = Stroke(1.5.dp.toPx()))
                }
                is HandTheme.Marginalia -> drawManuscriptFingerSegment(finalPath, segRect)
            }
            
            currentBase = Offset(currentBase.x, currentBase.y - segLength)
        }
    }
}


@Preview(device = "id:pixel_7", showBackground = true)
@Composable
fun GameScreenThemesPreview() {
    var themeIndex by remember { mutableStateOf(0) }
    val themes = HandTheme.allThemes
    val currentTheme = themes[themeIndex]
    
    Column(modifier = Modifier.fillMaxSize().background(currentTheme.backgroundColor)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
            themes.forEachIndexed { index, theme ->
                Button(
                    onClick = { themeIndex = index },
                    modifier = Modifier.padding(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                ) {
                    Text(theme.name.take(1), color = if (theme.isDark) Color.Black else Color.White)
                }
            }
        }
        
        Text("Left vs Right Hand (Anatomy Check)", color = currentTheme.textColor, modifier = Modifier.padding(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            ThemedHand(
                fingers = 5,
                maxFingers = 5,
                isDead = false,
                isInvalid = false,
                theme = currentTheme,
                isLeftHand = true,
                modifier = Modifier.weight(1f)
            )
            ThemedHand(
                fingers = 5,
                maxFingers = 5,
                isDead = false,
                isInvalid = false,
                theme = currentTheme,
                isLeftHand = false,
                modifier = Modifier.weight(1f)
            )
        }
        
        Text("Finger Counts (Index -> All)", color = currentTheme.textColor, modifier = Modifier.padding(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { i ->
                ThemedHand(
                    fingers = i + 1,
                    maxFingers = 5,
                    isDead = false,
                    isInvalid = false,
                    theme = currentTheme,
                    isLeftHand = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("10-Finger Scaling Check", color = currentTheme.textColor, modifier = Modifier.padding(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { i ->
                val fingerCount = (i + 1) * 2
                ThemedHand(
                    fingers = fingerCount,
                    maxFingers = 10,
                    isDead = false,
                    isInvalid = false,
                    theme = currentTheme,
                    isLeftHand = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
