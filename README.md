# app-photos

**Photos, on [`mokuroku`](https://github.com/kotoba-lang/mokuroku).** An album
of images with the metadata that came out of the camera.

Design: [ADR-2608035000](https://github.com/com-junkawasaki/root/blob/main/90-docs/adr/2608035000-app-standard-application-suite-on-a-shared-catalog-kernel.edn).

## Two capabilities

`fs/browse` finds the files; `image/metadata` opens them far enough to read
EXIF. Separate because a picker that only needs to list a folder should not
also be able to read where every photo was taken.

That split produces a third state most photo apps do not model: **fs/browse
granted, image/metadata refused**. Every file is listed and nothing is known
about any of them, so sorting by date is meaningless. `source/metadata-only-denied`
is that state, and it is neither "denied" nor "working".

## A fallback date says it is a fallback

`:captured` prefers the EXIF capture time and falls back to the file's
modification time — and records which it used in `:time-source`. Rows dated
from the filesystem carry a **File date** badge.

Silently mixing the two produces a timeline where copied files jump to today.
That is the single most common way a photo library sorts wrongly, and it is
invisible precisely because the app looks like it is working.

## Orientation is applied before dimensions are reported

EXIF orientations 5–8 swap width and height. A viewer that ignores this reports
a portrait photo as landscape, and every aspect-ratio-driven layout puts it in
the wrong slot.

## Test

```sh
kbb -M:local:test
kbb -M:lint
```

design-quality: 100.00 on album / selection / awaiting-grant (2026-08-03).
