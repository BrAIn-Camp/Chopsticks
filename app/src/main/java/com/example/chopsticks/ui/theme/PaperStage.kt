package com.example.chopsticks.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/*
 * Paper Cutout, redesigned after the paper-theatre look of storybook RPGs like Paper Mario:
 * The Thousand-Year Door. Everything is a flat paper sprite: bold outline, a thick white paper
 * border, and a soft drop shadow on the stage floor. The game is staged in a little cardboard
 * theatre: a painted sky with clouds on strings, a wooden stage, spotlights that swing onto the
 * player whose turn it is, and an audience that hops when the turn changes.
 *
 * Hands are white cartoon gloves; their count is a round badge stuck on the back. When it becomes
 * your turn, your gloves do a paper flip. Moves fly as a paper airplane and land in a starburst.
 */
object PaperInk {
    val Outline = Color(0xFF2A2838)
    val PaperWhite = Color(0xFFFFFFFF)
    val GloveShade = Color(0xFFD5DCEA)
    val Red = Color(0xFFE2412F)
    val RedDark = Color(0xFFA82820)
    val Gold = Color(0xFFF6C443)
    val GoldDark = Color(0xFFC9921E)
    val SkyTop = Color(0xFF6EC3F2)
    val SkyLow = Color(0xFFC7EBFB)
    val Hill = Color(0xFF7CC66A)
    val Plank = Color(0xFFD9A066)
    val PlankDark = Color(0xFFC08650)
    val StageEdge = Color(0xFF8B5A2B)
    val House = Color(0xFF2E2440)
    val Shadow = Color(0x33000000)
    val AudienceColors = listOf(
        Color(0xFFE2412F), Color(0xFF4F8EF7), Color(0xFFF6C443),
        Color(0xFF6CCB5F), Color(0xFFB07CE8), Color(0xFFF28FB1)
    )
}

/** A slow 0..1..0 sway for clouds on strings. */
@Composable
fun rememberPaperSway(): State<Float> =
    rememberInfiniteTransition(label = "paper_sway").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway"
    )

/**
 * The cardboard theatre: sky, clouds on strings, hills, wooden stage, valance, spotlights aimed at
 * [spotlight] (the active player's hands), and the audience in the foreground. [hop] (0..1) makes
 * the audience jump.
 */
