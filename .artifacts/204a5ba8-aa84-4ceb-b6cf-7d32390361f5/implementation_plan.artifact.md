# Implementation Plan - Audit and Enhance Wikipedia Rules Tests

Audit the existing unit tests in `app/src/test/java/com/example/chopsticks/domain/engine/` and add comprehensive tests for Wikipedia rules that are currently under-tested.

## Proposed Changes

### [Component Name]

#### [NEW] [WikipediaRulesTest.kt](file:///C:/Users/Zac/AndroidStudioProjects/Chopsticks/app/src/test/java/com/example/chopsticks/domain/engine/WikipediaRulesTest.kt)
Create a new test class to cover specific Wikipedia rules:
- **Logan Clause (Exchange Life)**: Verify sacrifices and revivals, including failure cases when disabled or conditions aren't met.
- **Meta Rule (Condense)**: Verify condensing behavior with various `maxFingers` and interaction with swap/suicide rules.
- **Even Splits Only**: Verify that transfers and divisions are restricted to even source hands and equal splits.
- **Misere Mode**: Verify reversed win conditions.
- **Identity & Swap Rules**: Verify restrictions on identity and swap moves.
- **Variable Capacity**: Verify overflow and death thresholds for different `maxFingers` values.

## Verification Plan

### Automated Tests
- Run all unit tests in `app/src/test/java/com/example/chopsticks/domain/engine/`:
  - `RulesEngineTest.kt`
  - `ComplexRulesTest.kt`
  - `WikipediaRulesTest.kt` (New)
- Command: `./gradlew :app:testDebugUnitTest`
