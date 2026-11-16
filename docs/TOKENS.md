# Tokens

Code: `ui/theme/Color.kt`, `Type.kt`, `Dimens.kt`, `Motion.kt`. Machine-readable copy: `design/tokens.json`.
Screens never write a raw colour, dp, sp or tween (`ProjectRulesTest`).

## Colour (seed hue h = 222)
| Role | Light | Dark |
|---|---|---|
| background / surface | hsl(h 40% 98.2%) | hsl(h 22% 6.5%) |
| surfaceContainerLow (rows, boxes) | hsl(h 36% 94.8%) | see Color.kt |
| surfaceContainer | hsl(h 30% 91.5%) | |
| surfaceContainerHigh | hsl(h 22% 87.5%) | |
| onSurface | hsl(h 34% 11%) | |
| onSurfaceVariant | hsl(h 12% 37%) | |
| outline | hsl(h 14% 72%) | |
| primary | hsl(h 70% 30%) | |
| primaryContainer / on | hsl(h 82% 91.5%) / hsl(h 72% 16%) | |
| secondaryContainer / on | hsl(h 34% 87.5%) / hsl(h 36% 16%) | |
| error / container / on | hue 356 | |
| warning solid / container / on | hsl(34 92% 46%) / hsl(40 92% 88%) / hsl(30 70% 20%) | hsl(38 92% 62%) / hsl(36 38% 21%) / hsl(40 92% 86%) |

Check hues: Wi-Fi 232 · Hotspots 150 · Bluetooth 268 · Lens 34 · Infrared 338 · Magnet 92 · Checklist 186
(`checkColors(hue)`: container + onContainer).
Brand: launcher background `#173782` (`Brand` in Color.kt: the mark's tile on Welcome and About).

## Type
| Style | Size/line | Weight | Used for |
|---|---|---|---|
| display | 32/40 | 500 | Welcome, big counts |
| headline | 30/38 | 500 | tab titles |
| title | 22/28 | 500 | top bars, sheets, lens headers |
| cardTitle | 16/22 | 500 | rows, boxes, tiles |
| body | 16/24 | 400 | paragraphs |
| supporting | 14/20 | 400 | second lines |
| label | 14/20 | 500 | buttons, chips, section labels |
| caption | 12/16 | 500 | nav, check row, tags |

## Space · Size · Shape
Space: screen 16 · block 12 · row 2 · card 16 · heading 24 · inline 8 · small 4 · dock 28.
Size: row 72 / 60 · icon circle 44 (icon 22) · icon button 48 · button 52 · chip 36 · tag 26 · top bar 72 ·
lens Start 236 (core 136) · lens progress 220 (124) · lens header 124 (64) · check icon 44, check slot 62 · tile ≥ 120.
Shape: surfaces 24 · sheet top 28 · grouped rows 24 outside / 8 inside · pills and circles.

## Motion
See MOTION.md: standard 300 ms, short 150 ms, emphasized easing for push/pop.
