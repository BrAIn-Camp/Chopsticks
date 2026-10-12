package com.example.chopsticks.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/*
 * Marginalia: the board as a page of a medieval manuscript.
 *
 * Hands are drawn like the finger-reckoning diagrams of Bede and Pacioli (ink contour, pen hatching,
 * a lapis sleeve) with their count written in red as a medieval numeral. The turn is shown by a
 * manicule, the little pointing hand readers drew in margins for six centuries. Moves are broad-nib
 * pen flourishes; a knocked-out hand is struck through and marked "vacat" as a scribe cancelled text.
 * The menu's lower margin has the knight and the snail, the most famous drollery of all.
 */
object Folio {
    val Vellum = Color(0xFFEFE2C4)
    val VellumLight = Color(0xFFF7EEDA)
    val VellumAged = Color(0xFFC9AE7E)
    val Ink = Color(0xFF2B1C12)          // iron-gall
    val Vermilion = Color(0xFFB8321A)
    val Lapis = Color(0xFF22408F)        // ultramarine
    val Verdigris = Color(0xFF3D7A58)
    val Gold = Color(0xFFC9A13B)
    val GoldLight = Color(0xFFF0D77E)
}

/** Medieval lower-case numerals, with the final i written as j, and iiij rather than iv. */
fun medievalNumeral(n: Int): String = when (n) {
    1 -> "i"; 2 -> "ij"; 3 -> "iij"; 4 -> "iiij"; 5 -> "v"
    6 -> "vj"; 7 -> "vij"; 8 -> "viij"; 9 -> "ix"; 10 -> "x"
    else -> n.toString()
}

/** The page: aged vellum, faint foxing, red ruling lines and the ruled text block. */
fun DrawScope.drawManuscriptPage() {
    drawRect(Folio.Vellum)
    drawRect(
        Brush.radialGradient(
            0.5f to Color.Transparent,
            1f to Folio.VellumAged.copy(alpha = 0.7f),
            center = center,
            radius = size.maxDimension * 0.75f
        )
    )
    // Foxing: age spots, seeded so they stay put
    val r = java.util.Random(1494)
    repeat(28) {
        drawCircle(
            Folio.VellumAged.copy(alpha = 0.10f + r.nextFloat() * 0.12f),
            (1 + r.nextInt(5)).dp.toPx(),
            Offset(r.nextFloat() * size.width, r.nextFloat() * size.height)
        )
    }
    // Ruling: horizontal lines in faded red, and the double vertical bounding lines of the text block
    val ruling = Folio.Vermilion.copy(alpha = 0.10f)
    var y = 30.dp.toPx()
    while (y < size.height) {
        drawLine(ruling, Offset(0f, y), Offset(size.width, y), 0.8.dp.toPx())
        y += 26.dp.toPx()
    }
    val bound = Folio.Vermilion.copy(alpha = 0.22f)
    listOf(20.dp.toPx(), 24.dp.toPx(), size.width - 24.dp.toPx(), size.width - 20.dp.toPx()).forEach { x ->
        drawLine(bound, Offset(x, 0f), Offset(x, size.height), 0.8.dp.toPx())
    }
    // Pricking holes down the outer edge, used to guide the ruling
    var py = 30.dp.toPx()
    while (py < size.height) {
        drawCircle(Folio.Ink.copy(alpha = 0.25f), 1.dp.toPx(), Offset(size.width - 6.dp.toPx(), py))
        py += 26.dp.toPx()
    }
}

