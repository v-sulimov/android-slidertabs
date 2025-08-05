# Changelog

All notable changes to this project will be documented in this file.

## [1.0.2] - 05/08/2025

### Changed
- Updated Android Gradle Plugin to version 8.12.0
- Updated Gradle to version 9.0.0

## [1.0.1] - 15/07/2025

### Changed
- Updated Gradle wrapper to version 8.14.3
- Updated Android Gradle Plugin to version 8.11.1

## [1.0.0] - 01/07/2025

### Added
- Initial release of the `SliderTabs` library.
- Custom Android View (`SliderTabs`) for displaying two tabs with a sliding indicator.
- Support for customizable background colors, slider color, text colors, and tab text via XML attributes.
- Smooth sliding animation with configurable duration using `ValueAnimator` and `AccelerateDecelerateInterpolator`.
- Tab selection listener (`OnTabSelectedListener`) for handling left and right tab selection events.
- State persistence across configuration changes using `SavedState`.
- Density-aware sizing for consistent appearance across different screen densities devices.
- Touch handling for switching tabs by tapping the opposite side of the current selection.
- Programmatic tab selection via `selectLeftTab()` and `selectRightTab()` methods.
