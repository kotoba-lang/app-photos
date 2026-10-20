# app-photos（向き・capability）— itonami（dapp）上の分散型設計

状態: 第 1 スライス実装（2026-10-20）。正本はこの文書。
追跡: Linear JUN-128（親 JUN-116）。

## 位置づけ

app-photos は Photos —— カメラが吐いたメタデータ付きの画像。
**2 つの capability を分ける**: `fs/browse`（列挙）と `image/metadata`
（EXIF）。「フォルダの一覧だけが必要な選択器にすべての写真の撮影場所を
読む権限を与えてはならない」。

## このスライス: 向き・capability 判定コア

| 層 | 実体 | 状態 |
|---|---|---|
| 判定コア guest | `aiueos/photos_orient.cljk` — orientation-swap（**EXIF orientation 5-8 は幅高さ入れ替え**）/ grant-judgment（**browse と EXIF の過剰・不足を独立ビットで拒否**）/ capture-source（**EXIF capture time 優先**、無ければファイル時刻）/ command-admissible（open/quicklook/copy-path/export） | **実装済み・実測** |
| 記述子 | `aiueos/photos-orient.edn` | **実装済み** |
| parity | `test/app_photos/orient_parity_test.cljk` — **実物の model.cljk**（mokuroku+amu+kotoba-sema クラスパス）と一致（rotated? / displayed-dimensions / capability 分割 / commands） | **実装済み・実測** |

実測: `amu test aiueos/photos_orient.cljk` → **18/18**（jvm-kir / js / wasm）。
parity → 2 tests / 11 assertions / 0 failures。

## 正本の正確な規則（guest はこれに追従 —— 明文の設計意図つき）

- **EXIF orientation 5-8 で幅高さを入れ替える**: 「無視する viewer は縦位置
  の写真を横位置に報告し、アスペクト比駆動のレイアウトがすべて誤った
  スロットに置く」（preview の rotation 90/270/-90 と同根・別語彙）。
- **capability の分離**（monitor の process/metrics と同じ思想）: 一覧だけの
  選択器に EXIF（撮影場所）を与えない。guest の grant-judgment が過剰・不足
  を独立ビットで拒否。
- **撮影時刻**: EXIF capture time 優先、ファイル時刻へフォールバック。

## 実測メモ

- `rotated?` は**集合**（`#{5 6 7 8}`）—— `(rotated? 5)` は 5 を返す（真理値
  ではない）。parity の assert を contains?/nil? 形に訂正（実測で検出）。

## 未実装境界

- EXIF 読み取り（image/metadata の実体）と GPS 等の機微フィールドの扱い。
- album / source の UI 面判定。
- mon（DNA）化、aiueos session 載せ（fleet 共通）。