/** An ivy-leaf vine down a margin, leaves alternating lapis, vermilion and gold. */
fun DrawScope.drawIvyBorder(x: Float, top: Float, bottom: Float) {
    val amp = 5.dp.toPx()
    val stem = Path()
    val steps = 80
    for (i in 0..steps) {
        val y = top + (bottom - top) * i / steps
        val px = x + sin(i * 0.45f) * amp
        if (i == 0) stem.moveTo(px, y) else stem.lineTo(px, y)
    }
    drawPath(stem, Folio.Ink.copy(alpha = 0.7f), style = Stroke(1.2.dp.toPx()))
    val colors = listOf(Folio.Lapis, Folio.Vermilion, Folio.Gold)
    var i = 3
    var k = 0
    while (i < steps) {
        val y = top + (bottom - top) * i / steps
        val px = x + sin(i * 0.45f) * amp
        val side = if (k % 2 == 0) 1f else -1f
        drawIvyLeaf(Offset(px + side * 5.dp.toPx(), y), 4.5.dp.toPx(), colors[k % 3], side)
        i += 4
        k++
    }
}

private fun DrawScope.drawIvyLeaf(c: Offset, s: Float, color: Color, side: Float) {
    val leaf = Path().apply {
        moveTo(c.x - side * s, c.y)
        quadraticTo(c.x, c.y - s * 1.3f, c.x + side * s, c.y - s * 0.2f)
        quadraticTo(c.x + side * s * 0.4f, c.y + s * 0.2f, c.x + side * s * 0.9f, c.y + s * 0.8f)
        quadraticTo(c.x, c.y + s * 1.1f, c.x - side * s, c.y)
        close()
    }
    drawPath(leaf, color)
    drawPath(leaf, Folio.Ink.copy(alpha = 0.7f), style = Stroke(0.8.dp.toPx()))
}

/**
 * A manicule: a cuffed hand with the index finger extended, its fingertip at [tip],
 * pointing in [angleDegrees] (0 = right, 90 = down).
 */
fun DrawScope.drawManicule(tip: Offset, angleDegrees: Float, scale: Float = 1f) {
    val u = 1.dp.toPx() * scale
    val outline = Stroke(1.4f * u)
    translate(tip.x, tip.y) {
        rotate(angleDegrees, pivot = Offset.Zero) {
            // Sleeve and cuff
            val sleeve = Rect(Offset(-64 * u, -12 * u), Size(18 * u, 24 * u))
            drawRect(Folio.Lapis, sleeve.topLeft, sleeve.size)
            drawRect(Folio.Ink, sleeve.topLeft, sleeve.size, style = outline)
            drawLine(Folio.Gold, Offset(-48 * u, -12 * u), Offset(-48 * u, 12 * u), 3 * u)
            // Fist
            val fist = Rect(Offset(-47 * u, -10 * u), Size(30 * u, 22 * u))
            drawRoundRect(Folio.VellumLight, fist.topLeft, fist.size, CornerRadius(8 * u))
            drawRoundRect(Folio.Ink, fist.topLeft, fist.size, CornerRadius(8 * u), style = outline)
            // Curled fingers
            for (k in 0..2) {
                val fy = -1 * u + k * 4.5f * u
                drawLine(Folio.Ink, Offset(-24 * u, fy), Offset(-18 * u, fy), 1.2f * u)
            }
            // Thumb along the top
            drawRoundRect(Folio.VellumLight, Offset(-36 * u, -15 * u), Size(16 * u, 7 * u), CornerRadius(3.5f * u))
            drawRoundRect(Folio.Ink, Offset(-36 * u, -15 * u), Size(16 * u, 7 * u), CornerRadius(3.5f * u), style = outline)
            // Index finger, pointing
            drawRoundRect(Folio.VellumLight, Offset(-24 * u, -10 * u), Size(24 * u, 8 * u), CornerRadius(4 * u))
            drawRoundRect(Folio.Ink, Offset(-24 * u, -10 * u), Size(24 * u, 8 * u), CornerRadius(4 * u), style = outline)
            drawLine(Folio.Ink.copy(alpha = 0.6f), Offset(-6 * u, -8 * u), Offset(-6 * u, -4 * u), 0.8f * u) // nail
        }
    }
}

/**
 * A hand in the manner of a finger-reckoning diagram: vellum, ink contour, pen hatching on the
 * shadow side, a lapis sleeve with a gold cuff, and its count written in red on the palm.
 */
