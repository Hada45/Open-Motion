# Changelog

## 2026-10-23 — Timeline Ruler: Start-of-Project Gap

- Cover the leading blank area to the left of the time ruler at 0 seconds and the first frames, so vertically scrolled layer headers cannot show through above the track boundary.
- Let that area receive ruler input rather than selecting the hidden layer underneath. Retain the previous stacking-order correction, zero/tick positions, horizontal scrolling and playhead visibility.
- Refresh the PWA shell-cache identity for this build.
- Validation: 166 scoped browser checks passed in four desktop/mobile layouts, including the entire leading gap at 0 seconds, frames 1 and 10, later times, vertical scrolling, ruler input and layer controls. The v1 gap was reproduced. Physical phones were not tested.

## 2026-10-23 — Timeline Ruler Visibility

- Keep sticky layer headers below the fixed time ruler when vertically scrolling the timeline, including the left side during horizontal scrolling.
- Preserve playhead visibility, ruler seeking, horizontal timeline zoom/pan, and layer controls. Only the header stacking order changes; project data and application scripts are unchanged.
- Refresh the PWA shell-cache identity for this build.
- Validation: 181 scoped browser checks passed across two portrait phone sizes, mobile landscape and desktop, including real touch gestures, ruler seeking, layer visibility controls and animation-data preservation. No uncaught page errors. Physical phones were not tested.

## 2026-10-23 — AM Template Workflow, Effect Geometry & Element Labels

### AM Link starts in Template Editor

- After a successful AM Link download/import, close the import dialog and open the existing Template Editor automatically, for official, premium and third-party resolver choices.
- Replace shared photo, video and audio sources before continuing through **Lanjut → Buka Timeline** or **Ekspor**. Retain clip timing, keyframes, effects and shared-source slot grouping.
- Update the import button, description and Home tooltip to explain this flow. Failed/cancelled imports remain in their existing dialogs; reopening a saved project, other file imports and missing-media recovery do not gain a new automatic Template Editor step.

### Native AM layer geometry

- Correct the shared shader contract: `acLayerSize` is the untransformed primitive/content size, not the extent after layer or parent scaling, rotation or mirroring.
- Keep logical layer/scene sizes, centers and motion vectors separate from preview raster buffers, FBOs and texture dimensions. Preserve shader source, imported values and keyframes.
- Fix the Turbulent Displace pattern distortion in the supplied Renaxprst 9:16 project. The shared correction also covers live size readers such as Tiles, Noise, Displacement Map, Fractal Warp and procedural patterns.
- Audit 315 registered effect definitions: 104 have live layer-size consumers in WebGL1/2; three other Swirl definitions have unused size expressions optimized out by the compiler. This is not a claim of complete native Android effect parity.

### Element labels and PWA refresh

- Restore **Kamera**, **Objek Null**, **Grup Kosong** and **Elemen** labels. Limit icon-only text hiding to the actual shape grid; retain readable icon-above-text object cards and scrolling in short panels.
- Refresh the offline shell cache for this release without changing imported media or project storage.

### Verification and limitations

- 64 local import-flow/UI checks pass on the new HTML in isolated Chrome desktop and mobile emulation, covering all three resolver choices, delayed completion, errors/cancellation, project selection, empty templates, media replacement and returning to/reopening the timeline. Resolver responses are mocked; these are not live premium-service or physical-phone tests.
- The retained geometry implementation was separately verified with 642 WebGL1/2 graph variants, 48 supplemental variants, 1,544 numeric contract assertions, 12 user-project frame cases, 16 real local/composition/parent/group/animated-bounds route cases and 295 UI regressions. Its shader/runtime changes are preserved unchanged by the import-flow update.
- Physical iPhone/iPad/Android GPUs, full MP4 encoding/muxing and full native AM visual parity remain unverified. This release does not include the unfinished V5.300/V5.301 experiments or fix the separate AM effect-readiness export failure.

## 2026-10-23 — Home Banner, Media Recovery & Preview Warnings

