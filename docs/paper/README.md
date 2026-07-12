# MAVLab Technical Report

**MAVLab: A Phone-First Drone Digital-Twin Simulator for Flattening the Drone-Systems Learning Curve**
Ambrose Mweu Kioko and James Wainaina Kaira — Ascend Labs, Ascend Drone Technologies Ltd., Nairobi, Kenya (2026).

- 🌐 [Published version](https://labs.fly-ascend.com/mavlab-technical-paper.pdf) — on the Ascend Labs website
- 📄 [`mavlab-technical-report.pdf`](mavlab-technical-report.pdf) — the two-column PDF (in-repo copy)
- 📝 [`paper.md`](paper.md) — the markdown source
- 🖼️ [`figures/`](figures/) — architecture diagram and app-surface screenshots

The report covers the drone-education *tooling cliff*, the phone-first architecture (physics, autopilot, and MAVLink server all on-device), the protocol-first engineering methodology, the MAVLink/mission capability surface, and live QGroundControl acceptance results for release v1.5.0.

## Citing

See [`CITATION.cff`](../../CITATION.cff) at the repository root, or use GitHub's **"Cite this repository"** button.

## Building the PDF

`build-pdf.sh` regenerates the PDF from `paper.md` (uses `paper.css` / `paper-2col.css`). See the script header for dependencies.
