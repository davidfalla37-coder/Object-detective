# Barcode Catalogue Improvement Plan

Status: isolated development work only. This document does not change the published Google Play app or deploy any backend service.

## What the current app does

- The native ML Kit scanner reads the barcode and passes the value and format into the WebView.
- The product-identification button sends the code and format to the existing `lookup-barcode` Supabase Edge Function.
- The interface displays `Checking product catalogues…` while that lookup runs.
- If the service returns no product, the UI reports that the barcode was read but no catalogue match was found.

This separates scanner success from catalogue lookup success. The 19 Crimes test is therefore a catalogue/lookup coverage case unless further evidence shows otherwise.

## Safe implementation approach

1. Keep all work on this development branch. Do not commit to `main`, change the published package, or deploy to the live Supabase project.
2. Locate and recover the current `lookup-barcode` Edge Function source and its project configuration before changing lookup behaviour. The function source is not currently present in this repository.
3. Add a server-side fallback provider for food and drink barcodes, initially evaluating Open Food Facts. Use its current v3 product-by-barcode API, send an identifying User-Agent from the server, respect rate limits, and cache results where appropriate.
4. Preserve the existing provider as the first lookup source until comparative tests show the fallback is safe.
5. Return provider/source, exact barcode, product name, brand, quantity/type, and a clear match status. Do not turn fuzzy text matches into exact barcode matches.
6. Keep an honest no-match result and offer photo/label-based identification as a separate fallback.
7. Check third-party data terms, attribution and privacy disclosures before release.

## Required tests before any release

- Known barcode returns the exact product and source.
- Unknown barcode produces a clear no-match result without crashing.
- Provider timeout, rate limit, malformed response and offline mode fail gracefully.
- EAN-8, EAN-13, UPC-A and UPC-E formatting/leading-zero cases are covered.
- Wine, spirits and non-alcoholic drinks are tested separately.
- Similar product variants are not silently treated as identical.
- Existing QR scanning and ordinary barcode reading remain unchanged.
- Preview APK uses an isolated application ID and preview backend only.

## Initial real-world regression case

- Product: 19 Crimes wine, bottle seen in a UK shop.
- Observation: scanner read the barcode, but the product was not identified.
- Exact barcode: not captured by the tester; do not invent it.
- Expected behaviour: query catalogue sources and either return a verifiable exact match or clearly report no match.
