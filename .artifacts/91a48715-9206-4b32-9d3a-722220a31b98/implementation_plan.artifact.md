# Implementation Plan - Fix KSP Deprecation in KMP

The project is experiencing a Gradle sync error because the `ksp` configuration is deprecated in Kotlin Multiplatform (KMP) projects. The recommended approach is to use target-specific KSP configurations (e.g., `kspAndroid`, `kspIosArm64`) to ensure the annotation processor runs correctly for each target.

## User Review Required

> [!IMPORTANT]
> The fix involves explicitly defining KSP dependencies for each platform target (Android and iOS). If you add more targets to your KMP project in the future (like Desktop or Web), you will need to add corresponding `ksp<Target>` entries in the `dependencies` block.

## Proposed Changes

### [composeApp]

#### [MODIFY] [build.gradle.kts](file:///D:/Github/Schedulizer/composeApp/build.gradle.kts)

Replace the deprecated `ksp` configuration with target-specific configurations for Room:
- `kspAndroid`
- `kspIosArm64`
- `kspIosSimulatorArm64`

```kotlin
dependencies {
    debugImplementation(libs.compose.uiTooling)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
```

## Verification Plan

### Automated Tests
- Run Gradle Sync to verify the deprecation warning/error is resolved.
- Run `./gradlew assembleDebug` (or equivalent) to ensure Room code generation still works for Android.
- Run `./gradlew compileKotlinIosArm64` (if on Mac) or similar tasks to verify iOS targets.

### Manual Verification
- Check the `build/generated/ksp` directory to ensure Room implementation files (like `AppDatabase_Impl.kt`) are being generated for the targets.