fun DrawScope.drawManuscriptHand(
    palmRect: Rect,
    isLeftHand: Boolean,
    bottom: Float,
    fingers: Int,
    textMeasurer: TextMeasurer
) {
    val hand = shadowHandPath(palmRect, isLeftHand, bottom)
    val base = palmRect.top + palmRect.height * 0.70f
    val wristY = base + (bottom - base) * 0.35f

    drawPath(hand, Folio.VellumLight)
    clipPath(hand) {
        // Hatching along the pinky edge, the side away from the light
        val edgeX = if (isLeftHand) palmRect.left else palmRect.right
        val dir = if (isLeftHand) 1f else -1f
        var y = palmRect.top + palmRect.height * 0.1f
        while (y < base) {
            drawLine(
                Folio.Ink.copy(alpha = 0.45f),
                Offset(edgeX + dir * palmRect.width * 0.06f, y + 6.dp.toPx()),
                Offset(edgeX + dir * palmRect.width * 0.2f, y),
                0.9.dp.toPx()
            )
            y += 5.dp.toPx()
        }
        // Sleeve and gold cuff
        drawRect(Folio.Lapis, Offset(palmRect.left - 20f, wristY), Size(palmRect.width + 40f, bottom - wristY))
        drawLine(Folio.Gold, Offset(palmRect.left - 20f, wristY), Offset(palmRect.right + 20f, wristY), 4.dp.toPx())
        var dx = palmRect.left
        while (dx < palmRect.right) {
            drawCircle(Folio.GoldLight, 1.4.dp.toPx(), Offset(dx, wristY + 8.dp.toPx()))
            dx += 7.dp.toPx()
        }
    }
    drawPath(hand, Folio.Ink, style = Stroke(1.8.dp.toPx(), join = StrokeJoin.Round))

    // Palm creases
    val creases = Path().apply {
        fun x(rel: Float) = if (isLeftHand) palmRect.left + rel * palmRect.width else palmRect.right - rel * palmRect.width
        moveTo(x(0.2f), palmRect.top + palmRect.height * 0.2f)
        quadraticTo(x(0.5f), palmRect.top + palmRect.height * 0.12f, x(0.8f), palmRect.top + palmRect.height * 0.22f)
        moveTo(x(0.75f), palmRect.top + palmRect.height * 0.3f)
        quadraticTo(x(0.6f), palmRect.top + palmRect.height * 0.5f, x(0.7f), base - palmRect.height * 0.05f)
    }
    drawPath(creases, Folio.Ink.copy(alpha = 0.35f), style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))

    // The count, rubricated
    if (fingers > 0) {
        val label = textMeasurer.measure(
            medievalNumeral(fingers),
            TextStyle(color = Folio.Vermilion, fontSize = 22.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
        )
        drawText(
            label,
            topLeft = Offset(
                palmRect.center.x - label.size.width / 2f,
                palmRect.top + palmRect.height * 0.42f - label.size.height / 2f
            )
        )
    }
}

/** A finger segment: vellum, ink outline (the joints read as knuckle creases), hatching on one side. */
fun DrawScope.drawManuscriptFingerSegment(segment: Path, segRect: Rect) {
    drawPath(segment, Folio.VellumLight)
    clipPath(segment) {
        var y = segRect.top + 3.dp.toPx()
        while (y < segRect.bottom) {
            drawLine(
                Folio.Ink.copy(alpha = 0.4f),
                Offset(segRect.right - segRect.width * 0.3f, y + 4.dp.toPx()),
                Offset(segRect.right, y),
                0.8.dp.toPx()
            )
            y += 4.5.dp.toPx()
        }
    }
    drawPath(segment, Folio.Ink, style = Stroke(1.6.dp.toPx(), join = StrokeJoin.Round))
}

