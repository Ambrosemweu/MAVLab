#!/usr/bin/env python3
"""pen2compose — a focused .pen node-tree -> Jetpack Compose transpiler.

Reads a Pencil node JSON tree (as returned by the Pencil MCP `batch_get`, or
hand-authored to match that schema) and emits an equivalent @Composable.

Scope: the node/property subset used by MAVLab's onboarding frames — frame
(vertical/horizontal/none), text, rectangle, ellipse, icon, and ref
(component instance). Gradients, path canvas art, shadows and blur are not
transpiled; those are flagged with a // TODO in the output for manual polish.

Usage:
    python3 pen2compose.py <input.json> <output.kt> <ComposableName>
"""
import json
import re
import sys

# Pencil reusable-component id -> real app composable call.
# Extend this map so instances render the live component, not a re-drawn copy.
COMPONENT_MAP = {
    "s61lH": ("AltitudeInstrument", None),   # PFD Instrument
    "BNoyV": ("Button", "PrimaryPill"),      # filled pill
    "awPEd": ("OutlinedButton", "GhostPill"),  # ghost pill
}

# lucide icon name -> Material icon (Icons.Default.*). Where the design mirrors an
# app tab, map to the real app icon (Analytics / ControlCamera / Height / Route / Settings).
LUCIDE_TO_MATERIAL = {
    "check": "Check", "wind": "Air", "x": "Close", "plus": "Add",
    "chevron-right": "ChevronRight", "route": "Route", "settings": "Settings",
    "chart-column": "Analytics", "gamepad-2": "ControlCamera",
    "arrow-up-down": "Height", "monitor": "Monitor", "smartphone": "PhoneAndroid",
}

FONT_WEIGHTS = {"normal": "FontWeight.Normal", "400": "FontWeight.Normal",
                "500": "FontWeight.Medium", "600": "FontWeight.SemiBold",
                "700": "FontWeight.Bold", "bold": "FontWeight.Bold"}


def color(hex_str):
    """#RGB / #RRGGBB / #RRGGBBAA  ->  Compose Color(0xAARRGGBB)."""
    h = hex_str.lstrip("#")
    if len(h) == 3:
        h = "".join(c * 2 for c in h)
    if len(h) == 6:
        h = "FF" + h                      # opaque
    elif len(h) == 8:
        h = h[6:8] + h[0:6]               # RRGGBBAA -> AARRGGBB
    return f"Color(0x{h.upper()})"


def is_transparent(fill):
    return isinstance(fill, str) and fill.lstrip("#").upper() in ("00000000",)


def shape(node):
    cr = node.get("cornerRadius")
    if cr is None:
        return None
    if isinstance(cr, list):
        return f"RoundedCornerShape({cr[0]}.dp, {cr[1]}.dp, {cr[2]}.dp, {cr[3]}.dp)"
    return f"RoundedCornerShape({cr}.dp)"


def modifiers(node, parent, include_fill=True):
    """Build the ordered Modifier chain for a node.

    include_fill=False for text (fill is the text color) and ellipse
    (background is applied after a clip), so they don't get a stray
    .background() from the node's fill.
    """
    m = []
    # sizing
    pl = (parent or {}).get("layout")
    w, hgt = node.get("width"), node.get("height")
    if w == "fill_container":
        m.append("weight(1f)" if pl == "horizontal" else "fillMaxWidth()")
    elif isinstance(w, (int, float)):
        m.append(f"width({w}.dp)")
    if hgt == "fill_container":
        m.append("weight(1f)" if pl == "vertical" else "fillMaxHeight()")
    elif isinstance(hgt, (int, float)):
        m.append(f"height({hgt}.dp)")
    # absolute offset (child of a layout:"none" parent)
    if (parent or {}).get("layout") in (None, "none") and parent is not None \
            and ("x" in node or "y" in node):
        m.append(f"offset({node.get('x', 0)}.dp, {node.get('y', 0)}.dp)")
    sh = shape(node)
    # background
    fill = node.get("fill")
    if include_fill and isinstance(fill, str) and not is_transparent(fill):
        m.append(f"background({color(fill)}" + (f", {sh})" if sh else ")"))
    # border
    if node.get("stroke") and node.get("strokeWidth"):
        sw = node["strokeWidth"]
        m.append(f"border({sw}.dp, {color(node['stroke'])}" + (f", {sh})" if sh else ")"))
    # padding (after background/border, like the app)
    pad = node.get("padding")
    if isinstance(pad, list) and len(pad) == 2:
        m.append(f"padding(horizontal = {pad[1]}.dp, vertical = {pad[0]}.dp)")
    elif isinstance(pad, list) and len(pad) == 4:
        m.append(f"padding(start = {pad[3]}.dp, top = {pad[0]}.dp, end = {pad[1]}.dp, bottom = {pad[2]}.dp)")
    elif isinstance(pad, (int, float)):
        m.append(f"padding({pad}.dp)")
    return "Modifier." + ".".join(m) if m else None


