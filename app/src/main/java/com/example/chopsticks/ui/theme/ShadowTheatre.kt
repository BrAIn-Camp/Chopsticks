package com.example.chopsticks.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/*
 * Ombromanie: the 19th-century art of hand shadows.
 *
 * The board is a back-lit shadow screen. Hands are crisp cut-paper silhouettes (after Lotte Reiniger),
 * with lace cuffs and a crescent moon cut out so the lamplight shows through. A single lamp swings
 * toward whoever's turn it is. Moves fly as a silhouette swallow. The menu shows Montmartre rooftops
 * at three depths, as in Henri Rivière's shadow plays at Le Chat Noir, with the cat on the roof.
 */
object ShadowInk {
    val Ink = Color(0xFF15100E)
    val LampCore = Color(0xFFFFF2CC)
    val LampWarm = Color(0xFFF4C46A)
    val Amber = Color(0xFFD28A43)
    val Ember = Color(0xFF9C4A2C)
    val Dusk = Color(0xFF3C1E2C)
    val Curtain = Color(0xFF7E1A16)
    val CurtainDark = Color(0xFF420B0A)
    val Gold = Color(0xFFC99A3B)
    /** Lamplight seen through cut-outs in a silhouette. */
    val Light = Color(0xFFFFE7B3)
    val Parchment = Color(0xFFF3E3C0)
}

/** An irregular 0..1 flicker, like a flame in a draught. Read it inside draw code so only drawing repeats. */
@Composable
fun rememberCandleFlicker(): State<Float> {
    val transition = rememberInfiniteTransition(label = "candle")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 2600
                0.7f at 300
                0.2f at 700
                1f at 1100
                0.4f at 1600
                0.85f at 2100
                0.1f at 2400
            }
        ),
        label = "flicker"
    )
}

/** The lit screen: a warm lamp glow at [lamp], fading to ember and dusk at the edges. */
fun DrawScope.drawLampLitScreen(lamp: Offset, flicker: Float) {
    drawRect(ShadowInk.Amber)
    val radius = size.maxDimension * (0.60f + 0.025f * flicker)
    drawRect(
        Brush.radialGradient(
            0f to ShadowInk.LampCore,
            0.35f to ShadowInk.LampWarm,
            1f to ShadowInk.LampWarm.copy(alpha = 0f),
            center = lamp,
            radius = radius
        )
    )
    drawRect(
        Brush.radialGradient(
            0.55f to Color.Transparent,
            0.85f to ShadowInk.Ember.copy(alpha = 0.55f),
            1f to ShadowInk.Dusk.copy(alpha = 0.85f),
            center = center,
            radius = size.maxDimension * 0.78f
        )
    )
}

/** Velvet curtains down both edges of the stage, swaying very slightly. */
fun DrawScope.drawSideCurtains(sway: Float) {
    val w = 16.dp.toPx()
    val folds = Brush.horizontalGradient(
        listOf(ShadowInk.CurtainDark, ShadowInk.Curtain, ShadowInk.CurtainDark, ShadowInk.Curtain, ShadowInk.CurtainDark),
        startX = 0f, endX = w
    )
    fun curtain(mirror: Boolean) {
        val path = Path()
        val steps = 24
        path.moveTo(0f, 0f)
        for (i in 0..steps) {
            val y = size.height * i / steps
            val wave = sin(i * 0.9f + sway * 2f) * 3.dp.toPx()
            // Gathered toward the tie-back at 70% height
            val gather = 1f - 0.35f * (1f - abs(i.toFloat() / steps - 0.7f) / 0.7f).coerceIn(0f, 1f)
            path.lineTo(w * gather + wave, y)
        }
        path.lineTo(0f, size.height)
        path.close()
        if (mirror) {
            translate(left = size.width) { scale(-1f, 1f, pivot = Offset.Zero) { drawPath(path, folds) } }
        } else {
            drawPath(path, folds)
        }
    }
    curtain(mirror = false)
    curtain(mirror = true)
    // Gold tie-back tassels
    val y = size.height * 0.7f
    drawCircle(ShadowInk.Gold, 4.dp.toPx(), Offset(w * 0.6f, y))
    drawCircle(ShadowInk.Gold, 4.dp.toPx(), Offset(size.width - w * 0.6f, y))
}

