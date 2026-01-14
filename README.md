# TimeClock

TimeClock is an Android application designed for camera and video synchronization.
It displays precise time, milliseconds, and visual markers intended for post-production
sync when working with multiple cameras or different frame rates.

## Features

- Large HH:MM:SS time display
- Milliseconds (00–99) with high visibility
- Zener symbols synced with milliseconds
- Invert mode (black / white background)
- Anti burn-in micro-shift
- Landscape, full-screen, always-on display

## Zener Symbols

The app cycles through a fixed set of highly distinguishable symbols:

- ●  (circle)
- ▲  (triangle up)
- ✚  (cross)
- ▼  (triangle down)
- ★  (star)

These symbols are designed to be readable at a distance and under various lighting
conditions, making them suitable for camera capture.

## Use Case

TimeClock is intended for professional video and film production workflows:

- Multi-camera shoots
- Cameras recording at different frame rates (e.g. 24 / 25 / 30 fps)
- Situations where the actor does not see the screen
- Post-production synchronization and alignment

The screen is recorded by cameras and later used as a visual reference
for precise timeline matching in editing software.

## Technical Notes

- Android (Kotlin)
- ConstraintLayout-based UI
- Handler-based time loop (10 ms update)
- No network access
- No permissions required

## Status

This project is an early working version and actively evolving.
The focus is on visual clarity, timing accuracy, and real-world production usability.
