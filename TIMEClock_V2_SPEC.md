
## 15. Current visual behavior and controls (2026-11-19)

### Main screen
- Three color schemes, cycled by the theme icon:
  - Neutral gray: background `#767676`; date/FPS/theme icon `#333333`; time `#FFFFFF`; Zener figure/ring/frame counter `#000000`.
  - Dark: background `#000000`; date/FPS/theme icon `#FFA800`; time `#FBF72A`; Zener figure/ring/frame counter `#3ED52D`.
  - Light: background `#FFFFFF`; date/FPS/theme icon `#002594`; time `#0033CC`; Zener figure/ring/frame counter `#FF2BFF`.
- The logo stays in its original SVG colors, measures 720 × 296 reference pixels, has a 30% group opacity, a left inset of −60 px, and a bottom inset of 90 px on the 2000 × 1200 reference canvas.
- The theme control measures 60 × 60 reference pixels. The FPS label is bold and positioned at 100 px from the top; the theme control top inset is 110 px.
- The frame counter displays 00 through FPS − 1 for the current second. Its current reference geometry is text size 300 px, right inset 48 px, and bottom margin −20 px.
- The app requests that the screen remain on while active and uses immersive full-screen system UI.

### FPS selector
- Supported rates: 24, 25, 30, 50, 60, and 100 fps.
- Values appear in one horizontal row in a rounded dark-gray band; the selected value is white and other values are black. Values use bold text.
- The selector includes a BACK control. Selecting a rate updates the main-screen label, and both BACK and Android Back return to the main screen.

### Clapperboard and vector artwork
- The logo, theme icon, clapperboard, five Zener figures, and twelve numbered ring animation states load from `app/src/main/assets/design/` as SVG. The ring artwork has six functional large segments; the twelve smaller segments are decorative.
- The clapperboard remains vector and uses a breathing animation with scale 1.5–2.0 and group opacity 10–20%.
- SVG drawing is composited before group opacity/tint is applied. The clapperboard's overlapping internal paths may still appear translucent depending on the SVG artwork; verify its appearance on the target tablet.


## 16. Project and current reference geometry (2026-11-19)

The V2 branch is a single Android application module (`:app`). Gradle configuration is at the repository root; production code, resources, manifest, assets, and unit tests are under `app/src/`. The Gradle Wrapper is at the repository root.

Current main-screen placement on the 2000 × 1200 reference canvas:
- Zener figure and ring share a 500 × 500 px container, with a 770 px left inset and a 25 px bottom inset.
- Frame counter uses a 300 px text size, a 48 px right inset, and a −20 px bottom margin.
- Logo is 720 × 296 px, with a −60 px left inset and 90 px bottom inset.

These are the current implementation values in `TimeClockUiConfig.kt` and `MainActivity.kt`; they supersede the former 390 × 390 px figure, 840 px left inset, 90 px bottom inset, and earlier counter/logo values.

The supplied original launcher PNG is present as an app drawable resource. Whether Android uses it as the installed launcher icon should be verified from the manifest and on the target tablet.

## 17. Timing behavior

- The main-screen update loop is scheduled with Android `Choreographer` frame callbacks (VSync callbacks), not a fixed 8 ms Handler.
- The five Zener figures appear in order: circle, triangle up, cross, triangle down, star. Each occupies 11 of 60 logical frame slots, followed by one empty slot. The five figure/blank pairs fill one second and repeat independently of the selected camera FPS.
- The ring has six functional large segments. Each is active for two consecutive animation states, alongside the corresponding small moving marker; the twelve smaller ring marks are decorative. The twelve numbered SVG states therefore represent six large-segment positions, each shown twice. One state lasts five logical frame slots, so the complete ring sequence takes one second.
- The frame counter is calculated from the current time within the second: supported FPS (N) yields values 00 through (N-1), then resets to 00. It uses a monotonic elapsed-time estimate anchored to wall time and re-anchors after a system clock adjustment, avoiding accumulated scheduling drift.
- The Zener/ring timeline and the frame counter are independent: changing camera FPS does not change the Zener sequence.
