package com.example.chopsticks.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/*
 * Neon Rain: cyberpunk after its sources rather than its clichés.
 *
 * Blade Runner's layered megacity and low-key noir light (Syd Mead's "decent people don't live
 * below 60 stories"), Neuromancer's sky "the color of television, tuned to a dead channel" and its
 * vocabulary of jacking in and flatlining, Akira's red, and the angular netrunning interfaces of
 * Cyberpunk 2077. The palette is Akira red neon, sodium-amber streetlight and one cold hologram
 * cyan on near-black, not the stock pink-and-cyan synthwave.
 *
 * Hands are cybernetic prosthetics with a holographic wrist readout of the count. Targeting
 * brackets lock onto the active player; moves are right-angled netrun traces; a knocked-out hand
 * flatlines. A buzzing neon 箸 ("chopsticks") sign hangs over the rain.
 */
object NeonInk {
    val Night = Color(0xFF06070D)
    val Haze = Color(0xFF0E2A33)
    val Red = Color(0xFFE3131B)
    val RedGlow = Color(0xFFFF3B3F)
    val Cyan = Color(0xFF3CF2E5)
    val Amber = Color(0xFFFF9F1C)
    val Gunmetal = Color(0xFF262B36)
    val GunmetalDark = Color(0xFF12151C)
    val Chrome = Color(0xFF9AA4B5)
    val Text = Color(0xFFE8ECF2)
    val TowerFar = Color(0xFF0C111B)
    val TowerMid = Color(0xFF0A0E17)
    val TowerNear = Color(0xFF05070C)
}

/** A 0..1 loop for rain, static and blinking lights. */
@Composable
fun rememberNeonTime(): State<Float> =
    rememberInfiniteTransition(label = "neon_time").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

/** Whether a failing neon tube is lit at [time]: mostly on, with short buzzing dropouts. */
private fun neonLit(time: Float, seed: Int): Boolean {
    val t = (time * 20_000f / 90f).toInt()   // ~11 checks a second
    val r = java.util.Random((t * 31 + seed).toLong())
    return r.nextInt(23) != 0
}

/**
 * The megacity at night: dead-channel static sky, three layers of towers with lit windows and
 * aircraft lights, a buzzing 箸 sign, a passing spinner, wet street reflections, and rain.
 */
