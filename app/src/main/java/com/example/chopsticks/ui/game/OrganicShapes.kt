package com.example.chopsticks.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chopsticks.ui.theme.*
import kotlin.random.Random

fun Modifier.irregularBorder(
    color: Color,
    strokeWidth: Dp,
    cornerRadius: Dp,
    isIrregular: Boolean
) = this.drawBehind {
    if (!isIrregular) {
        drawRoundRect(
            color = color,
            style = Stroke(strokeWidth.toPx()),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    } else {
        val path = createOrganicRectPath(Rect(Offset.Zero, size))
        drawPath(path, color, style = Stroke(strokeWidth.toPx()))
    }
}



internal fun applyJitterToPath(path: Path, jitter: Float): Path {
    val newPath = Path()
    val measures = PathMeasure()
    measures.setPath(path, false)
    
    val length = measures.length
    val step = 4f
    var distance = 0f
    
    var first = true
    while (distance < length) {
        val pos = measures.getPosition(distance)
        val jX = (Random.nextFloat() - 0.5f) * jitter * 2
        val jY = (Random.nextFloat() - 0.5f) * jitter * 2
        if (first) {
            newPath.moveTo(pos.x + jX, pos.y + jY)
            first = false
        } else {
            newPath.lineTo(pos.x + jX, pos.y + jY)
        }
        distance += step
    }
    newPath.close()
    return newPath
}

internal fun createOrganicRectPath(rect: Rect): Path {
    val path = Path()
    val steps = 8
    val jitter = 4f
    
    path.moveTo(rect.left + Random.nextFloat() * jitter, rect.top + Random.nextFloat() * jitter)
    
    // Top
    for (i in 1..steps) {
        val x = rect.left + (rect.width / steps) * i
        path.lineTo(x, rect.top + Random.nextFloat() * jitter)
    }
    // Right
    for (i in 1..steps) {
        val y = rect.top + (rect.height / steps) * i
        path.lineTo(rect.right + Random.nextFloat() * jitter, y)
    }
    // Bottom
    for (i in 1..steps) {
        val x = rect.right - (rect.width / steps) * i
        path.lineTo(x, rect.bottom + Random.nextFloat() * jitter)
    }
    // Left
    for (i in 1..steps) {
        val y = rect.bottom - (rect.height / steps) * i
        path.lineTo(rect.left + Random.nextFloat() * jitter, y)
    }
    path.close()
    return path
}
