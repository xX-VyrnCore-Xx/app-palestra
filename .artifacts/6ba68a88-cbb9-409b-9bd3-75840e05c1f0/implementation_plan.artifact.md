# Fix Gradle Incompatibility and Enhance Badge/Graphic Realism

This plan addresses the Gradle JVM incompatibility and upgrades the app's visual identity, specifically focusing on making the achievements and badges look more realistic and premium.

## User Review Required

> [!IMPORTANT]
> The Gradle upgrade to 8.10.2 supports Java up to 23. Since you are using Java 25, you may still need to set the **Gradle JDK** to 21 or 23 in Android Studio settings (**Settings > Build, Execution, Deployment > Build Tools > Gradle**) if the build still fails after my changes. I will also configure a Gradle Toolchain to help stabilize this.

> [!NOTE]
> The "realistic" UI changes will introduce gradients, glassmorphism-inspired effects, and more detailed shadows to give the app a more premium, high-quality feel.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle-wrapper.properties](file:///C:/Users/matts/app-palestra/gradle/wrapper/gradle-wrapper.properties)
- Upgrade Gradle version from 8.7 to 8.10.2 to improve compatibility with newer JDKs.

#### [MODIFY] [build.gradle.kts (root)](file:///C:/Users/matts/app-palestra/build.gradle.kts)
- Upgrade Android Gradle Plugin (AGP) from 8.5.2 to 8.7.0.
- Ensure Kotlin version is 2.1.0+ for better compatibility.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/matts/app-palestra/app/build.gradle.kts)
- Add Gradle Toolchain configuration to target Java 21, ensuring the build is stable regardless of the IDE's JVM.

---

### UI Components Enhancement

#### [NEW] [RealisticBadge.kt](file:///C:/Users/matts/app-palestra/app/src/main/java/com/vyrncore/palestra/ui/components/RealisticBadge.kt)
- Create a new component that renders a 3D-like medal using gradients (Gold, Silver, Bronze), layered shadows, and a circular metallic border.

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/matts/app-palestra/app/src/main/java/com/vyrncore/palestra/ui/home/HomeScreen.kt)
- Redesign `MedalSummaryCard` to use the new `RealisticBadge` look.
- Enhance `RankCard` with a glossy military insignia style.
- Add subtle background gradients to the main sections for more depth.

#### [MODIFY] [MetricCard.kt](file:///C:/Users/matts/app-palestra/app/src/main/java/com/vyrncore/palestra/ui/components/MetricCard.kt)
- Update `MetricCard` to use a glassmorphic background (translucent with a fine border) instead of a flat secondary container color.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify the new Gradle configuration is compatible and the project builds successfully.

### Manual Verification
- Deploy the app to the device.
- Navigate to the Home screen.
- Verify the new "Realistic" badges and rank cards:
    - Check for Gold/Silver/Bronze gradients in the medagliere.
    - Verify the glossy progress bar and insignia in the rank card.
    - Ensure the MetricCards have a more modern "glass" look.
