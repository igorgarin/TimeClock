
## 15. Visual correction pass (2026-11-18)

The next V2 build uses the owner's updated clapperboard_V2_03.svg as a standalone SVG asset. It remains vector at runtime. SvgCanvasView composites the complete SVG into one Canvas layer and applies group alpha/tint only after drawing, preventing overlapping SVG paths from showing through each other.

Main-screen corrections:
- Preserve the legacy date and main-time XML geometry.
- Apply the 2000 × 1200 reference scale to the logo, Zener figure, frame counter, FPS label, theme icon, and clapperboard.
- Align the frame counter bottom inset to the Zener figure's 90 px reference inset and retain the V1 right inset.
- Use one shared color per horizontal band: date/FPS/theme icon; main time; Zener/frame counter.
- Apply SVG tint through a composited layer so Zener shapes follow the selected scheme.
- Keep the logo's original vector fill and apply its 20% group opacity.

FPS screen corrections:
- Neutral-gray background, BACK control at the upper left, horizontal 24/25/30/50/60 row in a rounded dark-gray band, selected number white and others black.
- Logo and clapperboard reuse the same SVG assets, reference geometry, group opacity, and breathing animation as the main screen.
- BACK control and Android BACK both return to the main screen; selecting FPS does not close the screen.

Launcher icon correction remains pending: the original supplied PNG must be installed unchanged as a density-independent drawable before the launcher-icon correction can be considered complete.


Build validation requested after visual correction commit `4699a34524c63643b6c581824d6f022ce2b46539`.
