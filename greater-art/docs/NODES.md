# Nodes: offline filename graph

## Navigation and controls

Stack, All songs (Library), and Nodes are pages 0, 1, and 2 of the same
HorizontalPager, with All songs as the initial page. The persistent navigation bar and
the pager share one position source.

On Nodes, an empty-area one-finger horizontal drag is intentionally left unconsumed by
the graph so the parent pager can swipe back to All songs from anywhere on empty canvas.
A one-finger drag that starts directly on a node moves only that node; the graph/map
itself cannot be manually panned. Two-finger pinch remains reserved for graph zoom and
does not add free map translation. Tap a node to play it. Fit resets the viewport and
temporary dragged positions; Playing may programmatically recenter the current node.
Find exposes filenames as searchable, accessible buttons.

### 1.9.18 display controls

Controls changes are saved locally when Apply is pressed (not on every slider tick).
They adjust minimum link strength, node size, edge opacity, labels, isolated nodes and
connection-weighted sizing; they are included in settings backup/reset. A node's visual
importance is log-scaled summed incident edge strength, recalculated after filtering.
The current track retains a double ring/play glyph even when isolated nodes are hidden.
Playing recenters on that node. The graph has a quiet opaque themed surface; hidden
wallpaper rendering is suspended when the pager settles here. These presentation controls
do not rewrite cached edges or repeat the force-layout calculation.

## Filename similarity

`graph/FilenameSimilarity.kt` uses physical filenames, never user display-title
overrides, tags, history, playlist membership or custom links. Strip a recognized
media extension, normalize NFKC, lowercase with Locale.ROOT, and turn punctuation into
word boundaries. Latin words remain tokens. Contiguous Han/hiragana/katakana/Hangul
runs remain whole tokens; this is not dictionary-based linguistic segmentation.
Character features operate on Unicode code points, including supplementary Han.
N-grams stay within runs rather than crossing punctuation.

```
score = 0.35 * token-set Jaccard
      + 0.30 * character-bigram multiset Dice
      + 0.10 * character-trigram multiset Dice
      + 0.20 * character-unigram multiset Dice
      + 0.05 * common-prefix-codepoints / max(name lengths)

Jaccard(A,B) = |intersection| / |union|
Dice(A,B) = 2 * sum(min(countA[g], countB[g])) / (totalA + totalB)
```

Identical nonempty normalized text scores 1; empty text scores 0. Caravan/Gambling
scores approximately 0.053333 through shared letters. This weak match is retained only
if it makes a node's top-six positive matches; stronger alternatives can displace it.
Each node selects six neighbors. The undirected union removes duplicate edges; a popular
node can have more than six incoming connections. Total edges are at most 6N.

## Work and memory bounds

`GraphBuilder` computes exact top-six matches up to 1,000 nodes. Above that, an inverted
token/bigram/character index proposes up to 192 candidates per node. Frequent postings
are deterministically rotated and sampled, so this large-library path is approximate,
not a guarantee of globally exact nearest neighbors. No dense N×N matrix is retained.

`GraphLayout` uses deterministic seeded positions, weighted springs, grid-based local
repulsion and 180 cooling steps on Dispatchers.Default. Stronger edges have shorter
target distances and stronger springs. Repulsion samples at most 32 nodes per nearby
grid cell. Isolated media remain visible. There is no continuous force solver on the
main thread or while the graph is hidden. Dragging moves the selected node without
restarting the full solve; those manual offsets are intentionally not persisted.

Canvas uses the app palette, culls offscreen nodes and caps ordinary labels. Selected
and playing nodes receive emphasis. No thumbnail decoding is launched for graph nodes.
Interaction performance on very large real-device libraries still requires profiling.

## Cache and ownership

`MainViewModel` owns graph state and cancels obsolete requests. Input preparation and
similarity/layout work are off-main; disk IO is on Dispatchers.IO. The graph is requested
lazily on the first visit, then refreshed after subsequent library scans. It includes all
scanned physical files independent of search/playlist filters; CUE virtual tracks are
collapsed to their physical source. Playing a source node plays that physical file.

`GraphRepository` holds one in-memory graph and an AtomicFile in app-private cache.
The SHA-256 key covers sorted IDs, raw filenames and the algorithm/layout version.
Renaming, adding or removing a file invalidates it. Metadata/audio-content changes that
do not affect filenames need no graph rebuild. Disk payload contains the fingerprint,
indices, edge weights and settled coordinates, not raw filenames or playback history.
Corrupt/truncated/mismatched caches are rejected and rebuilt. A cache-write failure
still leaves the newly calculated graph usable. No permissions or network access added.

## Regression coverage

Tests cover Latin/CJK/supplementary Han, normalization, weak letter matches, empty names,
score symmetry/bounds, sparse edge budgets, isolated nodes, deterministic layout, the
1,200-node approximate path, cache invalidation, round trips and corrupt cache rejection.
These are JVM tests, not a claim of phone frame-rate or gesture verification.