fun DrawScope.drawPaperStage(
    spotlight: Rect?,
    sway: Float,
    hop: Float,
    /** Where the clouds hang, as (x, y) fractions of the screen. */
    clouds: List<Pair<Float, Float>> = listOf(0.2f to 0.36f, 0.55f to 0.31f, 0.84f to 0.4f)
) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.5f

    // Painted sky backdrop
    drawRect(Brush.verticalGradient(listOf(PaperInk.SkyTop, PaperInk.SkyLow), endY = horizon), size = Size(w, horizon + 4f))

    // Cardboard hills along the horizon
    val hills = Path().apply {
        moveTo(0f, horizon)
        cubicTo(w * 0.15f, horizon - 60.dp.toPx(), w * 0.35f, horizon - 50.dp.toPx(), w * 0.5f, horizon - 10.dp.toPx())
        cubicTo(w * 0.62f, horizon - 70.dp.toPx(), w * 0.85f, horizon - 55.dp.toPx(), w, horizon - 15.dp.toPx())
        lineTo(w, horizon + 10f)
        lineTo(0f, horizon + 10f)
        close()
    }
    paperSprite(hills, PaperInk.Hill, border = 0f, shadow = false)

    // Clouds hanging on strings, swaying a little
    clouds.forEachIndexed { i, (cx, cy) ->
        val dx = sin((sway + i * 0.33f) * 2f * PI.toFloat()) * 5.dp.toPx()
        val center = Offset(w * cx + dx, h * cy)
        drawLine(PaperInk.Outline.copy(alpha = 0.5f), Offset(w * cx, 0f), center + Offset(0f, (-14).dp.toPx()), 1.dp.toPx())
        paperSprite(cloudPath(center, 18.dp.toPx() + i * 3.dp.toPx()), PaperInk.PaperWhite)
    }

    // Wooden stage floor
    val floorTop = horizon + 8f
    val stageFront = h - 92.dp.toPx()
    val board = 22.dp.toPx()
    var y = floorTop
    var row = 0
    while (y < stageFront) {
        drawRect(if (row % 2 == 0) PaperInk.Plank else PaperInk.PlankDark, Offset(0f, y), Size(w, board))
        drawLine(PaperInk.Outline.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), 1.dp.toPx())
        var x = if (row % 2 == 0) 40.dp.toPx() else 110.dp.toPx()
        while (x < w) {
            drawLine(PaperInk.Outline.copy(alpha = 0.25f), Offset(x, y), Offset(x, y + board), 1.dp.toPx())
            x += 150.dp.toPx()
        }
        y += board
        row++
    }
    // Stage front, footlights, and the dark house where the audience sits
    drawRect(PaperInk.StageEdge, Offset(0f, stageFront), Size(w, 12.dp.toPx()))
    drawLine(PaperInk.Outline, Offset(0f, stageFront), Offset(w, stageFront), 2.dp.toPx())
    drawRect(PaperInk.House, Offset(0f, stageFront + 12.dp.toPx()), Size(w, h))
    var fx = 30.dp.toPx()
    while (fx < w) {
        drawCircle(PaperInk.Gold, 5.dp.toPx(), Offset(fx, stageFront + 1.dp.toPx()))
        drawCircle(PaperInk.Outline, 5.dp.toPx(), Offset(fx, stageFront + 1.dp.toPx()), style = Stroke(1.2.dp.toPx()))
        fx += 64.dp.toPx()
    }

    // Spotlights from the top corners onto the active player
    spotlight?.let { target ->
        val pool = Rect(target.left + target.width * 0.1f, target.bottom - target.height * 0.25f, target.right - target.width * 0.1f, target.bottom + target.height * 0.1f)
        listOf(Offset(w * 0.12f, 20.dp.toPx()), Offset(w * 0.88f, 20.dp.toPx())).forEach { source ->
            val cone = Path().apply {
                moveTo(source.x - 10.dp.toPx(), source.y)
                lineTo(source.x + 10.dp.toPx(), source.y)
                lineTo(pool.right, pool.center.y)
                lineTo(pool.left, pool.center.y)
                close()
            }
            drawPath(cone, Brush.verticalGradient(listOf(Color(0x77FFF6D6), Color(0x33FFF6D6)), startY = source.y, endY = pool.center.y))
        }
        drawOval(Color(0x66FFF6D6), pool.topLeft, pool.size)
    }

    // Valance: red folds with a gold scalloped, fringed edge
    val valance = 16.dp.toPx() // kept slim so it clears the player label below it
    drawRect(Brush.horizontalGradient(listOf(PaperInk.RedDark, PaperInk.Red, PaperInk.RedDark, PaperInk.Red, PaperInk.RedDark), endX = 80.dp.toPx(), tileMode = TileMode.Repeated), size = Size(w, valance))
    val scallop = 18.dp.toPx()
    var sx = 0f
    while (sx < w) {
        val arc = Rect(Offset(sx, valance - scallop / 2), Size(scallop, scallop))
        drawArc(PaperInk.Gold, 0f, 180f, true, arc.topLeft, arc.size)
        drawArc(PaperInk.Outline, 0f, 180f, false, arc.topLeft, arc.size, style = Stroke(1.5.dp.toPx()))
        sx += scallop
    }

    // The audience: two rows of round heads in the foreground, hopping on cue
    val headR = 17.dp.toPx()
    val rows = listOf(h - 34.dp.toPx() to 0.8f, h - 8.dp.toPx() to 1f)
    rows.forEachIndexed { r, (baseY, scale) ->
        val count = 8
        for (i in 0 until count) {
            val cx = w * (i + 0.5f + (if (r == 0) 0.5f else 0f)) / count
            val jump = hop * 10.dp.toPx() * (0.5f + 0.5f * sin(i * 1.7f + r))
            val c = Offset(cx, baseY - jump)
            val color = PaperInk.AudienceColors[(i + r * 3) % PaperInk.AudienceColors.size]
            drawCircle(PaperInk.PaperWhite, headR * scale + 3.dp.toPx(), c)
            drawCircle(if (r == 0) lerp(color, PaperInk.House, 0.35f) else color, headR * scale, c)
            drawCircle(PaperInk.Outline, headR * scale, c, style = Stroke(1.5.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.35f), headR * scale * 0.3f, c + Offset(-headR * 0.35f, -headR * 0.35f) * scale)
        }
    }
}

