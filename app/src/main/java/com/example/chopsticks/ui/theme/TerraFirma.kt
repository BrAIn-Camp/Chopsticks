package com.example.chopsticks.ui.theme

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Terra Firma: a 2D sandbox-adventure look in the spirit of Terraria, drawn as real pixel art.
 *
 * Scenes and hands are painted into a small bitmap with anti-aliasing off, then scaled up with
 * nearest-neighbour filtering, so every edge is a crisp pixel. Outlines are a darker shade of each
 * object's own colour rather than black, with two tones of shading, as in the game's sprites.
 *
 * Each hand is an item in an inventory slot, its finger count shown as the stack count in the
 * slot's corner; the active player's slots get the selected-slot highlight. Day and night mark
 * whose turn it is, with "The Computer has awoken!" in boss purple at nightfall. Moves fly as a
 * falling star with orange damage numbers; a knocked-out hand drops a tombstone. The menu has a
 * pixel forest with a bouncing slime.
 */
object TerraInk {
    val SlotFill = Color(0xCC3B40A0)
    val SlotFillActive = Color(0xE64A54C8)
    val SlotRim = Color(0xFF242870)
    val SlotRimActive = Color(0xFFF4EDB8)
    val SlotLight = Color(0xFF6A72D6)
    val Skin = Color(0xFFE7A47A)
    val SkinShade = Color(0xFFBF7A50)
    val SkinOutline = Color(0xFF6B3820)
    val Tunic = Color(0xFF4F9A44)
    val TunicShade = Color(0xFF346E30)
    val Stone = Color(0xFF8C8C9C)
    val StoneShade = Color(0xFF63636F)
    val StoneOutline = Color(0xFF2F2F3A)
    val BossPurple = Color(0xFFAF4BFF)
    val EventGreen = Color(0xFF32FF82)
    val SlainRed = Color(0xFFE5393B)
    val Damage = Color(0xFFFFA028)
    val Star = Color(0xFFFFE45C)
    val StarOutline = Color(0xFFC98A1E)
    val PanelFill = Color(0xEB2E3480)
    val PanelRim = Color(0xFF5A62C9)
    val Wood = Color(0xFFB5784A)
    val WoodOutline = Color(0xFF6E4326)
}

// ---------------------------------------------------------------------------------------------
// Pixel rendering

/** A reusable low-resolution bitmap with an anti-aliasing-free paint. Keep one per drawing site. */
class PixelBuffer {
    private var bitmap: Bitmap? = null
    val paint = android.graphics.Paint().apply { isAntiAlias = false; isFilterBitmap = false; isDither = false }

    fun canvasFor(w: Int, h: Int): Pair<Bitmap, android.graphics.Canvas> {
        val b = bitmap?.takeIf { it.width == w && it.height == h }
            ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bitmap = it }
        b.eraseColor(0)
        return b to android.graphics.Canvas(b)
    }
}