/** A knocked-out hand, scraped back like a palimpsest, struck through in red and marked "vacat". */
fun DrawScope.drawManuscriptVacat(palmRect: Rect, isLeftHand: Boolean, bottom: Float, textMeasurer: TextMeasurer) {
    val hand = shadowHandPath(palmRect, isLeftHand, bottom)
    drawPath(hand, Folio.Ink.copy(alpha = 0.18f), style = Stroke(1.5.dp.toPx()))
    val y = palmRect.top + palmRect.height * 0.4f
    drawLine(Folio.Vermilion, Offset(palmRect.left - 6.dp.toPx(), y + 10.dp.toPx()), Offset(palmRect.right + 6.dp.toPx(), y - 10.dp.toPx()), 2.dp.toPx(), cap = StrokeCap.Round)
    val label = textMeasurer.measure(
        "vacat",
        TextStyle(color = Folio.Vermilion, fontSize = 18.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
    )
    drawText(label, topLeft = Offset(palmRect.center.x - label.size.width / 2f, y + 16.dp.toPx()))
}

/**
 * A move as a broad-nib pen flourish in vermilion: an S-curve whose stroke thickens and thins
 * with direction, as a calligraphy nib held at a fixed angle does. A gold dot rides the pen.
 */
fun DrawScope.drawPenFlourish(start: Offset, end: Offset, progress: Float) {
    val dir = end - start
    val len = dir.getDistance()
    if (len <= 0f) return
    val ortho = Offset(-dir.y / len, dir.x / len)
    val steps = 48
    // A bold S: the swing is large enough that the stroke turns through every direction
    fun point(t: Float) = start + dir * t + ortho * (sin(t * 2f * PI.toFloat()) * minOf(48.dp.toPx(), len * 0.18f))
    val nib = 35f * PI.toFloat() / 180f
    val last = (steps * progress.coerceIn(0f, 1f)).toInt()
    var prev = point(0f)
    for (i in 1..last) {
        val p = point(i.toFloat() / steps)
        val angle = atan2(p.y - prev.y, p.x - prev.x)
        // Hairline when moving along the nib, full width across it
        val across = abs(sin(angle - nib))
        val width = 1.dp.toPx() + 10.dp.toPx() * across * across
        drawLine(Folio.Vermilion, prev, p, width, cap = StrokeCap.Round)
        prev = p
    }
    drawCircle(Folio.Gold, 5.dp.toPx(), prev)
    drawCircle(Folio.Ink, 5.dp.toPx(), prev, style = Stroke(1.dp.toPx()))
}

/** Main-menu emblem: crossed chopsticks in lapis and vermilion with gilded tips and an ivy sprig. */
fun DrawScope.drawManuscriptChopsticks(scale: Float) {
    val c = center
    listOf(-40f to Folio.Lapis, 48f to Folio.Vermilion).forEach { (angle, color) ->
        rotate(angle, pivot = c) {
            val l = 108.dp.toPx() * scale
            val stick = Path().apply {
                moveTo(c.x - l, c.y - 5.dp.toPx() * scale)
                lineTo(c.x + l, c.y - 2.dp.toPx() * scale)
                lineTo(c.x + l, c.y + 2.dp.toPx() * scale)
                lineTo(c.x - l, c.y + 5.dp.toPx() * scale)
                close()
            }
            drawPath(stick, color)
            drawPath(stick, Folio.Ink, style = Stroke(1.2.dp.toPx()))
            drawRect(Folio.Gold, Offset(c.x + l - 22.dp.toPx() * scale, c.y - 2.5.dp.toPx() * scale), Size(22.dp.toPx() * scale, 5.dp.toPx() * scale))
        }
    }
    drawIvyLeaf(c + Offset(0f, (-74).dp.toPx() * scale), 9.dp.toPx() * scale, Folio.Verdigris, 1f)
    drawIvyLeaf(c + Offset(14.dp.toPx() * scale, (-66).dp.toPx() * scale), 7.dp.toPx() * scale, Folio.Gold, -1f)
}

/** Main-menu backdrop: the page, ivy borders, and the knight facing the snail in the lower margin. */
@Composable
fun ManuscriptBackground(modifier: Modifier = Modifier) {
    val creep = rememberInfiniteTransition(label = "snail").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart),
        label = "creep"
    )
    Canvas(modifier.fillMaxSize()) {
        drawManuscriptPage()
        drawIvyBorder(10.dp.toPx(), 40.dp.toPx(), size.height - 30.dp.toPx())
        drawIvyBorder(size.width - 10.dp.toPx(), 40.dp.toPx(), size.height - 30.dp.toPx())
        val ground = size.height * 0.95f
        drawLine(Folio.Ink.copy(alpha = 0.5f), Offset(size.width * 0.12f, ground), Offset(size.width * 0.88f, ground), 1.dp.toPx())
        drawSnail(Offset(size.width * (0.22f + 0.28f * creep.value), ground), 1f)
        drawKnight(Offset(size.width * 0.74f, ground), 1f)
    }
}