fun DrawScope.drawNeonCity(time: Float, textMeasurer: TextMeasurer) {
    val w = size.width
    val h = size.height

    // Sky: the colour of television tuned to a dead channel
    drawRect(Brush.verticalGradient(listOf(Color(0xFF151A24), NeonInk.Night), endY = h * 0.6f))
    val staticSeed = (time * 20_000f / 60f).toInt()
    val noise = java.util.Random(staticSeed.toLong())
    repeat(260) {
        val y = noise.nextFloat() * h * 0.55f
        drawRect(Color.White.copy(alpha = 0.04f + noise.nextFloat() * 0.06f), Offset(noise.nextFloat() * w, y), Size(2.dp.toPx(), 1.dp.toPx()))
    }
    // Low haze where the city light hits the smog
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, NeonInk.Haze.copy(alpha = 0.55f), Color.Transparent), startY = h * 0.25f, endY = h * 0.75f))

    // Three layers of towers
    fun towers(seed: Long, color: Color, minH: Float, maxH: Float, windowAlpha: Float, beacons: Boolean, windowOdds: Int) {
        val r = java.util.Random(seed)
        var x = -10f
        while (x < w) {
            val tw = (36 + r.nextInt(70)).dp.toPx()
            val th = h * (minH + r.nextFloat() * (maxH - minH))
            val top = h - th
            drawRect(color, Offset(x, top), Size(tw, th))
            // Lit windows: mostly amber sodium light, the odd cold cyan
            val cols = (tw / 7.dp.toPx()).toInt()
            val rows = (th / 9.dp.toPx()).toInt()
            for (cx in 0 until cols) for (cy in 0 until rows) {
                if (r.nextInt(windowOdds) == 0) {
                    val c = if (r.nextInt(6) == 0) NeonInk.Cyan else NeonInk.Amber
                    drawRect(c.copy(alpha = windowAlpha), Offset(x + 3.dp.toPx() + cx * 7.dp.toPx(), top + 6.dp.toPx() + cy * 9.dp.toPx()), Size(3.dp.toPx(), 4.dp.toPx()))
                }
            }
            if (beacons && r.nextInt(2) == 0) {
                val blink = ((time * 20_000f / 700f).toInt() + r.nextInt(3)) % 3 == 0
                drawLine(color, Offset(x + tw / 2, top), Offset(x + tw / 2, top - 18.dp.toPx()), 2.dp.toPx())
                if (blink) drawCircle(NeonInk.Red, 2.5.dp.toPx(), Offset(x + tw / 2, top - 18.dp.toPx()))
            }
            x += tw + r.nextInt(8).dp.toPx()
        }
    }
    towers(11, NeonInk.TowerFar, 0.32f, 0.6f, 0.22f, beacons = true, windowOdds = 22)

    // A spinner crossing the sky, tail lights trailing
    val sp = (time * 3f) % 1f
    val spinner = Offset(-60.dp.toPx() + sp * (w + 120.dp.toPx()), h * 0.22f + sin(sp * 6.28f) * 10.dp.toPx())
    drawRect(Color(0xFF1A1F2A), spinner - Offset(14.dp.toPx(), 3.dp.toPx()), Size(28.dp.toPx(), 7.dp.toPx()))
    drawLine(NeonInk.Red.copy(alpha = 0.6f), spinner - Offset(14.dp.toPx(), 0f), spinner - Offset(46.dp.toPx(), 0f), 2.dp.toPx())
    drawCircle(NeonInk.Cyan, 2.dp.toPx(), spinner + Offset(14.dp.toPx(), 0f))

    towers(23, NeonInk.TowerMid, 0.22f, 0.45f, 0.45f, beacons = true, windowOdds = 14)

    // The 箸 sign, a vertical neon tube on a bracket, buzzing
    val signRect = Rect(Offset(w * 0.74f, h * 0.36f), Size(54.dp.toPx(), 86.dp.toPx()))
    drawRect(Color(0xFF0B0C12), signRect.topLeft, signRect.size)
    drawRect(NeonInk.Red.copy(alpha = 0.5f), signRect.topLeft, signRect.size, style = Stroke(1.5.dp.toPx()))
    val lit = neonLit(time, 5)
    val glyphStyle = TextStyle(
        fontSize = 46.sp,
        color = if (lit) Color(0xFFFFD6D6) else Color(0xFF3A1214),
        shadow = if (lit) Shadow(NeonInk.RedGlow, Offset.Zero, 28f) else null
    )
    val glyph = textMeasurer.measure("箸", glyphStyle)
    drawText(glyph, topLeft = signRect.center - Offset(glyph.size.width / 2f, glyph.size.height / 2f))
    // The tube's light spilling onto the haze around it
    if (lit) drawCircle(Brush.radialGradient(listOf(NeonInk.Red.copy(alpha = 0.22f), Color.Transparent), center = signRect.center, radius = signRect.height * 1.1f), signRect.height * 1.1f, signRect.center)

    towers(37, NeonInk.TowerNear, 0.1f, 0.24f, 0.6f, beacons = false, windowOdds = 12)

    // Wet street: the neon smeared into reflections
    val street = h * 0.9f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0A0C12), NeonInk.Night), startY = street, endY = h), Offset(0f, street), Size(w, h - street))
    // Reflections smear into long vertical streaks on the wet surface
    fun streaks(x0: Float, width: Float, color: Color, seed: Long) {
        val r = java.util.Random(seed)
        repeat(9) {
            val x = x0 + r.nextFloat() * width
            val len = (h - street) * (0.4f + r.nextFloat() * 0.6f)
            drawLine(
                Brush.verticalGradient(listOf(color.copy(alpha = 0.15f + r.nextFloat() * 0.25f), Color.Transparent), startY = street, endY = street + len),
                Offset(x, street), Offset(x, street + len), (1 + r.nextInt(3)).dp.toPx()
            )
        }
    }
    if (lit) streaks(signRect.left, signRect.width, NeonInk.Red, 3)
    streaks(w * 0.12f, w * 0.2f, NeonInk.Amber, 4)
    streaks(w * 0.45f, w * 0.1f, NeonInk.Cyan, 5)
    // Rain, slanting, in front of everything
    val rain = java.util.Random(99)
    val fall = time * 40f
    repeat(140) {
        val x0 = rain.nextFloat() * (w + 100f)
        val speed = 0.7f + rain.nextFloat() * 0.6f
        val y = ((rain.nextFloat() + fall * speed) % 1f) * (h + 60f) - 30f
        val len = (14 + rain.nextInt(16)).dp.toPx()
        drawLine(Color(0xFFBFD6E6).copy(alpha = 0.10f + rain.nextFloat() * 0.12f), Offset(x0 - y * 0.12f, y), Offset(x0 - y * 0.12f - len * 0.25f, y + len), 1.dp.toPx())
    }
}

