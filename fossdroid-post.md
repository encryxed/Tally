# r/fossdroid post

## Title

Tally — offline receipt scanner that fills in the shop, date and total for you. No INTERNET permission at all. MIT.

## Body

I got tired of every receipt/expense app being a subscription that uploads photos of my shopping to someone else's server, so I wrote one that can't.

**What it does:** point the camera at a receipt, and it reads the shop name, the date and the total, picks a category, and adds it to your monthly spend. You don't type anything.

**The privacy bit is structural, not a promise.** The app holds no `INTERNET` permission. ML Kit's Play Services dependencies try to pull it into the merged manifest, so it's explicitly stripped with `tools:node="remove"`. Android itself won't let the app open a socket. `CAMERA` is the only permission it asks for. You can verify this yourself with `aapt dump permissions` on the APK rather than taking my word for it.

OCR is the bundled ML Kit Latin model — it ships inside the APK and runs on the phone's CPU, so there's no first-run download and it works in aeroplane mode.

**How the detection works,** since that's the actual work:

- *Shop* — matched against a dictionary of ~150 chains first. Failing that, scored by layout: large text, near the top, mostly letters, penalised for looking like an address, phone number, VAT ID or a price.
- *Total* — looks for labelled lines (`TOTAAL`, `TOTAL`, `TE BETALEN`, `GESAMT`, …) while vetoing `SUBTOTAAL`, VAT, discounts, cash tendered and change. Falls back to the largest amount in the lower half, flagged as a guess rather than silently trusted.
- *Date* — `dd-mm-yyyy`, `mm/dd/yyyy`, ISO, and written-out months in several languages. Ambiguous ones like `03/04` are resolved by your locale and marked as a guess, not a fact.
- Money parsing makes no locale assumption: whichever of `.` or `,` comes last is the decimal separator, so `1.234,56` and `1,234.56` both parse. Three-decimal currencies (dinars) are detected per receipt.

Every field carries a confidence, and anything low-confidence gets highlighted for you to check instead of quietly being wrong. Correct a shop name once and it remembers that till's layout for next time.

Also does budgets, monthly category breakdown, and CSV export via the system file picker.

**What it doesn't do:** no cloud sync, no accounts, no line-item parsing yet, Latin script only. It's v1.0 and I've tested it on my own receipts and a suite of 44 unit tests against realistic layouts — it will meet a till that confuses it, and I'd genuinely like to hear about it if it does.

Not on F-Droid: ML Kit is a proprietary Google dependency, so it isn't eligible for the main repo. Planning to submit to IzzyOnDroid. For now it's GitHub releases.

Kotlin, Jetpack Compose, Room, CameraX. minSdk 26. MIT licensed.

Source and APK: https://github.com/encryxed/Tally

---

## Notes before posting

- Check the subreddit rules and flair the post correctly (usually "App" or "Self-promotion").
- Put the demo GIF in the README first — it's what people click through to.
- Reply to comments for the first few hours; that's what drives the ranking.
- Do not cross-post everywhere the same day. Space r/degoogle and r/privacy out by a few days.
