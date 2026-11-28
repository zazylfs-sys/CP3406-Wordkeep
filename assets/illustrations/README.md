# Wordkeep illustrations

One illustration language for the whole app: **engraved editorial still-lifes**. They combine a modern
literary-magazine illustration with a vintage copperplate book plate and a hand-drawn herbarium study.

## Art direction

| | |
|---|---|
| Line | Fine engraved hatching, cross-hatching and stipple in ink green `#1F3A2E` |
| Fills | Flat spot colour under the line, slightly offset like a two-colour letterpress print |
| Palette | Ink green `#1F3A2E` · sage `#9DB29A` · cream paper `#F3ECDC` · ochre accents `#C99A3C` (sparing) |
| Motifs | Books, paper, pens, letterpress type, botanical sprigs (fern, olive, eucalyptus, ivy) |
| Mood | Literary, scholarly, calm, warm |
| Never | People or hands, cartoon or clip-art styles, gradients, glow, 3D, readable words, frames, rectangular backgrounds |

Every object has its own opaque fill and the background is transparent. The art therefore sits on the
tinted "plates" in light mode and on dark surfaces in dark mode without a white box. In dark mode
`BrandIllustration` dims the art to 86% so the cream fills do not glare.

## Files

| File | Scene | Used on |
|---|---|---|
| `illus_today_hero` | Open book, fountain pen, fern and olive | Today hero, work waiting |
| `illus_flashcards` | Fanned flash cards with a serif "A" | Today tile; Flashcard review header |
| `illus_meaning` | Magnifying glass over type-block puzzle pieces | Today tile; Meaning match header |
| `illus_spelling` | Headphones, sound-wave lines, letter blocks | Today tile; Listen & spell header |
| `illus_done` | Closed book, tea cup, potted olive | Today hero, up to date; session summary; nothing-due state |
| `illus_offline` | Oil lamp and notebook (simplified for small sizes) | Offline-mode notice; look-up in offline mode |
| `illus_empty_library` | Mostly empty shelf, blank notebook, ivy | Today hero with no words; Words and session empty states |
| `illus_stats` | Book stacks rising like a bar chart, with a sapling | Progress overview and empty state |
| `illus_onboarding` | Book growing a stem with letters for leaves | First launch |

Naming: `illus_<scene>` in snake case. The same name is used for the master, the prompt and the drawable.

## Folders

- `masters/` holds the 1024 × 1024 transparent PNG originals. Edit or replace these, never the exports.
- `prompts/style.txt` is the shared art-direction block, and `prompts/subjects.tsv` holds the per-scene subject lines. Together they are the full prompt for each image.
- `export.py` trims the empty margin and writes lossy WebP with lossless alpha. The longest side is 720 px (offline 560 px, onboarding 900 px). Output goes to `app/src/main/res/drawable-nodpi/`.

```sh
python3 assets/illustrations/export.py   # needs Pillow
```

The app reads only the exported drawables through the `Illustration` enum
(`ui/components/Illustrations.kt`), so every illustration is bundled and works offline.

## Origin

The images were generated on 28 Nov 2026 with the image-generation tool in OpenAI Codex CLI from the
prompts in `prompts/`. No reference images or existing artworks were supplied. They are declared in the
README's GenAI section.