/**
 * The outline of a hand shadow: a rounded palm (no thumb notch: in silhouette it reads as a bite),
 * narrowing into a wrist and a forearm that runs off the bottom edge, as real hand shadows do.
 * Built with the thumb on the right for a left hand, mirrored otherwise.
 */
fun shadowHandPath(palmRect: Rect, isLeftHand: Boolean, bottom: Float): Path {
    val w = palmRect.width
    val h = palmRect.height
    val t = palmRect.top
    val base = t + h * 0.70f
    val wristY = base + (bottom - base) * 0.35f
    fun x(rel: Float) = if (isLeftHand) palmRect.left + rel * w else palmRect.left + (1f - rel) * w
    return Path().apply {
        moveTo(x(0.22f), bottom)
        lineTo(x(0.27f), wristY)
        quadraticTo(x(0.27f), base, x(0.1f), base - h * 0.14f)
        // Pinky edge up to the knuckles
        cubicTo(x(0.05f), t + h * 0.45f, x(0.08f), t + h * 0.12f, x(0.19f), t + h * 0.03f)
        // Knuckle arc
        cubicTo(x(0.42f), t - h * 0.06f, x(0.64f), t - h * 0.06f, x(0.85f), t + h * 0.05f)
        // Thumb side, with the fuller mound of the thumb
        cubicTo(x(0.99f), t + h * 0.16f, x(1.03f), t + h * 0.5f, x(0.93f), base - h * 0.1f)
        quadraticTo(x(0.73f), base, x(0.73f), wristY)
        lineTo(x(0.8f), bottom)
        close()
    }
}

/** A hand as a cut-paper silhouette with a lace cuff at the wrist and a crescent moon cut out of the palm. */
fun DrawScope.drawShadowPalm(palmRect: Rect, isLeftHand: Boolean, bottom: Float) {
    val base = palmRect.top + palmRect.height * 0.70f
    val cuffY = base + (bottom - base) * 0.6f
    // The forearm falls out of the lamplight below the cuff
    val fadeFrom = ((cuffY + 6.dp.toPx()) / bottom).coerceIn(0f, 0.99f)
    drawPath(
        shadowHandPath(palmRect, isLeftHand, bottom),
        Brush.verticalGradient(0f to ShadowInk.Ink, fadeFrom to ShadowInk.Ink, 1f to ShadowInk.Ink.copy(alpha = 0f), startY = 0f, endY = bottom)
    )

    // Lace cuff: a scalloped line and a row of pierced holes across the wrist
    val left = palmRect.left + palmRect.width * 0.35f
    val right = palmRect.left + palmRect.width * 0.65f
    val holes = 4
    val holeR = 2.2.dp.toPx()
    val scallop = Path()
    for (i in 0 until holes) {
        val x = left + (right - left) * i / (holes - 1)
        drawCircle(ShadowInk.Light.copy(alpha = 0.9f), holeR, Offset(x, cuffY))
        if (i < holes - 1) {
            val next = left + (right - left) * (i + 1) / (holes - 1)
            scallop.moveTo(x, cuffY - 6.dp.toPx())
            scallop.quadraticTo((x + next) / 2, cuffY - 11.dp.toPx(), next, cuffY - 6.dp.toPx())
        }
    }
    drawPath(scallop, ShadowInk.Light.copy(alpha = 0.55f), style = Stroke(1.2.dp.toPx()))

    // Crescent moon and a star, pierced through the palm
    val moonCenter = Offset(
        palmRect.left + palmRect.width * (if (isLeftHand) 0.42f else 0.58f),
        palmRect.top + palmRect.height * 0.42f
    )
    drawPath(crescent(moonCenter, 8.dp.toPx(), if (isLeftHand) 1f else -1f), ShadowInk.Light.copy(alpha = 0.95f))
    drawPath(
        fourPointStar(moonCenter + Offset((if (isLeftHand) 14 else -14).dp.toPx(), (-7).dp.toPx()), 3.5.dp.toPx()),
        ShadowInk.Light.copy(alpha = 0.95f)
    )
}

