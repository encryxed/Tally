# Tally

Point your camera at a receipt. Tally reads the **store**, the **date** and the
**total**, files it under a category, and adds it to your monthly spend — no
typing, no account, no subscription.

Fully open source, built by [@encryxed](https://github.com/encryxed).

## Why it's actually private

The shipped APK has **no `INTERNET` permission**. Not "we promise not to send
anything" — the permission isn't there, so the OS refuses to let the app open a
socket at all. Text recognition uses the *bundled* ML Kit Latin model, which
ships inside the APK and runs on the phone's own CPU.

This takes an explicit fight with the build: ML Kit's Play Services
dependencies inject `INTERNET` into the merged manifest on their own, so
[`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) strips it back out
with `tools:node="remove"`. Verify any build yourself with:

```bash
aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk
```

You should see only `CAMERA` and `ACCESS_NETWORK_STATE`. The latter is left in
place because it merely permits *reading* whether a network exists — with no
`INTERNET` permission, nothing can be transmitted regardless.

Photos and data live in the app's private storage. Nothing is ever uploaded,
because nothing can be.

## How the detection works

OCR gives you a bag of text lines with pixel positions. Turning that into
"Albert Heijn, 13 Aug, €16.31" is the actual work, and it lives in
[`parse/`](app/src/main/java/com/encryxed/tally/parse):

| Field | How it's found |
| --- | --- |
| **Store** | Matched against a dictionary of ~150 chains first. Failing that, scored by layout — big text, near the top, mostly letters, penalised for looking like an address, phone number, VAT ID or price. |
| **Total** | Looks for labelled lines (`TOTAAL`, `TOTAL`, `TE BETALEN`, `GESAMT`, …), while vetoing `SUBTOTAAL`, VAT, discounts, cash tendered and change. Falls back to the largest amount in the lower half, flagged as a guess. |
| **Date** | Handles `dd-mm-yyyy`, `mm/dd/yyyy`, ISO and written-out months in several languages. Ambiguous orders like `03/04` are resolved by your locale and reported as a guess rather than a fact. |
| **Category** | From the matched chain, else keyword-matched against the line items. |

Every field carries a confidence. Anything the parser isn't sure about is
highlighted on the review screen instead of being silently wrong.

### It learns the shops it gets wrong

Store names are the hardest field, so correcting one is permanent. The lookup
key is the parser's *guess*, not the right answer — the same till prints the
same layout every visit, so a shop that reads wrongly reads wrongly the same
way each time. Fix it once and every later scan of that shop is correct.

## Languages and currency

Two different settings, because they answer two different questions.

**App language** is what Tally itself is shown in — 21 languages, from the gear
icon. It is independent of the system language, and not limited to Latin script.

**Receipt languages** are the words the parser hunts for when finding the total
and the date (`TOTAL`, `TOTAAL`, `GESAMT`, `RAZEM`, `YHTEENSÄ`, …), across 20
Latin-script languages. Enable the ones your receipts are printed in; leaving
off languages you never encounter makes detection slightly sharper, since there
is less to collide with.

Only Latin script is covered here, because that is what the bundled OCR model
reads. Knowing the Greek for "total" would not help when the recogniser cannot
see the letters — so the app can be *displayed* in Russian or Ukrainian, but it
cannot *read* a Cyrillic receipt.

**Currency** is detected from the receipt when it is printed there, falls back
to a default you set, and remains editable on any individual receipt.

Translations were produced without native review. Corrections are very welcome.

## Budgets

Set a weekly (Mon–Sun) or monthly (calendar month) cap from the gear icon.
The home screen then shows spend against it, what's left, and a per-day
allowance for the days remaining — turning amber near the limit and red past
it.

Money parsing deliberately doesn't assume a locale: whichever of `.` or `,`
comes **last** is treated as the decimal separator, so `1.234,56` and `1,234.56`
both come out as `1234.56`.

## Building

Requires JDK 17+ and an Android SDK with platform 35.

```bash
gradle assembleDebug          # or ./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

Install it with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tests

The parser is pure Kotlin with no Android dependencies, so the detection logic
is tested on the JVM against realistic receipt layouts:

```bash
gradle testDebugUnitTest
```

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · CameraX · ML Kit text
recognition (bundled, offline) · minSdk 26

## Licence

[MIT](LICENSE) © [encryxed](https://github.com/encryxed) — free to use, fork
and modify. If it saves you some typing, a star is appreciated.
