# Upgrade to Android 17 (API 37) Implementation Plan

This plan outlines the steps to upgrade the "Medication Wizard" project to compile and target Android 17 (API 37), ensuring compatibility with all layout configurations and modern Android standards.

## User Review Required

> [!IMPORTANT]
> **Android 17 (API 37) Breaking Changes:**
> - **Screen Orientation:** Apps targeting API 37+ might have their `screenOrientation` locks ignored on large screens (tablets/foldables). The app already includes multiple layout configurations (`sw600dp`, `sw720dp`), which mitigates this.
> - **Activity Recreation:** Default behavior for configuration changes may lead to more frequent activity recreations. Ensure state preservation is robust.

## Proposed Changes

### Build Configuration

#### [MODIFY] [build.gradle](file:///C:/Work/medician/build.gradle)
- Verify `SDK_COMPILE_VERSION` and `SDK_TARGET_VERSION` are set to `37`.

#### [MODIFY] [app/build.gradle](file:///C:/Work/medician/app/build.gradle)
- Add `kotlinOptions` with `jvmTarget = "21"`.
- Ensure `compileSdk` and `targetSdk` are correctly referencing the root project variables.

#### [MODIFY] [libs.versions.toml](file:///C:/Work/medician/gradle/libs.versions.toml)
- Fix `firebase-bom` version inconsistency.
- Bump core dependencies:
    - `appcompat` to `1.8.0` (or latest stable in 2026).
    - `material` to `1.15.0` (or latest stable in 2026).
    - `navigation` to `2.10.0`.
    - `lifecycle` to `2.12.0`.

### Layout & Resources

- The project already has 6 layout folders: `layout`, `layout-land`, `layout-sw600dp`, `layout-sw600dp-land`, `layout-sw720dp`, `layout-sw720dp-land`.
- No immediate changes needed as these already cover the requested 6 layouts.

### Translations

- No new strings are added in this upgrade. If any are added later, they will be translated to all 12 supported locales.

## Verification Plan

### Automated Tests
- Run `./gradlew build` to ensure compilation success with API 37.
- Check for any deprecation warnings introduced by API 37.

### Manual Verification
- Deploy the app to an Android 17 emulator/device.
- Verify that the app handles orientation changes correctly across different device types (Phone, 7" Tablet, 10" Tablet).
- Verify that activity state is preserved during configuration changes.