/**
 * Targeting brackets locked onto the active player's hands, a sweeping scan line, and a
 * small HUD label. [progress] (0..1) animates the lock-on.
 */
fun DrawScope.drawNeonTarget(area: Rect, progress: Float, time: Float, textMeasurer: TextMeasurer, label: String) {
    if (progress <= 0f) return
    val grow = 1f + (1f - progress) * 0.25f
    val r = Rect(area.center - Offset(area.width * grow / 2, area.height * grow / 2), Size(area.width * grow, area.height * grow))
    val arm = 22.dp.toPx()
    val c = NeonInk.Red.copy(alpha = progress)
    val sw = 2.dp.toPx()
    listOf(
        r.topLeft to Offset(1f, 1f), Offset(r.right, r.top) to Offset(-1f, 1f),
        Offset(r.left, r.bottom) to Offset(1f, -1f), r.bottomRight to Offset(-1f, -1f)
    ).forEach { (corner, d) ->
        drawLine(c, corner, corner + Offset(arm * d.x, 0f), sw)
        drawLine(c, corner, corner + Offset(0f, arm * d.y), sw)
        drawRect(NeonInk.Cyan.copy(alpha = progress), corner + Offset(6.dp.toPx() * d.x - 2f, 6.dp.toPx() * d.y - 2f), Size(4f, 4f))
    }
    // Scan line sweeping down through the area
    val scanY = r.top + ((time * 20_000f / 1800f) % 1f) * r.height
    drawLine(NeonInk.Cyan.copy(alpha = 0.25f * progress), Offset(r.left, scanY), Offset(r.right, scanY), 1.dp.toPx())
    val tag = textMeasurer.measure(label, TextStyle(color = NeonInk.Red.copy(alpha = progress), fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp))
    drawText(tag, topLeft = Offset(r.left + 4.dp.toPx(), r.top - tag.size.height - 2.dp.toPx()))
}

/**
 * A cybernetic hand: gunmetal plating with a chrome edge, panel seams and screws, glowing knuckle
 * joints, an optical sensor in the palm, wrist cables, and a holographic readout of the count.
 */