/** Draws into the low-resolution canvas. Paths come in screen coordinates; everything else is in pixels. */
class PixelPainter(
    val canvas: android.graphics.Canvas,
    private val paint: android.graphics.Paint,
    private val toPixels: android.graphics.Matrix,
    val width: Int,
    val height: Int
) {
    private fun lowRes(path: Path) = android.graphics.Path(path.asAndroidPath()).apply { transform(toPixels) }

    private fun use(color: Color, style: android.graphics.Paint.Style = android.graphics.Paint.Style.FILL) {
        paint.shader = null
        paint.style = style
        paint.color = color.toArgb()
    }

    fun fill(path: Path, color: Color) { use(color); canvas.drawPath(lowRes(path), paint) }

    fun outline(path: Path, color: Color, widthPx: Float = 2f) {
        use(color, android.graphics.Paint.Style.STROKE)
        paint.strokeWidth = widthPx
        paint.strokeJoin = android.graphics.Paint.Join.ROUND
        canvas.drawPath(lowRes(path), paint)
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Color) { use(color); canvas.drawRect(x, y, x + w, y + h, paint) }

    fun dot(x: Int, y: Int, color: Color) = rect(x.toFloat(), y.toFloat(), 1f, 1f, color)

    fun disc(cx: Float, cy: Float, r: Float, color: Color) { use(color); canvas.drawCircle(cx, cy, r, paint) }

    fun verticalGradient(top: Color, bottom: Color, y0: Float, y1: Float) {
        paint.style = android.graphics.Paint.Style.FILL
        // The paint is reused: reset its colour, or the gradient inherits the last colour's alpha
        paint.color = android.graphics.Color.BLACK
        paint.shader = LinearGradient(0f, y0, 0f, y1, top.toArgb(), bottom.toArgb(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, y0, width.toFloat(), y1, paint)
        paint.shader = null
    }

    inline fun clipped(path: android.graphics.Path, block: () -> Unit) {
        canvas.save(); canvas.clipPath(path); block(); canvas.restore()
    }

    fun clipPathOf(path: Path) = lowRes(path)

    inline fun shifted(dx: Float, dy: Float, block: () -> Unit) {
        canvas.save(); canvas.translate(dx, dy); block(); canvas.restore()
    }
}

/**
 * Runs [block] on a canvas [pixel] screen-pixels per art-pixel, then draws it scaled up without
 * smoothing. [preTransform] is applied to paths (in screen space) before they are reduced.
 */
fun DrawScope.drawPixelated(
    buffer: PixelBuffer,
    pixel: Float,
    preTransform: android.graphics.Matrix? = null,
    block: PixelPainter.() -> Unit
) {
    val w = ceil(size.width / pixel).toInt().coerceAtLeast(1)
    val h = ceil(size.height / pixel).toInt().coerceAtLeast(1)
    val (bitmap, canvas) = buffer.canvasFor(w, h)
    val matrix = android.graphics.Matrix().apply {
        if (preTransform != null) set(preTransform)
        postScale(1f / pixel, 1f / pixel)
    }
    PixelPainter(canvas, buffer.paint, matrix, w, h).block()
    drawImage(
        bitmap.asImageBitmap(),
        dstSize = IntSize((w * pixel).roundToInt(), (h * pixel).roundToInt()),
        filterQuality = FilterQuality.None
    )
}

/** The size of one art-pixel on screen: whole device pixels, about 4dp. */
fun DrawScope.terraPixel(): Float = 4.dp.toPx().roundToInt().toFloat().coerceAtLeast(2f)

// ---------------------------------------------------------------------------------------------
// The world

/**
 * The forest surface with the underground below it. [day] runs from 0 (night) to 1 (day);
 * [time] is a 0..1 loop for drifting clouds and twinkling stars; [horizon] is a fraction of height.
 */
fun PixelPainter.drawTerraWorld(day: Float, time: Float, horizon: Float) {
    val w = width.toFloat()
    val h = height.toFloat()
    val ground = (h * horizon).roundToInt().toFloat()

    // Sky, sun and moon, stars
    verticalGradient(
        lerp(Color(0xFF05081C), Color(0xFF3D5BE8), day),
        lerp(Color(0xFF1B2350), Color(0xFF9CC8FF), day),
        0f, ground
    )
    val r = java.util.Random(42)
    repeat(45) {
        val x = r.nextInt(width); val y = r.nextInt(ground.toInt().coerceAtLeast(1))
        val twinkle = 0.5f + 0.5f * sin((time * 12f + r.nextFloat()) * 2f * PI.toFloat())
        dot(x, y, Color.White.copy(alpha = (1f - day) * (0.35f + 0.65f * twinkle)))
    }
    if (day > 0.01f) {
        val sun = Offset(w * 0.78f, h * 0.1f + (1f - day) * h * 0.08f)
        disc(sun.x, sun.y, 9f, Color(0xFFFFF4C8).copy(alpha = 0.25f * day))
        disc(sun.x, sun.y, 6f, Color(0xFFFFF6D0).copy(alpha = day))
    }
    if (day < 0.99f) {
        val moon = Offset(w * 0.22f, h * 0.09f + day * h * 0.08f)
        val a = 1f - day
        disc(moon.x, moon.y, 5f, Color(0xFFD8D4C4).copy(alpha = a))
        listOf(-2 to -1, 1 to 1, 2 to -2, -1 to 2).forEach { (dx, dy) -> dot(moon.x.toInt() + dx, moon.y.toInt() + dy, Color(0xFFA29C88).copy(alpha = a)) }
    }

    // Drifting clouds
    for (i in 0 until 4) {
        val cx = ((time + i * 0.27f) % 1f) * (w + 40f) - 20f
        val cy = ground * (0.18f + 0.13f * i)
        val c = lerp(Color(0xFF2A3256), Color.White, day).copy(alpha = 0.85f)
        rect(cx - 8f, cy, 16f, 3f, c); rect(cx - 5f, cy - 2f, 9f, 2f, c); rect(cx - 1f, cy - 3f, 5f, 1f, c)
    }

    // Far layer: giant dusky tree silhouettes
    val far = lerp(Color(0xFF1A2C44), Color(0xFF4F8F95), day)
    for (i in 0 until 5) {
        val x = w * (0.1f + 0.21f * i)
        rect(x - 2f, ground - 30f, 4f, 30f, far)
        disc(x, ground - 32f, 9f + (i % 2) * 3f, far)
    }

    // Near forest: thin trunks with drooping leaf clumps
    val trunk = lerp(Color(0xFF3B2A1C), Color(0xFF7A5232), day)
    val trunkDark = lerp(Color(0xFF241910), Color(0xFF4A2F1B), day)
    val leaf = lerp(Color(0xFF1B3A28), Color(0xFF3FA34D), day)
    val leafDark = lerp(Color(0xFF10261A), Color(0xFF2B7A38), day)
    for (i in 0 until 6) {
        val x = (w * (0.06f + 0.18f * i) + (i % 2) * 5f).roundToInt().toFloat()
        val top = ground - 22f - (i % 3) * 6f
        rect(x, top, 2f, ground - top, trunk)
        rect(x + 2f, top, 1f, ground - top, trunkDark)
        disc(x + 1f, top - 2f, 5f, leafDark)
        disc(x, top - 3f, 4f, leaf)
        disc(x + 3f, top - 1f, 3f, leaf)
        // a side branch with a leaf clump
        rect(x + 3f, top + 8f, 3f, 1f, trunk)
        disc(x + 7f, top + 7f, 2f, leaf)
    }

    // Grass edge, flowers, and the dirt and stone walls of the underground
    rect(0f, ground, w, 1f, lerp(Color(0xFF2C6B2C), Color(0xFF5CD44A), day))
    rect(0f, ground + 1f, w, 1f, lerp(Color(0xFF1E4F22), Color(0xFF3BA53A), day))
    for (x in 0 until width step 3) {
        if ((x * 7) % 5 == 0) dot(x, ground.toInt() - 1, lerp(Color(0xFF2C6B2C), Color(0xFF5CD44A), day))
        if ((x * 13) % 23 == 0) { dot(x, ground.toInt() - 2, Color(0xFF3BA53A)); dot(x, ground.toInt() - 3, Color(0xFFFFD23A)) }
    }
    val dirtTop = ground + 2f
    val stoneTop = (h * (horizon + (1f - horizon) * 0.72f)).roundToInt().toFloat()
    rect(0f, dirtTop, w, stoneTop - dirtTop, Color(0xFF4E3626))
    rect(0f, stoneTop, w, h - stoneTop, Color(0xFF4A4A57))
    val rr = java.util.Random(7)
    repeat((width * height) / 60) {
        val x = rr.nextInt(width); val y = (dirtTop + rr.nextFloat() * (h - dirtTop)).toInt()
        val inStone = y >= stoneTop
        val c = when {
            inStone && rr.nextInt(9) == 0 -> listOf(Color(0xFFE08A3C), Color(0xFFC9C9D6), Color(0xFFF2D04A))[rr.nextInt(3)] // copper, iron, gold ore
            inStone -> Color(0xFF3B3B47)
            else -> Color(0xFF3E2A1D)
        }
        dot(x, y, c)
    }
    // Faint wall tile grid
    var ty = dirtTop
    while (ty < h) { rect(0f, ty, w, 1f, Color.Black.copy(alpha = 0.08f)); ty += 8f }
}

/** The inventory slot behind a hand: notched pixel corners, a rim, and a highlight when selected. */
fun PixelPainter.drawTerraSlot(selected: Boolean) {
    val w = width.toFloat(); val h = height.toFloat()
    val rim = if (selected) TerraInk.SlotRimActive else TerraInk.SlotRim
    rect(1f, 0f, w - 2f, h, rim); rect(0f, 1f, w, h - 2f, rim)
    rect(1f, 1f, w - 2f, h - 2f, if (selected) TerraInk.SlotFillActive else TerraInk.SlotFill)
    rect(2f, 1f, w - 4f, 1f, TerraInk.SlotLight.copy(alpha = 0.7f))   // top inner highlight
    rect(1f, 2f, 1f, h - 4f, TerraInk.SlotLight.copy(alpha = 0.5f))   // left inner highlight
}

/** A pixel-art hand: skin with a darker outline of the same hue, two tones of shading, a tunic sleeve. */
fun PixelPainter.drawTerraHand(
    palmRect: Rect,
    isLeftHand: Boolean,
    bottom: Float,
    fingerBases: List<Offset>,
    fingerAngles: List<Float>,
    fingerLengths: List<Float>,
    fingerWidths: List<Float>,
    raised: List<Int>
) {
    var hand = shadowHandPath(palmRect, isLeftHand, bottom)
    val fingers = raised.map { i -> capsule(fingerBases[i], fingerAngles[i], fingerWidths[i] * 1.05f, fingerLengths[i] * 0.9f) }
    fingers.forEach { hand = Path.combine(PathOperation.Union, hand, it) }
    outline(hand, TerraInk.SkinOutline, 2f)
    fill(hand, TerraInk.SkinShade)
    clipped(clipPathOf(hand)) { shifted(-1f, -1f) { fill(hand, TerraInk.Skin) } }

    // At this size neighbouring fingers merge into one block: outline each finger above the
    // knuckles so every finger reads on its own
    val aboveKnuckles = Path().apply { addRect(Rect(palmRect.left - 50f, -1000f, palmRect.right + 50f, palmRect.top + palmRect.height * 0.08f)) }
    clipped(clipPathOf(aboveKnuckles)) { fingers.forEach { outline(it, TerraInk.SkinOutline, 1f) } }

    // Tunic sleeve over the wrist and forearm
    val base = palmRect.top + palmRect.height * 0.70f
    val wristY = base + (bottom - base) * 0.3f
    val sleeve = Path.combine(PathOperation.Intersect, hand, Path().apply { addRect(Rect(palmRect.left - 50f, wristY, palmRect.right + 50f, bottom + 50f)) })
    fill(sleeve, TerraInk.TunicShade)
    clipped(clipPathOf(sleeve)) { shifted(-1f, -1f) { fill(sleeve, TerraInk.Tunic) } }
}

/** A tombstone on a little mound of dirt: what a knocked-out hand leaves behind. */
fun PixelPainter.drawTombstone(cx: Float, baseY: Float) {
    val x = cx.roundToInt().toFloat(); val y = baseY.roundToInt().toFloat()
    rect(x - 7f, y, 14f, 2f, Color(0xFF6E4B33))           // mound
    rect(x - 5f, y - 13f, 10f, 13f, TerraInk.StoneOutline)
    rect(x - 4f, y - 14f, 8f, 1f, TerraInk.StoneOutline)
    rect(x - 4f, y - 12f, 8f, 12f, TerraInk.StoneShade)
    rect(x - 4f, y - 13f, 7f, 12f, TerraInk.Stone)
    rect(x - 1f, y - 11f, 2f, 7f, TerraInk.StoneShade)     // cross
    rect(x - 3f, y - 9f, 6f, 2f, TerraInk.StoneShade)
}

/** A green slime mid-bounce: squashes on landing, stretches in the air. [phase] loops 0..1. */
fun PixelPainter.drawSlime(cx: Float, groundY: Float, phase: Float) {
    val jump = sin(phase * PI.toFloat())                   // 0 on the ground, 1 at the top
    val squash = if (phase < 0.12f || phase > 0.88f) 1.25f else 1f - 0.12f * jump
    val bw = (12f * squash).roundToInt()
    val bh = (9f / squash).roundToInt()
    val top = (groundY - bh - jump * 14f).roundToInt()
    val left = (cx - bw / 2f).roundToInt()
    val outline = Color(0xFF2E6B32)
    val body = Color(0xFF4CC04C)
    val shade = Color(0xFF379A3A)
    // A dome: rows narrow toward the top, flat along the bottom
    for (row in 0 until bh) {
        val t = row.toFloat() / bh
        val half = if (t < 0.55f) (bw / 2f) * kotlin.math.sqrt(1f - ((0.55f - t) / 0.55f).let { it * it }) else bw / 2f
        val w = (half * 2).roundToInt().coerceAtLeast(2)
        val x = left + (bw - w) / 2
        rect(x.toFloat(), (top + row).toFloat(), w.toFloat(), 1f, outline)
        if (w > 2 && row > 0 && row < bh - 1) {
            rect(x + 1f, (top + row).toFloat(), w - 2f, 1f, if (row > bh * 0.65f) shade else body)
        }
    }
    // Shine and a pair of dark eyes
    dot(left + 3, top + 2, Color(0xFFD8FFC8)); dot(left + 4, top + 2, Color(0xFFD8FFC8)); dot(left + 3, top + 3, Color(0xFFB9F5A8))
    dot(left + bw / 2 - 2, top + bh / 2, outline); dot(left + bw / 2 + 2, top + bh / 2, outline)
}

// ---------------------------------------------------------------------------------------------
// Effects drawn at full resolution but snapped to the pixel grid

/** A move: a falling star along an arc, shedding sparkles, snapped to the pixel grid. */
fun DrawScope.drawFallingStarMove(start: Offset, end: Offset, progress: Float) {
    val p = terraPixel()
    fun snap(o: Offset) = Offset((o.x / p).roundToInt() * p, (o.y / p).roundToInt() * p)
    val dir = end - start
    val len = dir.getDistance()
    if (len <= 0f) return
    val ortho = Offset(-dir.y / len, dir.x / len)
    val control = start + dir * 0.5f + ortho * 50.dp.toPx()
    fun point(t: Float) = start * ((1 - t) * (1 - t)) + control * (2 * (1 - t) * t) + end * (t * t)
    val t = progress.coerceIn(0f, 1f)

    // Sparkle dust behind the star, fading with distance
    val steps = 24
    for (i in 0 until (steps * t).toInt()) {
        val age = t - i.toFloat() / steps
        val a = (1f - age * 2.5f).coerceIn(0f, 1f)
        if (a <= 0f) continue
        val q = snap(point(i.toFloat() / steps) + ortho * (sin(i * 2.3f) * 6.dp.toPx()))
        drawRect(if (i % 2 == 0) Color(0xFFFF8AD8) else TerraInk.Star, q, Size(p, p), alpha = a)
    }
    // The star: a plus with diagonal tips, outlined
    val c = snap(point(t))
    val cells = listOf(0 to -2, -1 to -1, 0 to -1, 1 to -1, -2 to 0, -1 to 0, 0 to 0, 1 to 0, 2 to 0, -1 to 1, 0 to 1, 1 to 1, -1 to 2, 1 to 2)
    cells.forEach { (dx, dy) ->
        for ((ox, oy) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            drawRect(TerraInk.StarOutline, c + Offset((dx + ox) * p, (dy + oy) * p), Size(p, p))
        }
    }
    cells.forEach { (dx, dy) -> drawRect(TerraInk.Star, c + Offset(dx * p, dy * p), Size(p, p)) }
    drawRect(Color.White, c, Size(p, p))
}

/** Main-menu emblem: two wooden pixel chopsticks crossed behind a fallen star. */
fun DrawScope.drawTerraChopsticks(scale: Float) {
    val p = terraPixel() * 1.5f
    fun cell(x: Float, y: Float, color: Color) = drawRect(color, Offset((x / p).roundToInt() * p, (y / p).roundToInt() * p), Size(p, p))
    val c = center
    val len = 100.dp.toPx() * scale
    listOf(-1f, 1f).forEach { sign ->
        val steps = (2 * len / p).toInt()
        for (i in 0..steps) {
            val k = i.toFloat() / steps - 0.5f
            val x = c.x + sign * k * 2 * len * 0.75f
            val y = c.y + k * 2 * len * 0.62f
            cell(x, y - p, TerraInk.WoodOutline)
            cell(x, y + p, TerraInk.WoodOutline)
            cell(x, y, if (i % 5 == 0) TerraInk.WoodOutline.copy(alpha = 0.6f) else TerraInk.Wood)
        }
    }
    val s = c + Offset(0f, (-74).dp.toPx() * scale)
    val star = listOf(0 to -2, -1 to -1, 0 to -1, 1 to -1, -2 to 0, -1 to 0, 0 to 0, 1 to 0, 2 to 0, -1 to 1, 0 to 1, 1 to 1, -1 to 2, 1 to 2)
    star.forEach { (dx, dy) -> for ((ox, oy) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) cell(s.x + (dx + ox) * p, s.y + (dy + oy) * p, TerraInk.StarOutline) }
    star.forEach { (dx, dy) -> cell(s.x + dx * p, s.y + dy * p, TerraInk.Star) }
}

// ---------------------------------------------------------------------------------------------
// Composables

/** A slow 0..1 loop for drifting clouds and twinkling stars. */
@Composable
fun rememberTerraTime(): State<Float> =
    rememberInfiniteTransition(label = "terra_time").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

/** Status-message text: a colour with a dark outline, the way the game writes events. */
@Composable
fun TerraText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    textAlign: TextAlign = TextAlign.Center
) {
    Box(modifier) {
        val style = TextStyle(fontSize = fontSize, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = textAlign)
        Text(text, style = style.copy(color = Color(0xFF0A0A12), drawStyle = Stroke(width = fontSize.value * 0.32f, join = StrokeJoin.Round)))
        Text(text, style = style.copy(color = color))
    }
}

