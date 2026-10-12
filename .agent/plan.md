# Project Plan

A Chopsticks hand game app for Android. 
Key Features:
- 2 players: User vs AI or User vs User (Local).
- Selectable AI difficulty.
- Portrait mode with 2 hands per side.
- Touch and drag interaction (Attack: drag to opponent hand, Transfer/Revive: drag to own hand).
- Multiple rulesets based on Wikipedia: Standard, Exact Five, Rollover, Suicide, etc.
- In-app rule explanations.
- Visual Style: Abstract, artistic, sharp, unique. Avoid "AI slop" or generic mobile game looks. High-contrast, geometric, or minimalist artistic approach.
- Tech Stack: Jetpack Compose, Kotlin.
- Target: Standard phones (Portrait).

## Project Brief

# Project Brief: Chopsticks

## Features
*   **Dual-Mode Gameplay (PvP & PvE)**: Supports local two-player matches on a single device (Human vs. Human) and a solo mode against an AI with selectable difficulty levels.
*   **Intuitive Drag Interaction**: Gesture-based controls where players drag fingers to "Attack" opponent hands or "Transfer/Revive" points between their own hands.
*   **Customizable Rulesets**: Support for multiple Wikipedia-based variations, including Standard, Exact Five, Rollover, and Suicide rules.
*   **Geometric Artistic UI**: A unique, high-contrast, and minimalist visual style using abstract geometric shapes instead of traditional hand icons.
*   **Interactive Rule Guide**: An in-app reference providing clear explanations and visual aids for each supported ruleset.

## High-Level Technical Stack
*   **Kotlin & Coroutines**: Core language and asynchronous programming for game logic and AI processing.
*   **Jetpack Compose**: Declarative UI framework for building the game's abstract visual style.
*   **Jetpack Navigation 3**: State-driven navigation management for seamless transitions between the menu, game board, and rule explanations.
*   **Compose Material Adaptive**: Implementation of adaptive layouts to ensure consistent portrait orientation behavior across various device form factors.
*   **ViewModel & StateFlow**: Robust architectural components for managing game state and reactive UI updates.

---
*Note: The UI Design Image section is omitted as the generation tool is unavailable.*

## Implementation Steps
**Total Duration:** 1h 20m 50s

### Task_1_Core_Engine: Set up project dependencies (Compose, Navigation 3, Adaptive) and implement the core domain models and rules engine. The engine must support Standard, Exact Five, Rollover, and Suicide variations.
- **Status:** COMPLETED
- **Updates:** Domain models and RulesEngine implemented. Supported rulesets: Standard, Exact Five, Rollover, Suicide. High-contrast geometric theme set up. Unit tests passed.
- **Acceptance Criteria:**
  - Project builds successfully
  - Domain models (Hand, Player, GameState) are defined
  - Rules engine correctly handles move validation and state transitions for all rulesets
- **Duration:** 5m 35s

### Task_2_ViewModel_AI: Develop the GameViewModel to manage the reactive UI state and handle game events. Implement AI logic for PvE mode with selectable difficulty levels.
- **Status:** COMPLETED
- **Updates:** GameViewModel and ChopsticksAI implemented. AI supports Easy (random) and Hard (heuristic) modes. State management uses StateFlow. AI respects all rulesets.
- **Acceptance Criteria:**
  - GameViewModel exposes UI state via StateFlow
  - AI player makes valid moves in PvE mode
  - AI difficulty levels (Easy/Hard) are implemented
- **Duration:** 6m 26s

### Task_3_Game_UI_Gestures: Build the main Game Screen using Jetpack Compose with the geometric artistic style. Implement drag-and-drop mechanics for attacking and transferring fingers. Ensure adaptive layout support for different screen sizes.
- **Status:** COMPLETED
- **Updates:** Geometric abstract game UI implemented. Drag-and-drop mechanics for attacks/transfers working with visual feedback. High-contrast sharp theme applied. UI adapts to portrait screens. Interactive preview available.
- **Acceptance Criteria:**
  - UI features high-contrast geometric abstract shapes
  - Drag-and-drop interaction works for Attack and Transfer actions
  - UI adapts to different device form factors using Material Adaptive
- **Duration:** 3m 20s

