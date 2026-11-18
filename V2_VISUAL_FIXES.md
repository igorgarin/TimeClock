# TimeClock V2 visual corrections

Implemented on branch v2-development.

- Added the updated owner-provided clapperboard_V2_03.svg as a standalone vector asset.
- Added SvgCanvasView, which draws the complete SVG into a single composited Canvas layer before applying group alpha and tint. The artwork remains vector; no raster conversion is used.
- Reworked the FPS selector to match the supplied horizontal layout, including the BACK control.
- Reused the same clapperboard and logo geometry and assets on both screens.
- Moved the frame counter to the shared 90 px bottom reference inset and retained the V1 right inset.
- Applied the three color schemes consistently by horizontal band.
- Kept the V1 date/time XML geometry unchanged.

Launcher icon remains pending because the exact supplied PNG must be installed as a binary drawable without recomposition. The previously generated adaptive icon resources are not accepted as final.
