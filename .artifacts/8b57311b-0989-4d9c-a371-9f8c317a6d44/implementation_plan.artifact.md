# Theme Removal and Cleanup Plan

Modify the Chopsticks app to only include 'Analog Synth' and 'Paper Cutout' themes. Remove 'Swiss Blueprint', 'Void Vector', and 'Bauhaus Primary'.

## Proposed Changes

### [UI Theme]

#### [MODIFY] [HandTheme.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/ui/theme/HandTheme.kt)
- Remove `SwissBlueprint`, `VoidVector`, and `BauhausPrimary` object definitions.
- Update `allThemes` to include only `AnalogSynth` and `PaperCutout`.

#### [MODIFY] [MainMenu.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/ui/MainMenu.kt)
- Update `MainMenuPreview` to use `HandTheme.AnalogSynth` instead of `HandTheme.VoidVector`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/MainActivity.kt)
- Change default theme from `HandTheme.VoidVector` to `HandTheme.AnalogSynth`.

### [Game UI]

#### [MODIFY] [GameScreen.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/main/java/com/example/chopsticks/ui/game/GameScreen.kt)
- In `GameScreen`, remove deleted themes from the `statusText` logic.
- In `drawThemeVector`, remove the `when(theme)` branches for deleted themes.
- In `ThemedHand`, remove palm and finger drawing logic for deleted themes.
- Update `GameScreenThemesPreview` if needed (it uses `HandTheme.allThemes` which is already updated).

## Verification Plan

### Automated Tests
- Run existing unit tests to ensure AI and Rules Engine logic are intact.
- Check build status to ensure no compilation errors due to missing theme references.

### Manual Verification
- Verify that only two themes are visible in the Main Menu.
- Verify that the game starts with 'Analog Synth' by default.
- Verify that 'Analog Synth' and 'Paper Cutout' themes render correctly and their vector animations work.
- Confirm that AI rule compliance and animation timing remain intact in `GameViewModel.kt`.