Previously published in [ed642c8](https://github.com/Hada45/Open-Motion/commit/ed642c87ed7629581eb9cfb6d317cf803b78859b); this entry backfills the missing changelog.

- Fix the giant desktop download icon caused by install-banner CSS being limited to mobile media queries. Keep the banner compact, dismissible and hidden in standalone mode.
- Let media recovery explicitly choose premium, official anonymous (5 MB maximum package), or third-party resolver, with provider-specific notes and no automatic provider switching.
- Recover media without reimporting layers or changing IDs, animations, effects, trim or relationships. Reject stale recovery results after switching projects; commit the resolver choice only after media is complete and recovery starts.
- Add a focusable dismiss button and readable controls to preview warnings. Clear recovered/deleted/hidden/replaced media errors, ignore late events from obsolete sources and avoid video-codec advice for image failures.
- Historical validation: 121 added checks and 755 scoped checks across the then-published build. Live resolver downloads, physical iPhone/iPad/Infinix and full 4K export were not verified. The static 1080p/4K encoder probe was diagnostic evidence only; no encoder fallback policy was changed.

## 2026-10-23 — Mobile Compatibility, Resizable Panels & Freehand

Previously published in [1a25e6f](https://github.com/Hada45/Open-Motion/commit/1a25e6fb7e94dabe07695ad4247eca2b40db5dc9); this entry backfills the missing changelog.

- Add separate photo, video, audio and Files picker paths, safer MIME fallback and bounded media-probe cleanup for browser/device compatibility.
- Guard video/audio proxy ownership, pending playback and background teardown to prevent overlapping playback.
- Keep the timeline visible alongside responsive inspector/Add panels. Center resize grips inside existing panel headers, remember per-layout sizes, and let Add content fill the resized area with vertical overflow and retained shape paging.
- Add adjustable freehand stroke smoothing, optional stylus-pressure width and single-tap dots as editable vector outlines, with Undo/Redo and XML persistence.
- Keep Bezier handles reachable without rewriting curve overshoot. Expose GPU/media failures and retain a limited 2D startup fallback.
- Preserve community PWA, AM share/export, project thumbnails and keyboard-shortcut contributions and the prior export-resolution fixes.
- Historical validation: 634 scoped local checks on the then-published build. Physical iPhone/iPad/Infinix, hardware stylus and complete video export remain unverified; not every black-preview case is confirmed fixed.

## 2026-10-22 — Export Resolution & Diagnostics

Published in [ee4e288](https://github.com/Hada45/Open-Motion/commit/ee4e2886ce4bea60a2793ff73410122ffab33f62).

### Export resolution detection

- Fix the misleading 480p ceiling: GPU Fast and the other WebCodecs export engines now query their encoder configuration instead of being limited by Media Capabilities recording results.
- Match capability checks to the selected engine, output dimensions, FPS, quality bitrate and encoder preference. Recheck support after engine, codec, FPS or quality changes.
- Preserve an existing resolution choice when it remains available, debounce quality-slider checks and ignore stale asynchronous responses.
- Distinguish unsupported configurations from unavailable, failed or timed-out capability queries. Unknown choices are explicitly marked unverified rather than reported as a detected device limit.
- Remove the fabricated low-resolution maximum when every configuration is rejected; ask for another engine, codec or FPS instead. Unverified options up to 4K are not a guarantee of successful export.

### Diagnostics and export UI

- Retain detailed capability matrices, optional user-reported device/browser information and GPU effect-readiness failure context in the Diagnostics panel.
- Remove the direct "Diagnostik resolusi & GPU Fast" shortcut from the export sheet.
- Capability queries do not instantiate encoders, decoders or additional WebGL contexts. Codec profiles, rendering algorithms and output quality remain unchanged.

### Verification and limitations

- 345 local checks passed: 75 resolution-policy checks, 80 diagnostics checks and 190 UI/keyboard/community-feature regression checks in desktop/mobile-size Chrome and WebGL1 contexts.
- Capability responses used by the tests are fixtures, not measurements from an iPhone 17. Physical Safari/iPhone and complete video exports remain unverified; a supported configuration is not proof that a full project will export successfully.
- The separate AM effect-readiness failure is not fixed by this change. Unfinished V5.300/V5.301 native-effects experiments are not included.

## V5.285 — 2026-10-19

### Mobile & PWA UX Refinements

- **Add Object Drawer Physics**: smooth slide-down close animations (`.drawerClosing`) for mobile bottom sheets and desktop docked trays; prevent stuck states and race conditions upon rapid toggle.
- **Experimental Notice Suppression**: add persistent "Jangan tampilkan lagi" / "Don't show again" secondary action with synchronous `localStorage` verification and 14-language translations.
- **Projects Tab Empty State**: high-contrast dark theme empty state with styled icon card (`#18202e`, `#73e2bd` accent) and clear typographic hierarchy (`#f1f5f9` header, `#94a3b8` description).
- **iOS Safari Viewport Docking**: switch mobile browser viewport height to `100dvh` to keep bottom navigation cleanly above the Safari URL bar, while preserving edge-to-edge `100lvh` for standalone Home Screen PWAs.
- **Android PWA Viewport Alignment**: restrict hardware screen scaling to iOS and standardize non-iOS standalone PWAs on `100dvh`, preventing bottom navigation bars from sinking beneath Android system buttons or gesture navigation areas.
- **Navbar Polish**: high-contrast white text for language selector badge ID in mobile navigation bar.

## V5.284 — 2026-10-19

### Alight Motion UX Alignment & Add Object Drawer Overhaul

- Redesign Add Object (`+`) popup drawer matching Alight Motion v5.0.273 (`activity_edit_addpopup.xml`):
  - 5 categorical top tabs (Shapes, Elements, Media, Audio, Templates) with AM icon assets and active pill indicators.
  - Dedicated right-side quick-add action rail (Freehand Drawing, Vector Drawing, Text, and Close).
  - Clean responsive layouts: mobile bottom sheet with 18px top radius and desktop docked tray below the timeline.
- Implement parametric vector shape tracing for primitive shapes: plus, heart, pie, and crescent/moon.
- Expand export frame rates up to 240 FPS (15, 18, 20, 24, 25, 30, 48, 50, 60, 90, 120, 144, and 240 FPS), with auto-sync of current project FPS upon opening the export sheet.
- Refine timeline splitter bar, playback transport controls, and responsive preview surface sizing.

## V5.283 — 2026-10-17

This update brings the tested single-file experimental build to main, including
the keyboard-shortcuts contribution from [PR #33](https://github.com/Hada45/Open-Motion/pull/33)
by [anonimBRO-design](https://github.com/anonimBRO-design). The app remains a prototype.

### Configurable keyboard shortcuts

- Open the categorized guide with the editor's ? button, F1, or ?.
- Remap 13 actions with Ctrl/Cmd, Shift, and Alt; conflicting combinations cannot be saved.
- Persist per-browser settings and restore defaults. Retain the contribution's nine UI languages.
- Keep typing/IME isolated from editing commands, restore dialog focus, and respect the active group timeline when seeking.

### Layer groups, appearance and audio

- Dedicated group timelines with breadcrumb navigation, ordinary groups, Mask and Exclude groups, and AM Link group import.
- Ungrouping retains child layers and the supported Mask/Exclude relationships.
- Inspector border and shadow controls, audio spectrum, and reusable audio-spectrum presets.
- Preserve existing tracking, Text Animator and project-thumbnail features, including credit for the PR #32 contribution.

### Experimental Alight Motion compatibility

- Expanded native/script, drawing, path, text, repeat and GPU effect integration with editable imported parameters/keyframes.
- WebGL1/2 compatibility and effect-graph, blur/bounds and procedural-kernel improvements.
- Scatter Repeat parameter/parent caching avoids repeated metadata work without reducing source copy counts or replacing 3D with 2D.
- Improved native Float parameter/default and Hz/RPM accumulation boundaries, waveform/noise/script-return handling, and script environment/Timecode serialization.
- Preserve AM XML source ordering, nested-scene ownership, transform parents and roundtrip data.

### Verification and limitations

- V5.283 passes 52 keyboard/UI checks, nine languages, 190-layer XML roundtrip with 117 source-owner references, 96 effect call-site controls and 50 original 3D Scatter time points across WebGL 1/2.
- Its renderer/effect definitions remain unchanged from verified V5.281. All 108 earlier suites were not rerun for this UI-only update.
- These are scoped desktop controls, not full-project phone FPS or proof of native Android parity. Effect-specific gaps, nonlinear easing, some source-clock/retiming, camera/parent, typography, media and export contracts remain under development.
- The unfinished V5.282 live source-clock draft is NOT included. Full Alight Motion effect parity is not claimed.
- Refresh the PWA shell-cache version while leaving imported media and project storage untouched.
- README is unchanged.

## V5.238 — 2026-10-11

This update publishes the previously unreleased V5.238 work and carries forward
the project-thumbnail contribution merged in PR #32. The app remains a prototype.

### Tracking (experimental)

- Motion 2D: Point, Two Points, Planar, Corner Pin, and Stabilizer modes with editable baked keyframes.
- Camera 3D: experimental camera analysis, re-solving saved observations, and camera/Null placement.
- Playback displays tracked observations as `+` markers. Select a continuous track to create a Null for parenting other layers.
- Supports source-plane tracking for footage placed in 3D Space, as well as 2D observation Nulls.
- Adds continuous-lifetime filters and optional poor-to-excellent quality colors. Display filtering leaves the saved analysis and existing Null keyframes unchanged.
- New analyses record measured matching quality; older saved tracks use labeled estimates until reanalyzed.

### Text Animator

- Character, word, line, and whole-text targets with independent range selectors.
- XYZ position/rotation, scale, pivots, styling, paths, and restricted expressions.
- 130 built-in presets, including continuous 3D animation, with editable keyframes and easing.
- Custom presets and reusable effect packages with local JSON import/export.
- Per-character glyph planes can use the scene camera and parent transforms in 3D Space.

### Home project thumbnails

- Real saved-preview thumbnails on project cards, adapted from [PR #32](https://github.com/Hada45/Open-Motion/pull/32) by [anonimBRO-design](https://github.com/anonimBRO-design).
- Bounded thumbnail capture from an eligible paused preview without starting additional rendering or decoding.
- Thumbnail persistence, invalid-image fallback, and compatibility with older projects. If no valid preview is available, the previous thumbnail is retained.
- Preserves the original contribution credit and merge history.

### Current limitations

- Tracking is experimental and depends on footage quality and browser/device capability. A video-plane Null does not reconstruct physical depth inside the recording; missing observations are not fabricated across gaps.
- Quality colors express relative matching quality, not accuracy probabilities. Old tracks require reanalysis for measured scores.
- 3D text uses flat glyph planes without extrusion. Some effects use a compatibility path that flattens per-character depth.

### PWA update

- Refreshes the offline-shell cache version while retaining network-first navigation and leaving imported media and project storage untouched.


