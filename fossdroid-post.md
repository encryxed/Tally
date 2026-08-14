# r/fossdroid post

## Title

Tally — scans a receipt and fills in the shop, date and total for you. No INTERNET permission at all. MIT.

## Body

Every receipt app I could find was a subscription that uploads photos of my shopping to someone else's server, so I wrote one that can't.

Point the camera at a receipt and it reads the shop, the date and the total, picks a category and adds it to your monthly spend. You don't type anything.

**The privacy part is structural, not a promise.** The app holds no `INTERNET` permission. ML Kit's Play Services dependencies try to pull it into the merged manifest, so it's stripped back out with `tools:node="remove"`. Android won't let the app open a socket even if it wanted to. `CAMERA` is the only permission it asks for. Don't take my word for it — `aapt dump permissions` on the APK will tell you.

OCR is the bundled ML Kit Latin model, shipped inside the APK and run on the phone's own CPU. No first-run download, works in aeroplane mode.

**How the detection works**, since that's the actual work:

- *Shop* — matched against a dictionary of ~150 chains. Failing that, scored by layout: large text, near the top, mostly letters, penalised for looking like an address, phone number, VAT ID or a price. Every chain on the page is collected and ranked by prominence rather than taking the first one read, because a photo usually catches other text too and the receipt's own name is the one printed large.
- *Total* — looks for labelled lines (`TOTAL`, `TOTAAL`, `TE BETALEN`, `GESAMT`, …) while vetoing subtotals, VAT, discounts, cash tendered and change.
- *Date* — `dd-mm-yyyy`, `mm/dd/yyyy`, ISO and written-out months in several languages. Ambiguous ones like `03/04` are resolved by locale and reported as a guess rather than a fact.
- *Orientation* — the photo is read at all four rotations and the best-scoring parse wins. Every rule above is spatial, so a receipt lying sideways in frame otherwise produces not a worse answer but an arbitrary one.
- Money parsing assumes no locale: whichever of `.` or `,` comes last is the decimal separator, so `1.234,56` and `1,234.56` both work. Three-decimal currencies are detected per receipt.

Every field carries a confidence, and anything uncertain is flagged for you to check rather than quietly saved. If a photo loses the amount column — which happens, since labels are bold and figures are small — it reports the total as unreadable instead of reaching for some other number on the page. Correct a shop name once and it remembers that till's layout next time.

Also does budgets, monthly category breakdowns, and CSV export through the system file picker.

**Honest limitations.** It's v1.0. There are 57 unit tests covering the parser against realistic layouts, but it has been tried on only a handful of real tills, so it will meet one that confuses it — I'd genuinely like to hear about it when it does. No line-item parsing yet, no cloud sync by design, Latin script only. The APK is debug-signed, so it sideloads fine but won't update over a future release-signed build.

Not on F-Droid: ML Kit is a proprietary Google dependency, so it isn't eligible for the main repo. IzzyOnDroid submission is next. For now it's GitHub releases.

Kotlin, Compose, Room, CameraX. minSdk 26. MIT.

Source and APK: https://github.com/encryxed/Tally

---

## Before posting

- Flair it correctly — usually "App" or whatever the sub uses for releases.
- Reply to comments for the first few hours. That's what decides whether it ranks.
- Don't cross-post everywhere the same day. Leave r/degoogle and r/privacy a few days apart.
- Expect "why not Tesseract so it can go on F-Droid" — a fair question. Honest answer: Tesseract is markedly worse on receipt photos, and accuracy matters more here than repo eligibility. Worth saying plainly rather than dodging.