/** Draws [text] at [topLeft] with the same outlined look, for use inside draw code. */
fun DrawScope.drawTerraText(textMeasurer: TextMeasurer, text: String, topLeft: Offset, color: Color, fontSize: TextUnit) {
    val style = TextStyle(fontSize = fontSize, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    drawText(textMeasurer, text, topLeft, style.copy(color = Color(0xFF0A0A12), drawStyle = Stroke(width = fontSize.toPx() * 0.22f, join = StrokeJoin.Round)))
    drawText(textMeasurer, text, topLeft, style.copy(color = color))
}

/** Main-menu backdrop: the forest by day, the horizon low, a slime bouncing along the grass. */
@Composable
fun TerraMenuBackground(modifier: Modifier = Modifier) {
    val buffer = remember { PixelBuffer() }
    val time = rememberTerraTime()
    val hop = rememberInfiniteTransition(label = "slime").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "hop"
    )
    Canvas(modifier.fillMaxSize()) {
        drawPixelated(buffer, terraPixel()) {
            drawTerraWorld(day = 1f, time = time.value, horizon = 0.84f)
            val ground = (height * 0.84f).roundToInt().toFloat()
            val x = width * (0.15f + 0.7f * (0.5f + 0.5f * sin(time.value * 2f * PI.toFloat() * 3f)))
            drawSlime(x, ground, hop.value)
        }
    }
}

