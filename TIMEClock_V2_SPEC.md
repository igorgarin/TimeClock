# TimeClock V2 — Product & Technical Specification

Status: implementation baseline for branch `v2-development`
Target device: Teclast T40 Air
Design reference canvas: 2000 × 1200 px (5:3), landscape

## 1. Branching and preservation

- `master` is the V1 baseline and must remain unchanged.
- All V2 work is performed on `v2-development`.
- Do not merge V2 into `master` unless the owner explicitly requests it.
- Keep V1 behavior available for regression comparison while V2 is developed.

## 2. Product purpose

TimeClock is a full-screen Android visual clock for camera/video synchronization and post-production alignment. The display is intended to be filmed by cameras. It is a visual synchronization aid, not a claim of genlock or camera-accurate SMPTE timecode.

## 3. Target display and coordinate system

- Primary target: Teclast T40 Air, landscape.
- Design canvas: 2000 × 1200 reference pixels.
- Convert reference coordinates to the actual content area using a single uniform scale factor; do not assume Illustrator px are Android dp.
- Account for system bars/immersive full-screen insets consistently.
- Keep layout values centralized in a dedicated UI layout configuration, separate from timing/business logic.
- All movable/resizable decorative elements must be adjustable by changing configuration values, not by editing timer code.
- Preserve the existing V1 date and main time positions and sizes unless the owner explicitly approves a change.

## 4. Main screen elements and locked design values

Values below are reference-canvas pixels unless stated otherwise.

| Element | Requirement |
|---|---|
| Date | Preserve V1 position and size |
| Main HH:MM:SS time | Preserve V1 position and size |
| FPS selector | 110 px from top |
| Color-scheme button | 60 × 60 px |
| Logo | 360 × 148 px; 20% opacity; 90 px from left and bottom |
| Zener figure container | 390 × 390 px; 490 px from left and 90 px from bottom |
| Frame Counter | 90 px from bottom; right inset equal to V1 milliseconds right inset |
| Clapperboard | Visible on all screens; use SVG asset; preserve breathing animation |

When a V2 design reference does not specify an inset, use the corresponding V1 inset. Do not invent new positions for the date/time.

## 5. Zener figures

- Use five supplied SVG assets in the established order: circle, triangle up, cross, triangle down, star.
- Each SVG uses the same 390 × 390 px container/viewBox.
- Preserve intentional per-figure vertical offsets encoded in the artwork; do not normalize their visible artwork centers.
- Switch figure every 20 ms (50 changes per second), independent of selected FPS.
- Use SVG/vector resources, not Unicode glyphs.

## 6. Frame Counter and FPS

Available FPS values: 24, 25, 30, 50, 60.

- Remove 100/120 FPS options from V2.
- 29.97 DF/NDF is out of scope for this V2 release.
- Frame Counter displays the frame index within the current second, not hundredths of a second.
- Expected cycles:
  - 24 FPS: 00–23, then 00
  - 25 FPS: 00–24, then 00
  - 30 FPS: 00–29, then 00
  - 50 FPS: 00–49, then 00
  - 60 FPS: 00–59, then 00
- Derive frame index from the current wall-clock second boundary and a monotonic clock measurement; do not implement it as an unconstrained incrementing counter that accumulates drift.
- The Android display refresh rate limits visible updates. At 60 FPS on a 60 Hz panel, the UI cannot visibly refresh more often than the panel.
- Keep the displayed wall-clock time tied to system time. Use monotonic time for interval measurement and scheduling.

## 7. FPS selection screen

- Selecting FPS opens a separate screen, not a popup overlay.
- Show 24, 25, 30, 50, 60.
- Selected value is white; unselected values are black, following the approved design reference.
- Choosing a value updates the selection but does not automatically close the screen.
- Return to the main screen only through Android BACK.
- Logo and animated clapperboard remain visible on this screen.
- Preserve V1 insets wherever the V2 selection design does not specify a new inset.

## 8. Color schemes

- Preserve existing V1 schemes.
- Add a neutral-gray scheme using approximately sRGB `#767676` as a visual reference for an 18% gray card.
- This is not a calibrated 18% reflectance surface and must not be described as a substitute for a physical gray card.
- Color-scheme control is a 60 × 60 px vector/icon asset.

## 9. Animation and display behavior

- Keep the clapperboard breathing animation continuous on every screen.
- Preserve its existing behavior as the starting point: 6-second cycle, scale 1.5–2.0, alpha 0.10–0.20, reverse/infinite.
- Keep the screen awake and landscape/full-screen as in V1.
- Retain anti-burn-in behavior only after verifying it does not disturb fixed visual alignment or camera synchronization.

## 10. Asset organization

Proposed V2 vector asset location:
`app/src/main/res/drawable/`

Use clear stable resource names, for example:
- `ic_zener_circle.xml` (or SVG imported as Android VectorDrawable)
- `ic_zener_triangle_up.xml`
- `ic_zener_cross.xml`
- `ic_zener_triangle_down.xml`
- `ic_zener_star.xml`
- `ic_timeclock_logo.xml`
- `ic_clapperboard.xml`
- `ic_color_scheme.xml`

Keep source SVG files in a documented design-assets folder if they are committed. Android runtime resources must be in a format supported by Android (VectorDrawable XML or raster drawable); raw SVG files are not directly loadable by standard ImageView.

## 11. Architecture requirements

Separate these concerns:
1. Time/frame calculation and scheduling.
2. Main-screen and FPS-screen navigation/state.
3. UI layout geometry/configuration.
4. Theme/color-scheme definitions.
5. Drawable assets and animation.

Layout configuration should expose named values for element width, height, x/y anchor or inset, and opacity. The renderer converts reference-canvas coordinates to actual device pixels consistently. A later visual adjustment should normally require editing only the layout configuration or replacing an asset.

## 12. Known V1 source observations to account for

The current repository source inspected on `v2-development` is Kotlin + Android Views + ConstraintLayout. The current `MainActivity.kt`:
- Uses `System.currentTimeMillis()` for the 10 ms update loop.
- Displays hundredths of a second (00–99), not a frame counter.
- Chooses Unicode Zener symbols from the millisecond value.
- Has an `onPause()` callback that removes update callbacks; the inspected source does not show a matching `onResume()` restart.
- Uses a PNG clapperboard drawable and a 6-second breathing animator.
- Contains an anti-burn-in translation of the root layout.

The repository source and release notes are not fully aligned on lifecycle/watchdog fixes. Verify the exact release behavior and design a robust single-loop lifecycle before replacing the timing implementation. Do not assume the current source already contains the published v0.1.1 watchdog behavior.

## 13. Acceptance checklist

- [ ] V1 `master` remains untouched.
- [ ] V2 uses the approved 2000 × 1200 design coordinate system on the target tablet.
- [ ] Date and main time retain V1 geometry.
- [ ] Every adjustable layout value is centralized.
- [ ] SVG figures preserve their intended individual offsets.
- [ ] Figure rotation remains 20 ms and independent of FPS.
- [ ] Frame Counter cycles correctly for all five FPS settings.
- [ ] FPS selection remains open after selecting a value; BACK returns.
- [ ] Logo and clapperboard are visible on both screens.
- [ ] Existing color schemes remain; neutral gray is added.
- [ ] Lifecycle interruptions do not freeze or duplicate the timing loop.
- [ ] Long-duration and physical-device tests are completed before release.
