# Redesign Turn Indicator Implementation Plan

This plan outlines the steps to redesign the turn indicator in the Chopsticks app to be more intuitive and atmospheric, with theme-specific visuals for "Analog Synth" and "Paper Cutout".

## Proposed Changes

### [Component Name] UI/Game

#### [MODIFY] [GameScreen.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/ui/game/GameScreen.kt)
- Update `ThemedHand` to support an `isActive` parameter.
  - In **Analog Synth**, the "oscilloscope heartbeat" will be flat if `isActive` is false.
  - The hand will be slightly dimmed if `isActive` is false.
- Replace the existing `TurnIndicator` with a more sophisticated `ThemeTurnIndicator` system.
- Implement `AnalogSynthTurnIndicator`:
  - **Power Frame**: Neon lines and corners around the active player's hand area.
  - **Energy Cable**: A pulsing neon Bezier curve connecting the side of the screen to the active player's hands.
- Implement `PaperCutoutTurnIndicator`:
  - **Paper Spotlight**: An irregular textured paper shape sliding behind the active player's hands.
  - **Tracing Paper**: A translucent, grainy layer dimming the inactive player's hands.
  - **Active Stamp**: A sketchy, stop-motion stamp next to the current player's hands.
- Integrate these indicators into `GameScreen` using `updateTransition` for smooth animations when the turn switches.
- Handle 180° rotation for the top player's indicators.
- Remove text-based turn indicators in PvP.
- Update PvE status message to be theme-integrated (e.g., stapled label or digital display).

## Verification Plan

### Automated Tests
- N/A (UI-centric changes)

### Manual Verification
- Launch the app in PvP mode and verify:
  - Turn switch triggers smooth transitions for Energy Cable and Paper Spotlight.
  - Inactive player hands are dimmed and have a flat heartbeat (Synth).
  - Power Frame and Paper Spotlight correctly surround/underlie the hands.
  - Tracing Paper correctly dims the inactive side in Paper Cutout.
  - Indicators are correctly rotated for the top player.
- Launch the app in PvE mode and verify:
  - "COMPUTER THINKING" is correctly integrated into the theme.
  - Turn indicators still work as expected.
- Verify "TRY AGAIN" button functionality and styling.
