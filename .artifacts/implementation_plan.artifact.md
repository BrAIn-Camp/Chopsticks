# Implementation Plan - UI Refinement & Language Simplification

Refine the Chopsticks app's UI and language to be more user-friendly and less technical. This includes moving game rules to a dedicated configuration screen, simplifying the main menu, and replacing technical jargon with plain language.

## Proposed Changes

### [Navigation & Structure]

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/MainActivity.kt)
- Add `Configuration` to `ChopsticksRoute`.
- Handle navigation to the new `ConfigurationScreen`.

#### [NEW] [ConfigurationScreen.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/ui/ConfigurationScreen.kt)
- Create a new screen that hosts `RulesetConfig` settings (checkboxes, slider) and AI difficulty selection.
- Use a clean, simple layout matching the app's aesthetic.

### [UI & Language Polish]

#### [MODIFY] [MainMenu.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/ui/MainMenu.kt)
- Simplify the menu:
    - Remove the `RulesetConfig` section.
    - Remove the `AI Difficulty` section.
    - Add a "CONFIGURE RULES" button that navigates to the new screen.
- Update language:
    - "Ruleset Configuration" -> "Game Rules".
    - Update other labels as per user requirements.
- Change "START QUANTUM GRID" to "START GAME".

#### [MODIFY] [RuleGuide.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/ui/RuleGuide.kt)
- Update "DECODED RULES MATRIX" -> "GAME RULES".
- Update "▼ MATHEMATICAL VECTOR GRAPH" -> "MOVE VISUALIZATION".
- Simplify technical explanations in the rule descriptions.

#### [MODIFY] [GameScreen.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/ui/game/GameScreen.kt)
- Ensure "VECTOR DISTANCE: XXXpx" is not present (already removed with themes, but will verify).
- Update status texts if any technical terms are present.

### [Language Mapping]
- "Rollover" -> "Overflow (Numbers wrap around)"
- "Exact Five" -> "Exact Target (Must hit limit exactly)"
- "Divisions" -> "Revive Dead Hands"
- "Live Transfers" -> "Move Fingers Between Hands"
- "Suicide" -> "Allow Killing Own Hand"
- "Identity Rule" -> "Change Required (No simple swaps)"
- "Misere Mode" -> "Reverse Win (Last to lose wins)"
- "Max Fingers" -> "Fingers per Hand"
- "AI Action Vector" -> "Move Path"

## Verification Plan

### Automated Tests
- Run existing tests to ensure no regressions in game logic.
- `./gradlew :app:testDebugUnitTest`

### Manual Verification
- Verify navigation between Main Menu, Configuration, and Game.
- Check that all technical terms are replaced with user-friendly language.
- Confirm that "VECTOR DISTANCE" is not visible during AI moves.