private fun DrawScope.drawSnail(foot: Offset, scale: Float) {
    val u = 1.dp.toPx() * scale
    // Body with horns, facing right
    val body = Path().apply {
        moveTo(foot.x - 20 * u, foot.y)
        lineTo(foot.x + 16 * u, foot.y)
        quadraticTo(foot.x + 24 * u, foot.y - 2 * u, foot.x + 22 * u, foot.y - 9 * u)
        quadraticTo(foot.x + 14 * u, foot.y - 8 * u, foot.x + 6 * u, foot.y - 5 * u)
        lineTo(foot.x - 20 * u, foot.y - 3 * u)
        close()
    }
    drawPath(body, Folio.VellumAged)
    drawPath(body, Folio.Ink, style = Stroke(1.2f * u))
    drawLine(Folio.Ink, Offset(foot.x + 19 * u, foot.y - 9 * u), Offset(foot.x + 24 * u, foot.y - 22 * u), 1.2f * u)
    drawLine(Folio.Ink, Offset(foot.x + 21 * u, foot.y - 8 * u), Offset(foot.x + 30 * u, foot.y - 18 * u), 1.2f * u)
    drawCircle(Folio.Ink, 1.6f * u, Offset(foot.x + 24 * u, foot.y - 22 * u))
    drawCircle(Folio.Ink, 1.6f * u, Offset(foot.x + 30 * u, foot.y - 18 * u))
    // Spiral shell
    val shellCenter = Offset(foot.x - 4 * u, foot.y - 15 * u)
    drawCircle(Folio.Verdigris, 13 * u, shellCenter)
    drawCircle(Folio.Ink, 13 * u, shellCenter, style = Stroke(1.2f * u))
    val spiral = Path()
    for (i in 0..60) {
        val a = i * 0.28f
        val r = 12 * u * (1f - i / 66f)
        val p = shellCenter + Offset(cos(a) * r, sin(a) * r)
        if (i == 0) spiral.moveTo(p.x, p.y) else spiral.lineTo(p.x, p.y)
    }
    drawPath(spiral, Folio.Ink, style = Stroke(1.1f * u))
}