/** A button as a UI panel: indigo with a lighter rim and notched pixel corners, outlined white text. */
@Composable
fun TerraPanelButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, alpha: Float) {
    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { this.alpha = alpha }
            .drawBehind {
                val n = 4.dp.toPx()
                val panel = Path().apply {
                    moveTo(n, 0f); lineTo(size.width - n, 0f); lineTo(size.width - n, n); lineTo(size.width, n)
                    lineTo(size.width, size.height - n); lineTo(size.width - n, size.height - n); lineTo(size.width - n, size.height)
                    lineTo(n, size.height); lineTo(n, size.height - n); lineTo(0f, size.height - n); lineTo(0f, n); lineTo(n, n); close()
                }
                drawPath(panel, TerraInk.PanelFill)
                drawPath(panel, TerraInk.PanelRim, style = Stroke(2.dp.toPx()))
            }
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        TerraText(text, if (enabled) Color.White else Color(0xFF9A9AB8), fontSize = 15.sp)
    }
}

/** Game over in the game's own words: slain in red, or the defeated boss in purple. */
@Composable
fun TerraEndScreen(headline: String, headlineColor: Color, result: String, button: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TerraText(headline, headlineColor, fontSize = 30.sp)
        TerraText(result, Color.White, fontSize = 15.sp, modifier = Modifier.padding(top = 14.dp, bottom = 28.dp))
        button()
    }
}
