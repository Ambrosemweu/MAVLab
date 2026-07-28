# pen2compose

A focused **Pencil (`.pen`) → Jetpack Compose** transpiler. Reads a Pencil node
tree and emits an equivalent `@Composable`, so designs built in pen.dev can be
brought into the MAVLab Android app with high fidelity instead of hand-porting
from screenshots.

Prototype status: proven on the onboarding **failure panel** (`input/advanced_inputs.json`
→ `output/AdvancedInputsGenerated.kt`).

## Why

Hand-porting Pencil designs to Compose drifts (spacing, type, colour). This tool
maps the `.pen` node model to Compose 1:1 for the boring-but-exact parts —
layout, text, fills, borders, radii — and defers to real app components for the
instruments.

## Usage

```bash
python3 pen2compose.py <input.json> <output.kt> <ComposableName>
# e.g.
python3 pen2compose.py input/advanced_inputs.json output/AdvancedInputsGenerated.kt AdvancedInputsGenerated
```

`input.json` is a Pencil node tree. Get it from the Pencil MCP:

```
batch_get(filePath: "designs/mavlab-onboarding.pen", nodeIds: ["<frameId>"], readDepth: 12)
```

Save the returned JSON to `input/<name>.json` and run the converter. (The `.pen`
file is encrypted, so the tree is read through the MCP, not parsed directly.)

## Node mapping

| Pencil | Compose |
|---|---|
| `frame` layout vertical / horizontal / none | `Column` / `Row` / `Box` |
| `gap`, `padding`, `justifyContent`, `alignItems` | `Arrangement.spacedBy`, `padding`, arrangement, alignment |
| `width/height` = number / `fill_container` / `fit_content` | `.dp` / `weight(1f)` or `fillMaxWidth/Height` / wrap |
| `fill`, `cornerRadius`, `stroke` | `.background(shape)`, `RoundedCornerShape`, `.border` |
| `text` | `Text(text, fontSize, fontWeight, color, fontFamily)` |
| `ellipse` | `Box.clip(CircleShape).background(..)` |
| `icon` (lucide/material) | `Icon(Icons.Default.*)` via `LUCIDE_TO_MATERIAL` |
| `ref` (component instance) | mapped to a real composable via `COMPONENT_MAP` |

**Component mapping is the key idea:** a `ref` to a known reusable component
(e.g. the `PFD Instrument`) emits a call to the *real* app composable
(`AltitudeInstrument(...)`) rather than re-drawing it — so you get Pencil's
layout fidelity **and** live components. Extend `COMPONENT_MAP` in
`pen2compose.py`.

## Out of scope (flagged `// TODO`, polish by hand)

- Gradient / mesh / shader fills (emits solid or a TODO)
- `path` canvas art (SVG geometry)
- Drop shadows, blur, backdrop blur
- Exact font metrics (Inter vs system) — visual parity, not pixel-identical

## Colour convention

Pencil `#RRGGBBAA` → Compose `Color(0xAARRGGBB)` (alpha moved to front);
`#RRGGBB` → opaque `Color(0xFFRRGGBB)`.

## Next steps

1. Wire generated composables into `feature/onboarding` per frame.
2. Populate `COMPONENT_MAP` for `PFD Instrument`, `Drone Small`, pill buttons.
3. Batch mode: convert all 9 `Cur+` frames in one run.
4. Optional: fetch JSON directly via the Pencil CLI to remove the manual copy step.
