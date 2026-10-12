package com.example.chopsticks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.domain.model.Difficulty
import com.example.chopsticks.domain.model.RulesetConfig
import com.example.chopsticks.ui.theme.HandTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    currentRuleset: RulesetConfig,
    onRulesetChange: (RulesetConfig) -> Unit,
    currentDifficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit,
    onRestoreDefaults: () -> Unit,
    onBack: () -> Unit,
    theme: HandTheme,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "GAME RULES",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = theme.textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = theme.backgroundColor
                )
            )
        },
        containerColor = theme.backgroundColor,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Ruleset Section
            SectionHeader("RULESET", theme)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        theme.secondaryColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(theme.cornerRadius)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val overflowDesc = if (currentRuleset.maxFingers == 5) {
                    "If a hand goes over 5, the excess fingers wrap around. For example, with a limit of 5: a 3 hitting a 4 results in 2 fingers (3+4=7, so 7-5=2)."
                } else {
                    val limit = currentRuleset.maxFingers
                    val target = limit - 1
                    val attack = 3
                    val sum = target + attack
                    val result = sum - limit
                    "If a hand goes over $limit, the excess fingers wrap around. For example, with a limit of $limit, a $target hitting a $attack results in $result fingers ($target+$attack=$sum, so $sum-$limit=$result fingers)."
                }

                RuleToggle(
                    label = "Overflow Fingers",
                    checked = currentRuleset.overflow,
                    theme = theme,
                    description = overflowDesc
                ) {
                    onRulesetChange(currentRuleset.copy(overflow = it))
                }
                RuleToggle(
                    label = "Exact Limit (Illegal to exceed)",
                    checked = currentRuleset.exactFive,
                    theme = theme,
                    description = "A hand only dies if it hits exactly ${currentRuleset.maxFingers}. Attacks that would result in more than ${currentRuleset.maxFingers} fingers are not allowed."
                ) {
                    onRulesetChange(currentRuleset.copy(exactFive = it))
                }
                RuleToggle(
                    label = "Allow Hand Revival",
                    checked = currentRuleset.allowDivisions,
                    theme = theme,
                    description = "You can bring a dead hand back into play by moving fingers to it from your other living hand."
                ) {
                    onRulesetChange(currentRuleset.copy(allowDivisions = it))
                }
                RuleToggle(
                    label = "Allow Transfers",
                    checked = currentRuleset.allowTransfers,
                    theme = theme,
                    description = "You can shift fingers between your two living hands to change their values."
                ) {
                    onRulesetChange(currentRuleset.copy(allowTransfers = it))
                }
                RuleToggle(
                    label = "Allow Self-Destruct",
                    checked = currentRuleset.allowSuicide,
                    theme = theme,
                    description = "You are allowed to attack or transfer fingers to your own hand to reach the limit and make it die."
                ) {
                    onRulesetChange(currentRuleset.copy(allowSuicide = it))
                }
                RuleToggle(
                    label = "Reverse Mode (Misere)",
                    checked = currentRuleset.isMisere,
                    theme = theme,
                    description = "Last player with a live hand loses. The first player to lose both hands wins the game."
                ) {
                    onRulesetChange(currentRuleset.copy(isMisere = it))
                }
                RuleToggle(
                    label = "Allow Value Swaps",
                    checked = currentRuleset.allowSwaps,
                    theme = theme,
                    description = "When disabled, any move between your own hands must result in a different set of numbers. You cannot just swap values like 3-1 to 1-3."
                ) {
                    onRulesetChange(currentRuleset.copy(allowSwaps = it))
                }
                RuleToggle(
                    label = "Exchange Life (Logan Clause)",
                    checked = currentRuleset.allowExchangeLife,
                    theme = theme,
                    description = "A living hand can be sacrificed (set to 0) to revive a dead hand with the same number of fingers. For example, if your Left hand has 3 fingers and your Right hand is dead, you can kill the Left hand to bring the Right hand back with 3 fingers."
                ) {
                    onRulesetChange(currentRuleset.copy(allowExchangeLife = it))
                }
                RuleToggle(
                    label = "Condense Hands (Meta)",
                    checked = currentRuleset.allowCondense,
                    theme = theme,
                    description = "If total fingers across both hands exceed ${currentRuleset.maxFingers}, you can combine them, subtract ${currentRuleset.maxFingers}, and split the remainder. For example, if you have 4 and 4 fingers (total 8) with a limit of 5, you subtract 5 and split the remaining 3 fingers (8-5=3)."
                ) {
                    onRulesetChange(currentRuleset.copy(allowCondense = it))
                }
                RuleToggle(
                    label = "Even Splits Only",
                    checked = currentRuleset.onlyEvenSplits,
                    theme = theme,
                    description = "You can only redistribute fingers if you have an even number, and you must split them exactly in half. For example, if you have 4 fingers, you can split them into 2 and 2. A hand with 3 fingers cannot be split."
                ) {
                    onRulesetChange(currentRuleset.copy(onlyEvenSplits = it))
                }

                // Default Rule Description
                if (!currentRuleset.overflow && !currentRuleset.exactFive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.primaryColor.copy(alpha = 0.1f), shape = RoundedCornerShape(theme.cornerRadius))
                            .border(1.dp, theme.primaryColor.copy(alpha = 0.3f), shape = RoundedCornerShape(theme.cornerRadius))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Standard Rule",
                            color = theme.primaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hands die when they reach ${currentRuleset.maxFingers} or more fingers. This is the default behavior when Overflow and Exact Limit are disabled.",
                            color = theme.textColor,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = theme.secondaryColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    Text(
                        text = "Hand Capacity: ${currentRuleset.maxFingers} Fingers",
                        color = theme.textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "The maximum number of fingers allowed on a single hand.",
                        color = theme.textColor.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
                Slider(
                    value = currentRuleset.maxFingers.toFloat(),
                    onValueChange = { onRulesetChange(currentRuleset.copy(maxFingers = it.toInt())) },
                    valueRange = 2f..10f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.primaryColor,
                        activeTrackColor = theme.primaryColor,
                        inactiveTrackColor = theme.secondaryColor.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // AI Difficulty Section
            SectionHeader("COMPUTER DIFFICULTY", theme)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Difficulty.entries.take(3).forEach { diff ->
                        ModeButton(
                            text = diff.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = currentDifficulty == diff,
                            theme = theme
                        ) { onDifficultyChange(diff) }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Difficulty.entries.drop(3).forEach { diff ->
                        ModeButton(
                            text = diff.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = currentDifficulty == diff,
                            theme = theme
                        ) { onDifficultyChange(diff) }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            ArtisticActionButton(
                text = "RESTORE TO DEFAULTS",
                onClick = onRestoreDefaults,
                theme = theme,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(device = "id:pixel_7", showBackground = true)
@Composable
fun ConfigurationScreenPreview() {
    ConfigurationScreen(
        currentRuleset = RulesetConfig(),
        onRulesetChange = {},
        currentDifficulty = Difficulty.MEDIUM,
        onDifficultyChange = {},
        onRestoreDefaults = {},
        onBack = {},
        theme = HandTheme.AnalogSynth
    )
}