### Task_4_Navigation_Menu_Rules: Create the Main Menu, Interactive Rule Guide, and integrate all screens using Jetpack Navigation 3. Add final assets like the app icon.
- **Status:** COMPLETED
- **Updates:** Navigation 3 integrated. Main Menu, Rule Guide, and Settings implemented. Adaptive geometric icon created. Visual polish applied to all screens. All unit tests passed.
- **Acceptance Criteria:**
  - Seamless navigation between Menu, Game, and Rules screens
  - Interactive Rule Guide provides clear visual aids
  - App icon and geometric theme are applied
- **Duration:** 10m 10s

### Task_5_Run_Verify: Perform a final build, run the application, and verify stability. Instruct critic_agent to confirm requirements, check for crashes, and report UI issues.
- **Status:** COMPLETED
- **Updates:** Final verification completed by critic_agent. Build passes, all tests pass, and app is stable. Verified all rulesets (Standard, Exact Five, Rollover, Suicide), PvP/PvE modes, and AI difficulties. The geometric UI style is confirmed to be unique and consistent with requirements. Drag-and-drop mechanics are fully functional.
- **Acceptance Criteria:**
  - Build pass
  - All existing tests pass
  - App does not crash during PvP or PvE gameplay
  - Application aligns with the project brief requirements
- **Duration:** 21m 45s

### Task_6_Redesign_And_Modular_Rules: Implement the visual redesign and modular ruleset. Update RulesEngine and GameViewModel to support a checkbox-based configuration system with any combination of Rollover, Exact Five, Divisions (Revival), Live Transfers, and Suicide. Rewrite the hand rendering using a custom Canvas drawing approach to display stylized hands (palm + fingers) according to the selected visual theme, and completely declutter the game screen by moving settings to the menu.
- **Status:** COMPLETED
- **Updates:** Fixed NullPointerException in HandTheme initialization. Redesign and modular rules implementation completed. 5 themes (Swiss Blueprint, Analog Synth, Paper Cutout, Void Vector, Bauhaus Primary) are functional. Modular ruleset (Rollover, Exact Five, Divisions, Transfers, Suicide) is integrated into menu and engine. Game screen is decluttered. AI adapts to rulesets.
- **Acceptance Criteria:**
  - Modular ruleset checkboxes work in any combination in the menu
  - Custom Canvas draws distinct stylized hands with palm and fingers
  - Game screen is decluttered with rules moved to menu
  - AI adapts behavior to the selected modular ruleset combination
- **Duration:** 15m 33s

### Task_7_Run_And_Verify_Redesign: Perform a final build, run the application, and verify stability. Instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Final verification successful. The NullPointerException is fixed. All 5 artistic themes are functional and unique. Modular ruleset is fully operational. Game screen is clean and distraction-free. AI adapts to all rule combinations. App is stable.
- **Acceptance Criteria:**
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - application aligns with user requirements
- **Duration:** 5m 23s

### Task_8_Experimental_Hand_Art: Implement advanced hand anatomy (knuckles, stylized joints, palm lines) and flip Player 2 hands 180°. Enhance the 5 themes with specific artistic elements: Swiss Blueprint (annotations, grids), Analog Synth (Bezier patch cables, oscilloscope heartbeat), Paper Cutout (textures, staples, ink), Void Vector (circuit traces, glitches), and Bauhaus (translucent geometric shapes, BlendMode.Multiply).
- **Status:** COMPLETED
- **Updates:** Implemented experimental hand anatomy (joints, knuckles, palm lines). Flipped Player 2 hands 180 degrees. Added high-concept artistic details to all 5 themes: technical annotations for Blueprint, patch cables/oscilloscopes for Synth, textures/staples for Paper, glitches/circuits for Vector, and geometric blending for Bauhaus. UI is now unique and "interactive art" style.
- **Acceptance Criteria:**
  - Hand rendering includes joints, knuckles, and palm lines
  - Player 2 hands are rotated 180 degrees
  - Themes implement the unique artistic details specified in the brief (cables, annotations, textures, etc.)
  - UI reflects the 'experimental and unorthodox' design goal
- **Duration:** 2m 31s

### Task_9_AI_Vectors_And_Verify: Implement theme-specific animated AI Action Vectors showing the move path (attack/transfer) from source to target. Perform a final build, run the application, and verify stability. Instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Final re-verification successful. Player 2 hands are correctly rotated 180 degrees. Experimental details (knuckles, patch cables, staples, etc.) are bold and radically visible. AI Action Vectors are functional and theme-accurate. The app fully meets the "unorthodox interactive art" requirement. Zero crashes.
- **Acceptance Criteria:**
  - AI moves trigger theme-consistent visual vector animations
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - application aligns with user requirements
- **Duration:** 10m 7s

