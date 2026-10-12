# Implementation Plan - Wikipedia Rules & AI Fix

The AI is reportedly performing illegal moves (reviving dead hands when disabled). We will strictly enforce rule validation in the `RulesEngine`, fix the AI's move generation/simulation, and implement missing Wikipedia rules including Misere mode, variable hand capacity, and identity-change constraints.

## Proposed Changes

### Domain Layer

#### [MODIFY] [RulesetConfig.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/domain/model/RulesetConfig.kt)
- Ensure all required flags for Wikipedia rules are present.

#### [MODIFY] [RulesEngine.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/domain/engine/RulesEngine.kt)
- Replace hardcoded `5` with `state.ruleset.maxFingers`.
- Refine `validateTransfer` and `transfer` to strictly enforce:
    - `allowDivisions` (Revival) only when target hand is dead.
    - `allowTransfers` only when target hand is alive.
    - Identity-Change Constraint: Block moves where the resulting hand state (unordered set of fingers) is the same as the original, or where no change occurs.
- Ensure Misere Mode win conditions are correctly handled in both `attack` and `transfer`.

#### [MODIFY] [ChopsticksAI.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/domain/ai/ChopsticksAI.kt)
- Ensure `getPossibleMoves` and `minimax` strictly follow `RulesEngine.validateMove`.
- Audit move generation to prevent any edge cases where illegal moves might be considered.

### UI Layer

#### [MODIFY] [MainMenu.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/ui/MainMenu.kt)
- Update the UI to include toggles for the new Wikipedia rules.
- Improve the `maxFingers` selection to support more values if needed.

### Verification Plan

#### Automated Tests
- Update `RulesEngineTest.kt` with new test cases:
    - Revival blocked when `allowDivisions` is false.
    - Live-to-live transfers blocked when `allowTransfers` is false.
    - Identity-change constraint (e.g., 3-1 to 1-3) is blocked.
    - Misere mode win/loss conditions.
    - Variable hand capacity (e.g., max 6 fingers).
- Run `./gradlew :app:testDebugUnitTest` to verify all domain logic.

#### Manual Verification
- Launch the app, disable "Allow Divisions", and verify the AI never revives a dead hand.
- Toggle Misere mode and verify the winner is correctly identified.
