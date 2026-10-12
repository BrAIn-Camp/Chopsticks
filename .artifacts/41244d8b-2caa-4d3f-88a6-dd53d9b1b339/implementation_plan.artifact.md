# PvP UI Refinement Implementation Plan

This plan aims to refine the PvP UI by removing ambiguous text banners and introducing theme-consistent visual turn indicators.

## Proposed Changes

### UI Components

#### [MODIFY] [GameScreen.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/ui/game/GameScreen.kt)
- **Remove Text Banners in PvP**:
    - Update the status text logic to return `null` when in PvP mode, effectively hiding the banner during active gameplay.
    - Update "THINKING..." to "COMPUTER THINKING..." in PvE mode when it's the AI's turn.
- **Implement `TurnIndicator` Composable**:
    - Create a new internal composable `TurnIndicator` that renders:
        - **Analog Synth**: A glowing neon power strip with LED dots, positioned on the side.
        - **Paper Cutout**: A torn paper "ACTIVE" stamp/arrow with ink-like styling.
    - Use `InfiniteTransition` for a subtle pulsing animation.
- **Integrate Indicators into Layout**:
    - Wrap the Top (Opponent) and Bottom (Player) columns in `Box` layouts.
    - Overlay the `TurnIndicator` when `uiState.currentPlayerId` matches the player and the game is in PvP mode.
    - Ensure the top indicator respects the 180° rotation of the opponent's area.

## Verification Plan

### Automated Tests
- No new automated tests are required for this UI-only change, but existing tests will be run to ensure no regressions in game logic.
- `./gradlew :app:testDebugUnitTest`

### Manual Verification
1. **PvP Mode**:
    - Start a PvP game.
    - Verify that no "YOUR TURN" or "THINKING..." banners appear in the middle.
    - Verify that a visual indicator appears on the active player's side.
    - Switch active players and ensure the indicator moves accordingly.
    - Verify the look and feel for both **Analog Synth** and **Paper Cutout** themes.
2. **PvE Mode**:
    - Start a PvE game.
    - Verify that "YOUR TURN" appears when it's the player's turn.
    - Verify that "COMPUTER THINKING..." appears when it's the AI's turn.
    - Verify that NO side indicators appear in PvE mode (as per requirements, they are for PvP).