ARRANGE = {"start": "Start", "center": "Center", "end": "End",
           "space_between": "SpaceBetween", "space_around": "SpaceAround"}


def emit(node, parent, depth, out):
    pad = "    " * depth
    t = node.get("type")
    name = node.get("name", "")
    if t == "text":
        args = [f'text = "{node.get("content", "")}"']
        if "fontSize" in node:
            args.append(f'fontSize = {node["fontSize"]}.sp')
        if "fontWeight" in node:
            args.append(f'fontWeight = {FONT_WEIGHTS.get(str(node["fontWeight"]), "FontWeight.Normal")}')
        if node.get("fontFamily") == "Roboto Mono":
            args.append("fontFamily = FontFamily.Monospace")
        if "fill" in node:
            args.append(f'color = {color(node["fill"])}')
        m = modifiers(node, parent, include_fill=False)
        if m:
            args.append(f"modifier = {m}")
        out.append(f"{pad}Text({', '.join(args)})")
        return
    if t == "icon":
        icon = LUCIDE_TO_MATERIAL.get(node.get("icon", ""), "Circle")
        size = node.get("width", 16)
        out.append(f'{pad}Icon(Icons.Default.{icon}, contentDescription = null, '
                   f'tint = {color(node.get("fill", "#FFFFFF"))}, modifier = Modifier.size({size}.dp))')
        return
    if t == "ellipse":
        m = modifiers({**node, "cornerRadius": None}, parent, include_fill=False)
        base = m or "Modifier"
        out.append(f"{pad}Box({base}.clip(CircleShape).background({color(node.get('fill', '#FFFFFF'))}))")
        return
    if t == "ref":
        mapped = COMPONENT_MAP.get(node.get("ref"))
        label = mapped[1] if mapped else node.get("ref")
        out.append(f"{pad}// TODO ref -> {label or node.get('ref')}  (map in COMPONENT_MAP)")
        return
    # frame / rectangle. Pencil frames default to horizontal when `layout` is
    # omitted; only an explicit "none" means absolute (Box).
    layout = node.get("layout")
    comp = "Column" if layout == "vertical" else ("Box" if layout == "none" else "Row")
    args = []
    m = modifiers(node, parent)
    if m:
        args.append(f"modifier = {m}")
    if comp in ("Column", "Row"):
        main = "verticalArrangement" if comp == "Column" else "horizontalArrangement"
        if node.get("justifyContent"):
            args.append(f"{main} = Arrangement.{ARRANGE[node['justifyContent']]}")
        elif node.get("gap"):
            args.append(f"{main} = Arrangement.spacedBy({node['gap']}.dp)")
        if node.get("alignItems"):
            cross = "horizontalAlignment" if comp == "Column" else "verticalAlignment"
            al = node["alignItems"]
            val = {"Column": {"start": "Alignment.Start", "center": "Alignment.CenterHorizontally", "end": "Alignment.End"},
                   "Row": {"start": "Alignment.Top", "center": "Alignment.CenterVertically", "end": "Alignment.Bottom"}}[comp][al]
            args.append(f"{cross} = {val}")
    head = f"{comp}(" + (", ".join(args)) if args else f"{comp}("
    children = node.get("children", [])
    if not children:
        out.append(f'{pad}{head}) {{}}  // {name}')
        return
    out.append(f'{pad}{head}) {{  // {name}')
    for c in children:
        emit(c, node, depth + 1, out)
    out.append(f"{pad}}}")


HEADER = '''package com.ascend.mavlab.feature.onboarding.generated

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// AUTO-GENERATED by tools/pen2compose from a Pencil (.pen) node tree. Do not hand-edit;
// re-run the converter and polish gradients/canvas/shadows (flagged with // TODO).
'''


def main():
    if len(sys.argv) != 4:
        print(__doc__)
        sys.exit(1)
    inp, outp, comp_name = sys.argv[1], sys.argv[2], sys.argv[3]
    with open(inp) as f:
        root = json.load(f)
    if isinstance(root, list):
        root = root[0]
    body = []
    emit(root, None, 1, body)
    kt = HEADER + f"\n@Composable\nfun {comp_name}(modifier: Modifier = Modifier) {{\n" \
        + "\n".join(body) + "\n}\n"
    with open(outp, "w") as f:
        f.write(kt)
    print(f"Wrote {outp}  ({len(body)} lines of composable body)")


if __name__ == "__main__":
    main()
