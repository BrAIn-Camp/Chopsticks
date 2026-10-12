package com.example.chopsticks.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RuleGuide(
    onBack: () -> Unit,
    maxFingers: Int = 5,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("Standard") }
    val tabs = listOf("Standard", "Overflow", "Exact Five", "Divisions", "Transfers", "Even Splits", "Suicide", "Identity", "Misere", "Attack", "Exchange", "Condense")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepVoid)
                    .border(1.dp, StarkGrey)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.semantics { contentDescription = "Navigate back to Main Menu" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "DECODED RULES MATRIX",
                    color = PureWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            // Tabs Horizontal Grid Row
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                containerColor = PureBlack,
                contentColor = ElectricLime,
                edgePadding = 16.dp,
                divider = { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(StarkGrey)) },
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        val pos = tabPositions[tabs.indexOf(selectedTab)]
                        Box(
                            Modifier
                                .tabIndicatorOffset(pos)
                                .height(3.dp)
                                .background(ElectricLime)
                        )
                    }
                }
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        },
                        selectedContentColor = ElectricLime,
                        unselectedContentColor = StarkGrey
                    )
                }
            }

            // Main Rule Content (Scrollable for smaller screens)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Rule Description Card Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StarkGrey)
                        .background(DeepVoid)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "OPERATION: $selectedTab",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        
                        val explanation = when (selectedTab) {
                            "Standard" -> "Standard Rule: Hands die when they reach $maxFingers or more fingers."
                            "Overflow" -> {
                                if (maxFingers == 5) {
                                    "If a hand goes over 5, the excess fingers wrap around. For example, with a limit of 5: a 3 hitting a 4 results in 2 fingers (3+4=7, so 7-5=2)."
                                } else {
                                    val target = maxFingers - 1
                                    val attack = 3
                                    val sum = target + attack
                                    val result = sum - maxFingers
                                    "If a hand goes over $maxFingers, the excess fingers wrap around. For example, with a limit of $maxFingers, a $target hitting a $attack results in $result fingers ($target+$attack=$sum, so $sum-$maxFingers=$result fingers)."
                                }
                            }
                            "Exact Five" -> "A hand only dies if it hits exactly $maxFingers. Attacks that would result in more than $maxFingers fingers are not allowed."
                            "Divisions" -> "You can bring a dead hand back into play by moving fingers to it from your other living hand."
                            "Transfers" -> "You can shift fingers between your two living hands to change their values."
                            "Even Splits" -> "You can only redistribute fingers if you have an even number, and you must split them exactly in half. For example, if you have 4 fingers, you can split them into 2 and 2. A hand with 3 fingers cannot be split."
                            "Suicide" -> "You are allowed to attack or transfer fingers to your own hand to reach the limit and make it die."
                            "Identity" -> "Any move between your own hands must result in a different set of numbers. You cannot just swap values like 3-1 to 1-3."
                            "Misere" -> "Last player with a live hand loses. The first player to lose both hands wins the game."
                            "Attack" -> "Tap or drag from one of your active hands to an opponent's active hand. The number of fingers on your hand is added to the opponent's hand. A hand is eliminated based on the active death rules (Standard, Overflow, or Exact Limit)."
                            "Exchange" -> "Logan Clause: A living hand can be sacrificed (set to dead) to revive a dead hand with its current fingers. For example, if your Left hand has 3 fingers and your Right hand is dead, you can kill the Left hand to bring the Right hand back with 3 fingers."
                            "Condense" -> "Meta Rule: If total fingers across both hands exceed $maxFingers, you can combine them, subtract $maxFingers, and split the remainder. For example, if you have 4 and 4 fingers (total 8) with a limit of 5, you subtract 5 and split the remaining 3 fingers (8-5=3)."
                            else -> ""
                        }
                        
                        Text(
                            text = explanation,
                            color = PureWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Justify
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "CRITICAL: A hand that reaches the elimination threshold (usually $maxFingers fingers) is considered dead and removed from play.",
                            color = HotPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 15.sp
                        )
                    }
                }

                // GEOMETRIC DIAGRAM
                Text(
                    text = "▼ HOW IT WORKS",
                    color = StarkGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .border(2.dp, NeonCyan)
                        .background(DeepVoid)
                        .semantics { contentDescription = "Geometric diagram explaining the $selectedTab rule mechanism" },
                    contentAlignment = Alignment.Center
                ) {
                    RuleDiagramCanvas(ruleType = selectedTab)
                }
            }
        }
    }
}