/** A knocked-out hand: the faintest ghost of the hand and a curl of smoke where the shadow was. */
fun DrawScope.drawShadowGhost(palmRect: Rect, isLeftHand: Boolean, bottom: Float, phase: Float) {
    drawPath(shadowHandPath(palmRect, isLeftHand, bottom), ShadowInk.Ink.copy(alpha = 0.12f))
    val x = palmRect.center.x
    val bottom = palmRect.top + palmRect.height * 0.3f
    val drift = sin(phase * 6.28f) * 6.dp.toPx()
    val smoke = Path().apply {
        moveTo(x, bottom)
        cubicTo(x - 14.dp.toPx() + drift, bottom - 30.dp.toPx(), x + 16.dp.toPx() - drift, bottom - 55.dp.toPx(), x + drift, bottom - 85.dp.toPx())
        cubicTo(x - 10.dp.toPx(), bottom - 100.dp.toPx(), x + 6.dp.toPx(), bottom - 110.dp.toPx(), x - 4.dp.toPx() + drift, bottom - 125.dp.toPx())
    }
    drawPath(smoke, ShadowInk.Ink.copy(alpha = 0.28f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
}

/** A swallow seen from below, flying toward [heading] (radians). [wingPhase] cycles the wingbeat. */
fun DrawScope.drawSilhouetteSwallow(center: Offset, heading: Float, wingPhase: Float, size: Float, color: Color = ShadowInk.Ink) {
    val s = size
    val flap = abs(cos(wingPhase * 6.2832f))
    val span = s * (0.35f + 0.6f * flap)
    val body = Path().apply {
        moveTo(s * 0.6f, 0f)
        quadraticTo(s * 0.2f, -s * 0.17f, -s * 0.3f, -s * 0.06f)
        lineTo(-s * 0.8f, -s * 0.3f)  // forked tail
        lineTo(-s * 0.5f, 0f)
        lineTo(-s * 0.8f, s * 0.3f)
        lineTo(-s * 0.3f, s * 0.06f)
        quadraticTo(s * 0.2f, s * 0.17f, s * 0.6f, 0f)
        close()
    }
    fun wing(side: Float) = Path().apply {
        moveTo(s * 0.2f, 0f)
        quadraticTo(-s * 0.05f, side * span * 0.6f, -s * 0.38f, side * span)
        quadraticTo(-s * 0.02f, side * span * 0.35f, -s * 0.12f, 0f)
        close()
    }
    translate(center.x, center.y) {
        rotate(Math.toDegrees(heading.toDouble()).toFloat(), pivot = Offset.Zero) {
            drawPath(body, color)
            drawPath(wing(-1f), color)
            drawPath(wing(1f), color)
        }
    }
}

/** A move: a swallow flying an arc from [start] to [end], leaving a dotted trail. */
fun DrawScope.drawShadowMovePath(start: Offset, end: Offset, progress: Float) {
    val dir = end - start
    val len = dir.getDistance()
    if (len <= 0f) return
    val ortho = Offset(-dir.y / len, dir.x / len)
    val control = start + dir * 0.5f + ortho * 70.dp.toPx()
    fun point(t: Float) = start * ((1 - t) * (1 - t)) + control * (2 * (1 - t) * t) + end * (t * t)

    val dots = 16
    for (i in 0..(dots * progress).toInt()) {
        drawCircle(ShadowInk.Ink.copy(alpha = 0.35f), 2.5.dp.toPx(), point(i.toFloat() / dots))
    }
    val t = progress.coerceIn(0f, 1f)
    val p = point(t)
    val tangent = (control - start) * (2 * (1 - t)) + (end - control) * (2 * t)
    drawSilhouetteSwallow(p, atan2(tangent.y, tangent.x), wingPhase = progress * 4f, size = 30.dp.toPx())
}

/** Main-menu emblem: two ink chopsticks crossed in front of a crescent moon. */
fun DrawScope.drawShadowChopsticks(scale: Float) {
    val c = center
    // The moon rises above the title card, between the sticks
    drawPath(crescent(c + Offset(0f, (-78).dp.toPx() * scale), 26.dp.toPx() * scale, 1f), ShadowInk.LampCore)
    drawPath(crescent(c + Offset(0f, (-78).dp.toPx() * scale), 26.dp.toPx() * scale, 1f), ShadowInk.Ink.copy(alpha = 0.5f), style = Stroke(1.5.dp.toPx()))
    listOf(-40f, 48f).forEach { angle ->
        rotate(angle, pivot = c) {
            val stick = Path().apply {
                val l = 108.dp.toPx() * scale
                moveTo(c.x - l, c.y - 5.dp.toPx() * scale)
                lineTo(c.x + l, c.y - 2.dp.toPx() * scale)
                lineTo(c.x + l, c.y + 2.dp.toPx() * scale)
                lineTo(c.x - l, c.y + 5.dp.toPx() * scale)
                close()
            }
            drawPath(stick, ShadowInk.Ink)
        }
    }
}

/** Main-menu backdrop: the lit screen with Montmartre rooftops at three depths and the black cat. */
@Composable
fun ShadowTheatreBackground(modifier: Modifier = Modifier) {
    val flicker = rememberCandleFlicker()
    val tail = rememberInfiniteTransition(label = "cat").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tail"
    )
    Canvas(modifier.fillMaxSize()) {
        drawLampLitScreen(Offset(size.width * 0.5f, size.height * 0.28f), flicker.value)
        drawRooftops(baseline = size.height * 0.93f, height = size.height * 0.12f, seed = 3, color = ShadowInk.Dusk.copy(alpha = 0.28f))
        drawRooftops(baseline = size.height * 0.96f, height = size.height * 0.10f, seed = 11, color = ShadowInk.Ink.copy(alpha = 0.55f))
        val catRoof = drawRooftops(baseline = size.height * 1.0f, height = size.height * 0.08f, seed = 29, color = ShadowInk.Ink)
        drawSittingCat(Offset(size.width * 0.74f, catRoof(size.width * 0.74f)), 22.dp.toPx(), tail.value)
    }
}

/**
 * A row of roofs with chimneys and the odd pitched gable, seeded so it never changes between frames.
 * Returns the roof height at a given x so things can sit on it.
 */
private fun DrawScope.drawRooftops(baseline: Float, height: Float, seed: Int, color: Color): (Float) -> Float {
    val r = java.util.Random(seed.toLong())
    val path = Path()
    val tops = mutableListOf<Pair<ClosedFloatingPointRange<Float>, Float>>()
    path.moveTo(0f, size.height)
    var x = 0f
    while (x < size.width) {
        val w = (28 + r.nextInt(44)).dp.toPx()
        val top = baseline - height * (0.45f + r.nextFloat() * 0.55f)
        path.lineTo(x, top)
        if (r.nextInt(3) == 0) {
            path.lineTo(x + w / 2, top - height * 0.25f) // gable
        } else if (r.nextInt(2) == 0) {
            val cx = x + w * (0.2f + r.nextFloat() * 0.5f)
            path.lineTo(cx, top)
            path.lineTo(cx, top - 10.dp.toPx())
            path.lineTo(cx + 7.dp.toPx(), top - 10.dp.toPx())
            path.lineTo(cx + 7.dp.toPx(), top)
        }
        path.lineTo(x + w, top)
        tops += (x..(x + w)) to top
        x += w
    }
    path.lineTo(size.width, size.height)
    path.close()
    drawPath(path, color)
    return { px -> tops.firstOrNull { px in it.first }?.second ?: baseline }
}

private fun DrawScope.drawSittingCat(feet: Offset, s: Float, tailPhase: Float) {
    val ink = ShadowInk.Ink
    // Body
    drawOval(ink, topLeft = Offset(feet.x - s * 0.55f, feet.y - s * 1.3f), size = Size(s * 1.1f, s * 1.35f))
    // Head and ears
    val head = Offset(feet.x + s * 0.05f, feet.y - s * 1.45f)
    drawCircle(ink, s * 0.42f, head)
    val ears = Path().apply {
        moveTo(head.x - s * 0.38f, head.y - s * 0.1f); lineTo(head.x - s * 0.3f, head.y - s * 0.65f); lineTo(head.x - s * 0.05f, head.y - s * 0.3f); close()
        moveTo(head.x + s * 0.38f, head.y - s * 0.1f); lineTo(head.x + s * 0.3f, head.y - s * 0.65f); lineTo(head.x + s * 0.05f, head.y - s * 0.3f); close()
    }
    drawPath(ears, ink)
    // Eyes catching the lamp
    drawCircle(ShadowInk.LampWarm, s * 0.06f, head + Offset(-s * 0.15f, -s * 0.02f))
    drawCircle(ShadowInk.LampWarm, s * 0.06f, head + Offset(s * 0.15f, -s * 0.02f))
    // Tail curling off the roof edge
    val sway = (tailPhase - 0.5f) * s * 0.8f
    val tail = Path().apply {
        moveTo(feet.x + s * 0.45f, feet.y - s * 0.15f)
        cubicTo(feet.x + s * 1.2f, feet.y, feet.x + s * 1.1f + sway, feet.y + s * 0.9f, feet.x + s * 1.5f + sway, feet.y + s * 1.2f)
    }
    drawPath(tail, ink, style = Stroke(s * 0.16f, cap = StrokeCap.Round))
}

/** A button styled as a theatre ticket stub: notched ends, a perforation line, crimson serif type. */
@Composable
fun ShadowTicketButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, alpha: Float) {
    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { this.alpha = alpha }
            .drawBehind {
                val notch = 7.dp.toPx()
                val ticket = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height / 2 - notch)
                    arcTo(Rect(Offset(size.width, size.height / 2), notch), 270f, -180f, false)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    lineTo(0f, size.height / 2 + notch)
                    arcTo(Rect(Offset(0f, size.height / 2), notch), 90f, -180f, false)
                    close()
                }
                drawPath(ticket, ShadowInk.Parchment)
                drawPath(ticket, ShadowInk.Curtain, style = Stroke(1.5.dp.toPx()))
                val stubX = 14.dp.toPx()
                drawLine(
                    ShadowInk.Curtain.copy(alpha = 0.6f),
                    Offset(stubX, 5.dp.toPx()), Offset(stubX, size.height - 5.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                )
            }
            .clickable(enabled = enabled) { onClick() }
            .padding(start = 24.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = if (enabled) ShadowInk.Curtain else ShadowInk.Curtain.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

/** Game over as a silent-film intertitle: "Fin." in a double-ruled frame, then the result. */
@Composable
fun ShadowIntertitle(result: String, subtitle: String, button: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .drawBehind {
                drawRect(ShadowInk.Ink)
                val outer = 6.dp.toPx()
                val inner = 12.dp.toPx()
                drawRect(ShadowInk.Parchment, Offset(outer, outer), Size(size.width - 2 * outer, size.height - 2 * outer), style = Stroke(2.dp.toPx()))
                drawRect(ShadowInk.Parchment.copy(alpha = 0.6f), Offset(inner, inner), Size(size.width - 2 * inner, size.height - 2 * inner), style = Stroke(1.dp.toPx()))
                listOf(Offset(inner, inner), Offset(size.width - inner, inner), Offset(inner, size.height - inner), Offset(size.width - inner, size.height - inner)).forEach {
                    drawPath(fourPointStar(it, 6.dp.toPx()), ShadowInk.Gold)
                }
            }
            .padding(horizontal = 36.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Fin.", color = ShadowInk.Parchment, fontSize = 56.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
        Text(
            result,
            color = ShadowInk.LampWarm,
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            subtitle,
            color = ShadowInk.Parchment.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )
        button()
    }
}

/** A crescent: a disc with an offset disc removed. [facing] 1 opens to the right, -1 to the left. */
private fun crescent(center: Offset, radius: Float, facing: Float): Path {
    val disc = Path().apply { addOval(Rect(center, radius)) }
    val bite = Path().apply { addOval(Rect(center + Offset(facing * radius * 0.45f, -radius * 0.2f), radius * 0.85f)) }
    return Path.combine(PathOperation.Difference, disc, bite)
}

private fun fourPointStar(center: Offset, r: Float): Path = Path().apply {
    val k = r * 0.28f
    moveTo(center.x, center.y - r)
    lineTo(center.x + k, center.y - k)
    lineTo(center.x + r, center.y)
    lineTo(center.x + k, center.y + k)
    lineTo(center.x, center.y + r)
    lineTo(center.x - k, center.y + k)
    lineTo(center.x - r, center.y)
    lineTo(center.x - k, center.y - k)
    close()
}