/**
 * A flat paper sprite: drop shadow, thick white paper border, fill, then a bold outline.
 * [border] is the white border width in px (0 for none).
 */
fun DrawScope.paperSprite(path: Path, fill: Color, border: Float = 7.dp.toPx(), shadow: Boolean = true) {
    if (shadow) translate(4.dp.toPx(), 5.dp.toPx()) { drawPath(path, PaperInk.Shadow) }
    if (border > 0f) drawPath(path, PaperInk.PaperWhite, style = Stroke(border, join = StrokeJoin.Round))
    drawPath(path, fill)
    drawPath(path, PaperInk.Outline, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
}

private fun cloudPath(c: Offset, r: Float): Path {
    fun disc(dx: Float, dy: Float, rr: Float) = Path().apply { addOval(Rect(c + Offset(dx, dy), rr)) }
    var p = disc(0f, 0f, r)
    p = Path.combine(PathOperation.Union, p, disc(-r * 1.1f, r * 0.3f, r * 0.75f))
    p = Path.combine(PathOperation.Union, p, disc(r * 1.1f, r * 0.3f, r * 0.8f))
    p = Path.combine(PathOperation.Union, p, Path().apply { addRect(Rect(c.x - r * 1.6f, c.y + r * 0.2f, c.x + r * 1.7f, c.y + r * 0.95f)) })
    return p
}

/** A rounded finger: a capsule from [base] (slightly into the palm) out along [angleDeg]. */
internal fun capsule(base: Offset, angleDeg: Float, width: Float, length: Float): Path {
    val a = angleDeg * PI.toFloat() / 180f
    val r = width / 2f
    val pts = mutableListOf<Offset>()
    // Tip semicircle, then the base semicircle, in local coords (up is -y)
    for (i in 0..12) {
        val t = PI.toFloat() + PI.toFloat() * i / 12f
        pts += Offset(cos(t) * r, -length + r + sin(t) * r)
    }
    for (i in 0..12) {
        val t = PI.toFloat() * i / 12f
        pts += Offset(cos(t) * r, r * 0.6f + sin(t) * r)
    }
    return Path().apply {
        pts.forEachIndexed { i, p ->
            val x = base.x + p.x * cos(a) - p.y * sin(a)
            val y = base.y + p.x * sin(a) + p.y * cos(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/**
 * A white cartoon glove as one paper sprite: palm and the raised fingers merged into a single
 * outline, cel shading on the lower-right edge, a rolled cuff, stitching on the back, and the
 * count as a round badge stuck on it.
 */
fun DrawScope.drawPaperGlove(
    palmRect: Rect,
    isLeftHand: Boolean,
    fingerBases: List<Offset>,
    fingerAngles: List<Float>,
    fingerLengths: List<Float>,
    fingerWidths: List<Float>,
    raised: List<Int>,
    fingers: Int,
    textMeasurer: TextMeasurer
) {
    val base = palmRect.top + palmRect.height * 0.70f
    var glove = shadowHandPath(palmRect, isLeftHand, bottom = base + palmRect.height * 0.12f)
    for (i in raised) {
        val finger = capsule(fingerBases[i], fingerAngles[i], fingerWidths[i] * 1.2f, fingerLengths[i] * 0.88f)
        glove = Path.combine(PathOperation.Union, glove, finger)
    }
    val cuffRect = Rect(
        Offset(palmRect.center.x - palmRect.width * 0.34f, base + palmRect.height * 0.04f),
        Size(palmRect.width * 0.68f, palmRect.height * 0.2f)
    )
    val cuff = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(cuffRect, CornerRadius(cuffRect.height / 2))) }
    val silhouette = Path.combine(PathOperation.Union, glove, cuff)

    translate(5.dp.toPx(), 7.dp.toPx()) { drawPath(silhouette, PaperInk.Shadow) }
    drawPath(silhouette, PaperInk.PaperWhite, style = Stroke(12.dp.toPx(), join = StrokeJoin.Round))

    // Glove with cel shading: the shade shows where the glove shifted up-left doesn't cover it
    drawPath(glove, PaperInk.GloveShade)
    clipPath(glove) { translate((-4).dp.toPx(), (-4).dp.toPx()) { drawPath(glove, PaperInk.PaperWhite) } }
    drawPath(glove, PaperInk.Outline, style = Stroke(2.5.dp.toPx(), join = StrokeJoin.Round))

    // Stitching on the back of the glove
    for (rel in listOf(0.38f, 0.5f, 0.62f)) {
        val x = palmRect.left + palmRect.width * rel
        drawLine(PaperInk.Outline.copy(alpha = 0.55f), Offset(x, palmRect.top + palmRect.height * 0.1f), Offset(x, palmRect.top + palmRect.height * 0.26f), 2.dp.toPx(), cap = StrokeCap.Round)
    }

    // Rolled cuff
    drawPath(cuff, PaperInk.GloveShade)
    clipPath(cuff) { translate(0f, (-3).dp.toPx()) { drawPath(cuff, PaperInk.PaperWhite) } }
    drawPath(cuff, PaperInk.Outline, style = Stroke(2.5.dp.toPx()))

    if (fingers > 0) drawCountBadge(Offset(palmRect.center.x, palmRect.top + palmRect.height * 0.46f), fingers, textMeasurer)
}

/** The count as a round badge sticker: white rim, red face, bold white number. */
fun DrawScope.drawCountBadge(center: Offset, count: Int, textMeasurer: TextMeasurer) {
    val r = 14.dp.toPx()
    drawCircle(PaperInk.Shadow, r + 3.dp.toPx(), center + Offset(2.dp.toPx(), 3.dp.toPx()))
    drawCircle(PaperInk.PaperWhite, r + 3.dp.toPx(), center)
    drawCircle(PaperInk.Red, r, center)
    drawCircle(PaperInk.Outline, r, center, style = Stroke(2.dp.toPx()))
    drawArc(Color.White.copy(alpha = 0.45f), 200f, 70f, false, center - Offset(r * 0.7f, r * 0.7f), Size(r * 1.4f, r * 1.4f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    val label = textMeasurer.measure(count.toString(), TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.SansSerif))
    drawText(label, topLeft = center - Offset(label.size.width / 2f, label.size.height / 2f))
}

/** A knocked-out hand: a crumpled ball of paper with crease lines. */
fun DrawScope.drawCrumpledPaper(center: Offset, radius: Float) {
    val r = java.util.Random(7)
    val n = 11
    val pts = (0 until n).map { i ->
        val a = 2 * PI.toFloat() * i / n
        val rr = radius * (0.8f + r.nextFloat() * 0.3f)
        center + Offset(cos(a) * rr, sin(a) * rr * 0.85f)
    }
    val ball = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        pts.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    paperSprite(ball, Color(0xFFF1F1EE), border = 5.dp.toPx())
    listOf(0, 3, 5, 8).forEach { i ->
        drawLine(PaperInk.Outline.copy(alpha = 0.35f), center + (pts[i] - center) * 0.15f, pts[i], 1.5.dp.toPx())
    }
}

/** A move: a paper airplane flies an arc from [start] to [end] and lands in a starburst. */
fun DrawScope.drawPaperPlaneMove(start: Offset, end: Offset, progress: Float) {
    val dir = end - start
    val len = dir.getDistance()
    if (len <= 0f) return
    val ortho = Offset(-dir.y / len, dir.x / len)
    val control = start + dir * 0.5f + ortho * 60.dp.toPx()
    fun point(t: Float) = start * ((1 - t) * (1 - t)) + control * (2 * (1 - t) * t) + end * (t * t)
    val t = progress.coerceIn(0f, 1f)

    // Dashed flight path behind the plane
    val dashes = 18
    for (i in 0 until (dashes * t).toInt()) {
        val a = point(i.toFloat() / dashes)
        val b = point((i + 0.5f) / dashes)
        drawLine(PaperInk.Outline.copy(alpha = 0.35f), a, b, 2.dp.toPx(), cap = StrokeCap.Round)
    }

    // Starburst on landing
    if (t > 0.7f) {
        val k = ((t - 0.7f) / 0.3f).coerceIn(0f, 1f)
        val pop = FastOutSlowInEasing.transform(k)
        paperSprite(starPath(end, 30.dp.toPx() * pop, 0.55f, 10), PaperInk.Gold, border = 5.dp.toPx())
        for (j in 0 until 4) {
            val a = PI.toFloat() / 4 + j * PI.toFloat() / 2
            val c = end + Offset(cos(a), sin(a)) * (34.dp.toPx() + 22.dp.toPx() * pop)
            paperSprite(starPath(c, 8.dp.toPx() * (1.2f - pop * 0.5f), 0.45f, 5), PaperInk.Gold, border = 3.dp.toPx(), shadow = false)
        }
    }

    val p = point(t)
    val tangent = (control - start) * (2 * (1 - t)) + (end - control) * (2 * t)
    val s = 22.dp.toPx()
    translate(p.x, p.y) {
        rotate(Math.toDegrees(atan2(tangent.y, tangent.x).toDouble()).toFloat(), pivot = Offset.Zero) {
            val plane = Path().apply {
                moveTo(s * 0.7f, 0f)
                lineTo(-s * 0.5f, -s * 0.5f)
                lineTo(-s * 0.25f, 0f)
                lineTo(-s * 0.5f, s * 0.5f)
                close()
            }
            paperSprite(plane, PaperInk.PaperWhite, border = 4.dp.toPx())
            val lowerWing = Path().apply {
                moveTo(s * 0.7f, 0f)
                lineTo(-s * 0.25f, 0f)
                lineTo(-s * 0.5f, s * 0.5f)
                close()
            }
            drawPath(lowerWing, PaperInk.GloveShade)
            drawPath(lowerWing, PaperInk.Outline, style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}

/** A star with [points] spikes; [inner] is the inner radius as a fraction of [r]. */
fun starPath(c: Offset, r: Float, inner: Float, points: Int): Path = Path().apply {
    for (i in 0 until points * 2) {
        val a = -PI.toFloat() / 2 + PI.toFloat() * i / points
        val rr = if (i % 2 == 0) r else r * inner
        val p = c + Offset(cos(a) * rr, sin(a) * rr)
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/** Main-menu emblem: two paper-cutout chopsticks (red and gold) and a star. */
fun DrawScope.drawPaperChopsticks(scale: Float) {
    val c = center
    listOf(-40f to PaperInk.Red, 48f to PaperInk.Gold).forEach { (angle, color) ->
        rotate(angle, pivot = c) {
            val l = 104.dp.toPx() * scale
            val stick = Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(c.x - l, c.y - 6.dp.toPx() * scale, c.x + l, c.y + 6.dp.toPx() * scale), CornerRadius(6.dp.toPx() * scale)))
            }
            paperSprite(stick, color)
        }
    }
    paperSprite(starPath(c + Offset(0f, (-78).dp.toPx() * scale), 22.dp.toPx() * scale, 0.5f, 5), PaperInk.Gold)
}

/** Main-menu backdrop: the cardboard theatre with the spotlights on the title. */
@Composable
fun PaperStageBackground(modifier: Modifier = Modifier) {
    val sway = rememberPaperSway()
    Canvas(modifier.fillMaxSize()) {
        drawPaperStage(
            spotlight = Rect(Offset(size.width * 0.2f, size.height * 0.1f), Size(size.width * 0.6f, size.height * 0.16f)),
            sway = sway.value,
            hop = 0f,
            // Tucked into the top corners, clear of the menu buttons
            clouds = listOf(0.1f to 0.09f, 0.9f to 0.12f)
        )
    }
}

/** A button as a HUD pill: white, bold outline, drop shadow, and a red star badge on the left. */
@Composable
fun PaperPillButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, alpha: Float) {
    Row(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { this.alpha = alpha }
            .drawBehind {
                val pill = CornerRadius(size.height / 2)
                drawRoundRect(PaperInk.Shadow, topLeft = Offset(0f, 4.dp.toPx()), size = size, cornerRadius = pill)
                drawRoundRect(PaperInk.PaperWhite, cornerRadius = pill)
                drawRoundRect(PaperInk.Outline, cornerRadius = pill, style = Stroke(2.5.dp.toPx()))
            }
            .clickable(enabled = enabled) { onClick() }
            .padding(start = 7.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(34.dp)) {
            drawCircle(PaperInk.Red)
            drawCircle(PaperInk.Outline, style = Stroke(2.dp.toPx()))
            drawPath(starPath(center, size.minDimension * 0.32f, 0.5f, 5), Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text.uppercase(),
            color = if (enabled) PaperInk.Outline else PaperInk.Outline.copy(alpha = 0.4f),
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * A speech bubble, the shape of every line of dialogue. The tail points down from the lower-left,
 * or up from the upper-left when [tailOnTop] (for a speaker above the bubble).
 */
@Composable
fun PaperSpeechBubble(
    modifier: Modifier = Modifier,
    tailOnTop: Boolean = false,
    showStar: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .drawBehind {
                val tailH = 14.dp.toPx()
                val body = if (tailOnTop) Rect(Offset(0f, tailH), Size(size.width, size.height - tailH))
                           else Rect(Offset.Zero, Size(size.width, size.height - tailH))
                val bubble = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(body, CornerRadius(minOf(22.dp.toPx(), body.height / 2))))
                }
                val tail = Path().apply {
                    val edge = if (tailOnTop) body.top + 2f else body.bottom - 2f
                    moveTo(size.width * 0.22f, edge)
                    lineTo(size.width * 0.2f, if (tailOnTop) 0f else size.height)
                    lineTo(size.width * 0.34f, edge)
                    close()
                }
                val shape = Path.combine(PathOperation.Union, bubble, tail)
                paperSprite(shape, PaperInk.PaperWhite, border = 6.dp.toPx())
                // The little star in the corner
                if (showStar) {
                    paperSprite(starPath(Offset(size.width - 16.dp.toPx(), body.bottom - 14.dp.toPx()), 9.dp.toPx(), 0.5f, 5), PaperInk.Gold, border = 3.dp.toPx(), shadow = false)
                }
            }
            .padding(
                start = 22.dp, end = 22.dp,
                top = if (tailOnTop) 26.dp else 18.dp,
                bottom = if (tailOnTop) 14.dp else 32.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/** Game over: "Curtain call!" in a big speech bubble, with the crowd's verdict. */
@Composable
fun PaperCurtainCall(result: String, subtitle: String, button: @Composable () -> Unit) {
    PaperSpeechBubble(Modifier.padding(horizontal = 8.dp)) {
        Text(
            "CURTAIN CALL!",
            color = PaperInk.Red,
            fontSize = 34.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = Shadow(PaperInk.Outline.copy(alpha = 0.35f), Offset(0f, 5f), 0f))
        )
        Text(result, color = PaperInk.Outline, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
        Text(subtitle, color = PaperInk.Outline.copy(alpha = 0.75f), fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
        button()
    }
}