@Composable
fun RuleDiagramCanvas(ruleType: String) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val w = size.width
        val h = size.height

        when (ruleType) {
            "Attack" -> {
                // Attacker box
                drawRect(color = NeonCyan, topLeft = Offset(w * 0.15f, h * 0.4f), size = Size(60.dp.toPx(), 40.dp.toPx()), style = Stroke(2.dp.toPx()))
                // Target box
                drawRect(color = HotPink, topLeft = Offset(w * 0.65f, h * 0.4f), size = Size(60.dp.toPx(), 40.dp.toPx()), style = Stroke(2.dp.toPx()))
                
                // Vector attack line arrow
                val startX = w * 0.15f + 60.dp.toPx()
                val endX = w * 0.65f
                val centerY = h * 0.4f + 20.dp.toPx()
                
                drawLine(color = ElectricLime, start = Offset(startX, centerY), end = Offset(endX, centerY), strokeWidth = 3.dp.toPx())
                
                // Arrow tip
                val path = Path().apply {
                    moveTo(endX, centerY)
                    lineTo(endX - 12.dp.toPx(), centerY - 8.dp.toPx())
                    lineTo(endX - 12.dp.toPx(), centerY + 8.dp.toPx())
                    close()
                }
                drawPath(path, color = ElectricLime)
                
                // Label dots or decorations
                drawCircle(color = PureWhite, radius = 3.dp.toPx(), center = Offset(startX + (endX - startX)/2, centerY))
            }
            "Transfer", "Transfers", "Identity", "Divisions", "Even Splits" -> {
                // Single player area containing two hands
                drawRect(color = StarkGrey, topLeft = Offset(w * 0.1f, h * 0.2f), size = Size(w * 0.8f, h * 0.6f), style = Stroke(1.dp.toPx()))
                
                // Left Hand box
                drawRect(color = NeonCyan, topLeft = Offset(w * 0.2f, h * 0.35f), size = Size(50.dp.toPx(), 40.dp.toPx()), style = Stroke(2.dp.toPx()))
                // Right Hand box
                drawRect(color = NeonCyan, topLeft = Offset(w * 0.55f, h * 0.35f), size = Size(50.dp.toPx(), 40.dp.toPx()), style = Stroke(2.dp.toPx()))
                
                // Bidirectional or flow transfer arrow
                val sX = w * 0.2f + 50.dp.toPx()
                val eX = w * 0.55f
                val cY = h * 0.35f + 20.dp.toPx()
                
                drawLine(color = ElectricLime, start = Offset(sX, cY), end = Offset(eX, cY), strokeWidth = 2.dp.toPx())
                // Small indicator square inside path
                drawRect(color = ElectricLime, topLeft = Offset(sX + (eX - sX)/2 - 5.dp.toPx(), cY - 5.dp.toPx()), size = Size(10.dp.toPx(), 10.dp.toPx()))
            }
            "Standard", "Misere" -> {
                // Hand getting 5 fingers -> cross out
                val cx = w / 2f
                val cy = h / 2f
                val boxSide = 80.dp.toPx()
                
                drawRect(color = StarkGrey, topLeft = Offset(cx - boxSide/2, cy - boxSide/2), size = Size(boxSide, boxSide), style = Stroke(2.dp.toPx()))
                
                // Draw 5 sharp lines inside
                val spacing = boxSide / 6f
                for (i in 1..5) {
                    val lx = cx - boxSide/2 + spacing * i
                    drawLine(color = ElectricLime, start = Offset(lx, cy - boxSide/3), end = Offset(lx, cy + boxSide/3), strokeWidth = 2.dp.toPx())
                }
                
                // Large Red cross meaning DEAD status
                drawLine(color = HighContrastRed, start = Offset(cx - boxSide/2 - 10f, cy - boxSide/2 - 10f), end = Offset(cx + boxSide/2 + 10f, cy + boxSide/2 + 10f), strokeWidth = 4.dp.toPx())
                drawLine(color = HighContrastRed, start = Offset(cx + boxSide/2 + 10f, cy - boxSide/2 - 10f), end = Offset(cx - boxSide/2 - 10f, cy + boxSide/2 + 10f), strokeWidth = 4.dp.toPx())
            }
            "Exact Five" -> {
                // Target with 4 fingers + 2 fingers -> blocked cross
                val cx = w / 2f
                val cy = h / 2f
                
                drawCircle(color = HotPink, radius = 50.dp.toPx(), center = Offset(cx, cy), style = Stroke(3.dp.toPx()))
                // Illegal indicator exclamation mark made of sharp boxes
                drawRect(color = HighContrastRed, topLeft = Offset(cx - 4.dp.toPx(), cy - 25.dp.toPx()), size = Size(8.dp.toPx(), 30.dp.toPx()))
                drawRect(color = HighContrastRed, topLeft = Offset(cx - 4.dp.toPx(), cy + 13.dp.toPx()), size = Size(8.dp.toPx(), 8.dp.toPx()))
            }
            "Overflow" -> {
                // Pentagon or loop circle diagram showing cyclic math
                val cx = w / 2f
                val cy = h / 2f
                val radius = 50.dp.toPx()
                
                drawCircle(color = StarkGrey, radius = radius, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
                
                // Draw 5 points on the circle representing 0, 1, 2, 3, 4
                for (i in 0 until 5) {
                    val angle = i * (2 * Math.PI / 5) - Math.PI / 2
                    val px = cx + radius * cos(angle).toFloat()
                    val py = cy + radius * sin(angle).toFloat()
                    
                    drawRect(
                        color = NeonCyan,
                        topLeft = Offset(px - 5.dp.toPx(), py - 5.dp.toPx()),
                        size = Size(10.dp.toPx(), 10.dp.toPx())
                    )
                }
                
                // A curved or arrow track indicating loop rotation flow
                drawLine(color = ElectricLime, start = Offset(cx - 20f, cy - radius - 15f), end = Offset(cx + 20f, cy - radius - 15f), strokeWidth = 2.dp.toPx())
            }
            "Suicide" -> {
                // Transfer line that terminates the hand
                val leftX = w * 0.25f
                val rightX = w * 0.75f
                val centerY = h * 0.5f
                
                drawRect(color = NeonCyan, topLeft = Offset(leftX - 20.dp.toPx(), centerY - 20.dp.toPx()), size = Size(40.dp.toPx(), 40.dp.toPx()), style = Stroke(2.dp.toPx()))
                drawRect(color = HighContrastRed, topLeft = Offset(rightX - 20.dp.toPx(), centerY - 20.dp.toPx()), size = Size(40.dp.toPx(), 40.dp.toPx()), style = Stroke(2.dp.toPx()))
                
                drawLine(color = HotPink, start = Offset(leftX + 20.dp.toPx(), centerY), end = Offset(rightX - 20.dp.toPx(), centerY), strokeWidth = 2.dp.toPx())
                
                // Termination cross inside target
                drawLine(color = HighContrastRed, start = Offset(rightX - 10.dp.toPx(), centerY - 10.dp.toPx()), end = Offset(rightX + 10.dp.toPx(), centerY + 10.dp.toPx()), strokeWidth = 2.dp.toPx())
                drawLine(color = HighContrastRed, start = Offset(rightX + 10.dp.toPx(), centerY - 10.dp.toPx()), end = Offset(rightX - 10.dp.toPx(), centerY + 10.dp.toPx()), strokeWidth = 2.dp.toPx())
            }
            "Exchange" -> {
                val cx = w / 2f
                val cy = h / 2f
                // Sacrificed hand
                drawRect(color = StarkGrey, topLeft = Offset(cx - 70.dp.toPx(), cy - 20.dp.toPx()), size = Size(40.dp.toPx(), 40.dp.toPx()), style = Stroke(2.dp.toPx()))
                drawLine(color = HighContrastRed, start = Offset(cx - 70.dp.toPx(), cy - 20.dp.toPx()), end = Offset(cx - 30.dp.toPx(), cy + 20.dp.toPx()), strokeWidth = 2.dp.toPx())
                
                // Revived hand
                drawRect(color = ElectricLime, topLeft = Offset(cx + 30.dp.toPx(), cy - 20.dp.toPx()), size = Size(40.dp.toPx(), 40.dp.toPx()), style = Stroke(2.dp.toPx()))
                
                // Arrow
                drawLine(color = NeonCyan, start = Offset(cx - 20.dp.toPx(), cy), end = Offset(cx + 20.dp.toPx(), cy), strokeWidth = 2.dp.toPx())
            }
            "Condense" -> {
                val cx = w / 2f
                val cy = h / 2f
                // Two hands merging
                drawCircle(color = NeonCyan, radius = 30.dp.toPx(), center = Offset(cx - 40.dp.toPx(), cy), style = Stroke(2.dp.toPx()))
                drawCircle(color = NeonCyan, radius = 30.dp.toPx(), center = Offset(cx + 40.dp.toPx(), cy), style = Stroke(2.dp.toPx()))
                
                // Minus maxFingers
                drawRect(color = HighContrastRed, topLeft = Offset(cx - 10.dp.toPx(), cy - 10.dp.toPx()), size = Size(20.dp.toPx(), 4.dp.toPx()))
                
                // Remainder split
                drawRect(color = ElectricLime, topLeft = Offset(cx - 15.dp.toPx(), cy + 40.dp.toPx()), size = Size(30.dp.toPx(), 30.dp.toPx()), style = Stroke(2.dp.toPx()))
            }
        }
    }
}

@Preview(device = "id:pixel_7", showBackground = true)
@Composable
fun RuleGuidePreview() {
    ChopsticksTheme(darkTheme = true) {
        RuleGuide(onBack = {})
    }
}
