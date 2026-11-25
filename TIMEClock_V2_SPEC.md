
## 15. Current visual behavior and controls (2026-11-20)

### Main screen
- Three color schemes, cycled by the theme icon:
  - Neutral gray: background `#767676`; date/FPS/theme icon `#333333`; time `#FFFFFF`; Zener figure/ring/frame counter `#000000`.
  - Dark: background `#000000`; date/FPS/theme icon `#FFA800`; time `#FBF72A`; Zener figure/ring/frame counter `#3ED52D`.
  - Light: background `#FFFFFF`; date/FPS/theme icon `#002594`; time `#0033CC`; Zener figure/ring/frame counter `#FF2BFF`.
- The main screen uses `clapperboard_logo_4.svg`; FPS and Sound screens use `clapperboard_logo.svg`. The separate IGRN logo view is not displayed.
- The Clack control uses `Clack_Button_02.svg`, at 120 px from the left and 100 px from the bottom on the 2000 × 1200 reference canvas. Pressing it immediately plays the selected signal and flashes the main background red for 100 ms.
- The theme control measures 60 × 60 reference pixels. The FPS label is bold and positioned at 100 px from the top; the theme control top inset is 110 px.
- The frame counter displays 00 through FPS − 1 for the current second. Its rendered text size matches the main clock while retaining its regular font weight; its reference geometry is right inset 48 px and bottom margin −28 px.
- The app requests that the screen remain on while active and uses immersive full-screen system UI.

### FPS selector
- Supported rates: 24, 25, 30, 50, 60, and 100 fps.
- Values appear in one horizontal row in a rounded dark-gray band; the selected value is white and other values are black. Values use bold text.
- The selector includes a BACK control. Selecting a rate updates the main-screen label, and both BACK and Android Back return to the main screen.

### Sound settings and signal selection
- The Sound screen uses the same BACK placement and clapperboard treatment as the FPS screen.
- The volume control is a ten-step horizontal drag slider. Level 1 is muted; level 10 is full app playback volume. The indicator follows the finger and the selected sound previews once when the gesture ends or pauses.
- Six sounds (`Signal1.wav` through `Signal6.wav`) are selected using a row of large numbered choices. Selecting a different number previews that sound once.
- The sound files are loaded from `app/src/main/assets/audio/` and the vector button/clapperboard artwork from `app/src/main/assets/design/`.

### Clapperboard and vector artwork
- The logo, theme icon, clapperboard, five Zener figures, and twelve numbered ring animation states load from `app/src/main/assets/design/` as SVG. The ring artwork has six functional large segments; the twelve smaller segments are decorative.
- The clapperboard remains vector and uses a breathing animation with scale 1.5–2.0 and group opacity 5–20% on the main and additional screens.
- SVG drawing is composited before group opacity/tint is applied. The clapperboard's overlapping internal paths may still appear translucent depending on the SVG artwork; verify its appearance on the target tablet.


## 16. Project and current reference geometry (2026-11-20)

The V2 branch is a single Android application module (`:app`). Gradle configuration is at the repository root; production code, resources, manifest, assets, and unit tests are under `app/src/`. The Gradle Wrapper is at the repository root.

Current main-screen placement on the 2000 × 1200 reference canvas:
- Zener figure and ring share a 500 × 500 px container, with a 770 px left inset and a 25 px bottom inset.
- Frame counter matches the rendered text size of the clock, retains regular font weight, and uses a 48 px right inset with a −28 px bottom margin.
- The Clack button is 250 × 250 px, with a 120 px left inset and 100 px bottom inset.
- Main-screen clapperboard asset: `clapperboard_logo_4.svg`; additional screens use `clapperboard_logo.svg`.

These are the current implementation values in `TimeClockUiConfig.kt` and `MainActivity.kt`; they supersede the former 390 × 390 px figure, 840 px left inset, 90 px bottom inset, and earlier counter/logo values.

The supplied original launcher PNG is present as an app drawable resource. Whether Android uses it as the installed launcher icon should be verified from the manifest and on the target tablet.

## 17. Timing behavior

- The main-screen update loop is scheduled with Android `Choreographer` frame callbacks (VSync callbacks), not a fixed 8 ms Handler.
- The five Zener figures appear in order: circle, triangle up, cross, triangle down, star. Each occupies 11 of 60 logical frame slots, followed by one empty slot. The five figure/blank pairs fill one second and repeat independently of the selected camera FPS.
- The ring has six functional large segments. Each is active for two consecutive animation states, alongside the corresponding small moving marker; the twelve smaller ring marks are decorative. The twelve numbered SVG states therefore represent six large-segment positions, each shown twice. One state lasts five logical frame slots, so the complete ring sequence takes one second.
- The frame counter is calculated from the current time within the second: supported FPS (N) yields values 00 through (N-1), then resets to 00. It uses a monotonic elapsed-time estimate anchored to wall time and re-anchors after a system clock adjustment, avoiding accumulated scheduling drift.
- The Zener/ring timeline and the frame counter are independent: changing camera FPS does not change the Zener sequence.