fun DrawScope.drawCyberHand(
    palmRect: Rect,
    isLeftHand: Boolean,
    bottom: Float,
    fingerBases: List<Offset>,
    fingerAngles: List<Float>,
    fingerLengths: List<Float>,
    fingerWidths: List<Float>,
    raised: List<Int>,
    fingers: Int,
    active: Boolean,
    pulse: Float,
    textMeasurer: TextMeasurer
) {
    val base = palmRect.top + palmRect.height * 0.70f
    val wristBottom = base + palmRect.height * 0.18f
    var hand = shadowHandPath(palmRect, isLeftHand, wristBottom)
    raised.forEach { i -> hand = Path.combine(PathOperation.Union, hand, capsule(fingerBases[i], fingerAngles[i], fingerWidths[i] * 0.95f, fingerLengths[i] * 0.92f)) }

    // Wrist cables trailing down out of frame
    val cableTop = wristBottom - 4.dp.toPx()
    listOf(-0.16f to NeonInk.GunmetalDark, 0f to Color(0xFF2E1418), 0.16f to NeonInk.GunmetalDark).forEachIndexed { i, (dx, c) ->
        val x = palmRect.center.x + palmRect.width * dx
        val cable = Path().apply {
            moveTo(x, cableTop)
            cubicTo(x, cableTop + 20.dp.toPx(), x + (i - 1) * 10.dp.toPx(), bottom - 10.dp.toPx(), x + (i - 1) * 16.dp.toPx(), bottom + 20.dp.toPx())
        }
        drawPath(cable, c, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(cable, NeonInk.Chrome.copy(alpha = 0.25f), style = Stroke(1.dp.toPx()))
    }

    // Red underglow when this hand is live
    if (active) {
        drawPath(hand, NeonInk.Red.copy(alpha = 0.10f), style = Stroke(18.dp.toPx(), join = StrokeJoin.Round))
        drawPath(hand, NeonInk.Red.copy(alpha = 0.18f), style = Stroke(7.dp.toPx(), join = StrokeJoin.Round))
    }

    // Plating
    drawPath(hand, Brush.linearGradient(listOf(NeonInk.Gunmetal, NeonInk.GunmetalDark), start = palmRect.topLeft, end = palmRect.bottomRight))
    clipPath(hand) {
        // Panel seams across the back of the hand, and screws at their ends
        val seamY = palmRect.top + palmRect.height * 0.38f
        drawLine(Color.Black.copy(alpha = 0.6f), Offset(palmRect.left, seamY), Offset(palmRect.right, seamY), 1.5.dp.toPx())
        drawLine(NeonInk.Chrome.copy(alpha = 0.18f), Offset(palmRect.left, seamY + 1.5.dp.toPx()), Offset(palmRect.right, seamY + 1.5.dp.toPx()), 1.dp.toPx())
        drawLine(Color.Black.copy(alpha = 0.6f), Offset(palmRect.center.x, seamY), Offset(palmRect.center.x, base), 1.5.dp.toPx())
        listOf(0.22f, 0.78f).forEach { rel -> drawCircle(NeonInk.Chrome.copy(alpha = 0.5f), 1.6.dp.toPx(), Offset(palmRect.left + palmRect.width * rel, seamY + 6.dp.toPx())) }
        // Wrist band
        drawRect(Color(0xFF0B0D12), Offset(palmRect.left - 20f, base), Size(palmRect.width + 40f, wristBottom - base + 40f))
        drawLine(NeonInk.Chrome.copy(alpha = 0.45f), Offset(palmRect.left - 20f, base), Offset(palmRect.right + 20f, base), 1.dp.toPx())
    }
    // Chrome edge, brighter toward the light
    drawPath(hand, Brush.linearGradient(listOf(Color(0xFFDDE3EC), NeonInk.Chrome.copy(alpha = 0.5f), Color(0xFF4A5160)), start = palmRect.topLeft, end = palmRect.bottomRight), style = Stroke(1.6.dp.toPx(), join = StrokeJoin.Round))

    // Knuckle joints: glowing rings across each raised finger
    val jointColor = if (active) NeonInk.Cyan else Color(0xFF3A4452)
    raised.forEach { i ->
        val a = fingerAngles[i] * PI.toFloat() / 180f
        val dir = Offset(sin(a), -cos(a))
        val across = Offset(cos(a), sin(a))
        val half = fingerWidths[i] * 0.42f
        listOf(0.33f, 0.62f).forEach { k ->
            val c = fingerBases[i] + dir * (fingerLengths[i] * 0.92f * k)
            drawLine(jointColor.copy(alpha = if (active) 0.85f else 0.6f), c - across * half, c + across * half, 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }

    // Optical sensor in the palm
    val eye = Offset(palmRect.center.x, palmRect.top + palmRect.height * 0.56f)
    drawCircle(Color(0xFF07080C), 9.dp.toPx(), eye)
    drawCircle(NeonInk.Chrome.copy(alpha = 0.6f), 9.dp.toPx(), eye, style = Stroke(1.dp.toPx()))
    drawCircle((if (active) NeonInk.Red else Color(0xFF4A1A1C)).copy(alpha = if (active) 0.6f + 0.4f * pulse else 1f), 4.dp.toPx(), eye)

    // Holographic wrist readout: [03]
    if (fingers > 0) {
        val text = "[" + fingers.toString().padStart(2, '0') + "]"
        val style = TextStyle(
            color = NeonInk.Cyan.copy(alpha = if (active) 1f else 0.6f),
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(NeonInk.Cyan, Offset.Zero, if (active) 14f else 4f)
        )
        val label = textMeasurer.measure(text, style)
        drawText(label, topLeft = Offset(palmRect.center.x - label.size.width / 2f, base + 2.dp.toPx()))
    }
}

/** A knocked-out hand, flatlined: a dashed red wireframe and a heart-monitor trace that goes flat. */
fun DrawScope.drawFlatline(palmRect: Rect, isLeftHand: Boolean, bottom: Float, time: Float, textMeasurer: TextMeasurer) {
    val hand = shadowHandPath(palmRect, isLeftHand, palmRect.top + palmRect.height * 0.88f)
    drawPath(hand, NeonInk.Red.copy(alpha = 0.45f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), time * 600f)))
    val y = palmRect.top + palmRect.height * 0.45f
    val l = palmRect.left - 10.dp.toPx()
    val rr = palmRect.right + 10.dp.toPx()
    val spike = l + (rr - l) * 0.3f
    val trace = Path().apply {
        moveTo(l, y)
        lineTo(spike, y)
        lineTo(spike + 6.dp.toPx(), y - 22.dp.toPx())
        lineTo(spike + 12.dp.toPx(), y + 14.dp.toPx())
        lineTo(spike + 18.dp.toPx(), y)
        lineTo(rr, y)
    }
    drawPath(trace, NeonInk.Red, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
    val label = textMeasurer.measure("FLATLINE", TextStyle(color = NeonInk.Red, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, shadow = Shadow(NeonInk.RedGlow, Offset.Zero, 10f)))
    drawText(label, topLeft = Offset(palmRect.center.x - label.size.width / 2f, y + 18.dp.toPx()))
}

/** A move as a netrun trace: right-angled data path, a bright packet at its head, a glitch burst on arrival. */
fun DrawScope.drawNetrunTrace(start: Offset, end: Offset, progress: Float) {
    val t = progress.coerceIn(0f, 1f)
    val midY = start.y + (end.y - start.y) * 0.5f
    val corners = listOf(start, Offset(start.x, midY), Offset(end.x, midY), end)
    val lengths = corners.zipWithNext { a, b -> (b - a).getDistance() }
    val total = lengths.sum().coerceAtLeast(1f)
    var remaining = total * t
    val path = Path().apply { moveTo(start.x, start.y) }
    var head = start
    for (i in lengths.indices) {
        val seg = lengths[i]
        if (remaining >= seg) {
            path.lineTo(corners[i + 1].x, corners[i + 1].y); head = corners[i + 1]; remaining -= seg
        } else {
            val k = if (seg > 0f) remaining / seg else 0f
            head = corners[i] + (corners[i + 1] - corners[i]) * k
            path.lineTo(head.x, head.y)
            break
        }
    }
    drawPath(path, NeonInk.Red.copy(alpha = 0.18f), style = Stroke(10.dp.toPx(), join = StrokeJoin.Miter))
    drawPath(path, NeonInk.Red, style = Stroke(2.5.dp.toPx(), join = StrokeJoin.Miter))
    drawRect(Color.White, head - Offset(4.dp.toPx(), 4.dp.toPx()), Size(8.dp.toPx(), 8.dp.toPx()))
    drawRect(NeonInk.Cyan, head - Offset(7.dp.toPx(), 7.dp.toPx()), Size(14.dp.toPx(), 14.dp.toPx()), style = Stroke(1.dp.toPx()))

    if (t > 0.78f) {
        val k = (t - 0.78f) / 0.22f
        val r = java.util.Random((t * 40).toLong())
        repeat(9) {
            val w = (12 + r.nextInt(40)).dp.toPx()
            val o = end + Offset((r.nextFloat() - 0.5f) * 90.dp.toPx(), (r.nextFloat() - 0.5f) * 70.dp.toPx())
            drawRect((if (r.nextBoolean()) NeonInk.Red else NeonInk.Cyan).copy(alpha = 0.55f * (1f - k * 0.6f)), o, Size(w, 3.dp.toPx()))
        }
    }
}

/** Main-menu emblem: two neon-tube chopsticks, red and cyan, crossing. */
fun DrawScope.drawNeonChopsticks(scale: Float, time: Float) {
    val c = center
    val l = 100.dp.toPx() * scale
    listOf(-40f to NeonInk.Red, 48f to NeonInk.Cyan).forEachIndexed { i, (angle, color) ->
        val lit = neonLit(time, 17 + i)
        rotate(angle, pivot = c) {
            val a = Offset(c.x - l, c.y); val b = Offset(c.x + l, c.y)
            if (lit) {
                drawLine(color.copy(alpha = 0.12f), a, b, 22.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color.copy(alpha = 0.3f), a, b, 10.dp.toPx(), cap = StrokeCap.Round)
            }
            drawLine(if (lit) Color.White.copy(alpha = 0.9f) else color.copy(alpha = 0.3f), a, b, 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color(0xFF2A2E38), a - Offset(6.dp.toPx(), 0f), a + Offset(6.dp.toPx(), 0f), 8.dp.toPx())   // tube cap
        }
    }
}

/** Main-menu backdrop: the city in the rain. */
@Composable
fun NeonCityBackground(modifier: Modifier = Modifier) {
    val time by rememberNeonTime()
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    Canvas(modifier.fillMaxSize()) { drawNeonCity(time, textMeasurer) }
}

/** The angular frame shared by buttons and panels: top-left and bottom-right corners cut away. */
fun chamferedRect(size: Size, cut: Float): Path = Path().apply {
    moveTo(cut, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height); lineTo(0f, size.height); lineTo(0f, cut); close()
}

/** A button as a chamfered HUD control: black glass, red edge, a cyan tick, spaced mono capitals. */
@Composable
fun NeonButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, alpha: Float) {
    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { this.alpha = alpha }
            .drawBehind {
                val frame = chamferedRect(size, 10.dp.toPx())
                drawPath(frame, Color(0xE60B0E15))
                drawPath(frame, (if (enabled) NeonInk.Red else NeonInk.Red.copy(alpha = 0.4f)), style = Stroke(1.5.dp.toPx()))
                drawRect(NeonInk.Cyan, Offset(size.width - 14.dp.toPx(), 4.dp.toPx()), Size(8.dp.toPx(), 2.dp.toPx()))
            }
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.uppercase(),
            color = if (enabled) NeonInk.Text else NeonInk.Text.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

/** Text split into red and cyan ghosts around white, jittering: chromatic aberration. */
@Composable
fun GlitchText(text: String, fontSize: TextUnit, color: Color = NeonInk.Text) {
    val jitter by rememberInfiniteTransition(label = "glitch").animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(90, easing = LinearEasing), RepeatMode.Reverse),
        label = "jitter"
    )
    Box {
        val style = TextStyle(fontSize = fontSize, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, letterSpacing = 2.sp)
        Text(text, style = style.copy(color = NeonInk.Red.copy(alpha = 0.8f)), modifier = Modifier.offset(x = (3 * jitter).dp))
        Text(text, style = style.copy(color = NeonInk.Cyan.copy(alpha = 0.7f)), modifier = Modifier.offset(x = (-3 * jitter).dp, y = (1 * jitter).dp))
        Text(text, style = style.copy(color = color))
    }
}

/** Game over: a glitching headline on a chamfered panel, the result, and a way back in. */
@Composable
fun NeonEndScreen(headline: String, result: String, subtitle: String, button: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .drawBehind {
                val frame = chamferedRect(size, 18.dp.toPx())
                drawPath(frame, Color(0xF00A0C12))
                drawPath(frame, NeonInk.Red, style = Stroke(2.dp.toPx()))
            }
            .padding(horizontal = 26.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlitchText(headline, 30.sp)
        Text(result, color = NeonInk.Cyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, modifier = Modifier.padding(top = 14.dp))
        Text(subtitle, color = NeonInk.Text.copy(alpha = 0.6f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
        button()
    }
}
