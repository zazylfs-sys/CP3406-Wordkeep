"""Export the illustration masters to the WebP drawables the app ships.

Usage (from the repository root):  python3 assets/illustrations/export.py

Each master is a 1024 px transparent PNG. The export trims the empty margin (keeping a 2% pad so
hatching is never clipped), scales the longest side to the size below and writes a lossy WebP with
a lossless alpha channel to app/src/main/res/drawable-nodpi/, which Android never rescales.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
MASTERS = ROOT / "assets/illustrations/masters"
OUT = ROOT / "app/src/main/res/drawable-nodpi"

# Longest side in px. 720 px covers a 240 dp slot at 3x density; small art needs less.
SIZES = {"illus_offline": 320, "illus_onboarding": 900}
DEFAULT_SIZE = 720


def export(master: Path) -> Path:
    image = Image.open(master).convert("RGBA")
    left, top, right, bottom = image.getchannel("A").getbbox()
    pad = round(max(image.size) * 0.02)
    image = image.crop((max(left - pad, 0), top - pad if top >= pad else 0,
                        min(right + pad, image.width), min(bottom + pad, image.height)))
    longest = SIZES.get(master.stem, DEFAULT_SIZE)
    scale = longest / max(image.size)
    image = image.resize((round(image.width * scale), round(image.height * scale)), Image.LANCZOS)
    target = OUT / f"{master.stem}.webp"
    image.save(target, "WEBP", quality=88, alpha_quality=100, method=6)
    return target


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    for master in sorted(MASTERS.glob("illus_*.png")):
        target = export(master)
        print(f"{target.relative_to(ROOT)}  {Image.open(target).size}  {target.stat().st_size // 1024} KB")