private fun DrawScope.drawKnight(foot: Offset, scale: Float) {
    val u = 1.dp.toPx() * scale
    val ink = Stroke(1.3f * u)
    // Legs, braced
    drawLine(Folio.Ink, Offset(foot.x - 6 * u, foot.y - 26 * u), Offset(foot.x - 12 * u, foot.y), 3 * u)
    drawLine(Folio.Ink, Offset(foot.x + 6 * u, foot.y - 26 * u), Offset(foot.x + 10 * u, foot.y), 3 * u)
    // Body
    drawRoundRect(Folio.VellumAged, Offset(foot.x - 10 * u, foot.y - 52 * u), Size(20 * u, 28 * u), CornerRadius(4 * u))
    drawRoundRect(Folio.Ink, Offset(foot.x - 10 * u, foot.y - 52 * u), Size(20 * u, 28 * u), CornerRadius(4 * u), style = ink)
    // Great helm with eye slit
    drawRoundRect(Folio.VellumLight, Offset(foot.x - 8 * u, foot.y - 70 * u), Size(16 * u, 18 * u), CornerRadius(3 * u))
    drawRoundRect(Folio.Ink, Offset(foot.x - 8 * u, foot.y - 70 * u), Size(16 * u, 18 * u), CornerRadius(3 * u), style = ink)
    drawLine(Folio.Ink, Offset(foot.x - 7 * u, foot.y - 63 * u), Offset(foot.x + 1 * u, foot.y - 63 * u), 2 * u)
    // Lance couched toward the snail, with a pennant
    drawLine(Folio.Ink, Offset(foot.x + 4 * u, foot.y - 40 * u), Offset(foot.x - 70 * u, foot.y - 22 * u), 2 * u)
    val pennant = Path().apply {
        moveTo(foot.x - 50 * u, foot.y - 27 * u)
        lineTo(foot.x - 36 * u, foot.y - 38 * u)
        lineTo(foot.x - 40 * u, foot.y - 29 * u)
        close()
    }
    drawPath(pennant, Folio.Vermilion)
    // Kite shield
    val shield = Path().apply {
        moveTo(foot.x - 16 * u, foot.y - 50 * u)
        lineTo(foot.x - 2 * u, foot.y - 50 * u)
        quadraticTo(foot.x - 1 * u, foot.y - 34 * u, foot.x - 9 * u, foot.y - 22 * u)
        quadraticTo(foot.x - 17 * u, foot.y - 34 * u, foot.x - 16 * u, foot.y - 50 * u)
        close()
    }
    drawPath(shield, Folio.Lapis)
    drawPath(shield, Folio.Ink, style = ink)
    drawLine(Folio.Gold, Offset(foot.x - 15 * u, foot.y - 42 * u), Offset(foot.x - 9 * u, foot.y - 36 * u), 2 * u)
    drawLine(Folio.Gold, Offset(foot.x - 3 * u, foot.y - 42 * u), Offset(foot.x - 9 * u, foot.y - 36 * u), 2 * u)
}

/** A button as a rubricated label: a gold initial on a lapis square, the rest in ink capitals. */
@Composable
fun RubricButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, alpha: Float) {
    val word = text.uppercase()
    Row(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { this.alpha = alpha }
            .drawBehind {
                drawRect(Folio.VellumLight)
                drawRect(Folio.Ink, style = Stroke(1.dp.toPx()))
                val inset = 3.dp.toPx()
                drawRect(Folio.Vermilion.copy(alpha = 0.5f), Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset), style = Stroke(0.8.dp.toPx()))
            }
            .clickable(enabled = enabled) { onClick() }
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Folio.Lapis)
                .border(1.5.dp, Folio.Gold),
            contentAlignment = Alignment.Center
        ) {
            Text(word.take(1), color = Folio.GoldLight, fontSize = 20.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(2.dp))
        Text(
            word.drop(1),
            color = if (enabled) Folio.Ink else Folio.Ink.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

/** Game over as a colophon: "Explicit." in red, the result, and a closing line, framed in ivy. */
@Composable
fun ManuscriptColophon(result: String, subtitle: String, button: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .drawBehind {
                drawRect(Folio.Vellum)
                drawRect(Folio.Ink, style = Stroke(1.5.dp.toPx()))
                drawIvyBorder(10.dp.toPx(), 14.dp.toPx(), size.height - 14.dp.toPx())
                drawIvyBorder(size.width - 10.dp.toPx(), 14.dp.toPx(), size.height - 14.dp.toPx())
            }
            .padding(horizontal = 40.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Explicit.", color = Folio.Vermilion, fontSize = 48.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
        Text(
            result,
            color = Folio.Ink,
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            subtitle,
            color = Folio.Ink.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )
        button()
    }
}
