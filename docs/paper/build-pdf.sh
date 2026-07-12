#!/usr/bin/env bash
# Rebuild the MAVLab paper PDF(s) from the markdown source.
# Requires: pandoc + weasyprint (installed under ~/.local/bin). No LaTeX/sudo needed.
#   ./build-pdf.sh          -> builds both single-column and two-column PDFs
#   ./build-pdf.sh 1col     -> single-column only
#   ./build-pdf.sh 2col     -> two-column (IEEE-style) only
set -euo pipefail
cd "$(dirname "$0")"
export PATH="$HOME/.local/bin:$PATH"

SRC="MAVLab - Phone-First Drone Digital-Twin Simulator.md"
BASE="MAVLab - Phone-First Drone Digital-Twin Simulator"
WANT="${1:-both}"

# strip internal YAML frontmatter (body carries title+byline); keep body '---' rules.
awk 'NR==1 && $0=="---"{fm=1; next} fm && $0=="---"{fm=0; next} !fm{print}' "$SRC" > .build_paper.md

# wrap the wide Appendix-A capability table in a .widetable div so the 2-column
# CSS can span it full-width (harmless in the 1-column build). Keeps the source .md clean.
python3 - <<'PY'
p=".build_paper.md"; L=open(p,encoding='utf-8').read().split('\n'); out=[]; i=0
WIDE = ('| MAV command', '| Bootcamp step')   # header rows of tables to span full-width
while i < len(L):
    if any(L[i].lstrip().startswith(h) for h in WIDE):
        j=i
        while j < len(L) and L[j].lstrip().startswith('|'): j+=1
        block = L[i:j]
        # if a section heading directly precedes the table, pull it into the
        # full-width block so heading + table stay together (no orphaned heading).
        head=[]; k=len(out)-1
        while k>=0 and out[k].strip()=='': k-=1
        if k>=0 and out[k].lstrip().startswith('## '):
            head=[out[k],'']; del out[k:]
        out += ['<div class="widetable">',''] + head + block + ['','</div>']; i=j
    else:
        out.append(L[i]); i+=1
open(p,'w',encoding='utf-8').write('\n'.join(out))
PY

render () {  # $1=output-suffix  $2..=extra --css args
  local out="$BASE${1}.pdf"; shift
  pandoc .build_paper.md \
    -f markdown+tex_math_dollars-implicit_figures \
    -t html5 -s --embed-resources \
    --css paper.css "$@" --resource-path=. \
    --metadata title="MAVLab v1.5.0 Technical Report" \
    --pdf-engine=weasyprint -o "$out"
  echo "Built: $out"
}

[ "$WANT" = both ] || [ "$WANT" = 1col ] && render ""
[ "$WANT" = both ] || [ "$WANT" = 2col ] && render " (2-column)" --css paper-2col.css

rm -f .build_paper.md
