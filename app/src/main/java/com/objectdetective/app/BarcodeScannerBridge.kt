package com.objectdetective.app

import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.net.URLEncoder
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class BarcodeScannerBridge(
    private val activity: Activity,
    private val webView: WebView
) {
    @JavascriptInterface
    fun startScan() {
        activity.runOnUiThread {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_PDF417,
                    Barcode.FORMAT_AZTEC
                )
                .enableAutoZoom()
                .build()

            GmsBarcodeScanning.getClient(activity, options)
                .startScan()
                .addOnSuccessListener { barcode ->
                    val value = barcode.rawValue.orEmpty()
                    if (value.isNotBlank()) {
                        val format = formatName(barcode.format)
                        webView.post {
                            webView.evaluateJavascript(
                                "window.onNativeBarcodeScanned(${JSONObject.quote(value)}, ${JSONObject.quote(format)})",
                                null
                            )
                        }
                    }
                }
                .addOnCanceledListener {
                    webView.post {
                        webView.evaluateJavascript(
                            "window.onNativeBarcodeError('Scan cancelled.')",
                            null
                        )
                    }
                }
                .addOnFailureListener {
                    webView.post {
                        webView.evaluateJavascript(
                            "window.onNativeBarcodeError('The scanner could not read that code. Please try again in better light.')",
                            null
                        )
                    }
                }
        }
    }

    /**
     * Read-only secondary barcode lookup. Kept native so we can send the
     * identifying User-Agent requested by Open Food Facts without exposing
     * any API credentials or changing the production lookup service.
     */
    @JavascriptInterface
    fun lookupOpenFoodFacts(code: String, format: String) {
        val allowedFormats = setOf("EAN_13", "EAN_8", "UPC_A", "UPC_E", "UPC-E")
        val digits = code.trim()
        if (format !in allowedFormats || !digits.matches(Regex("^[0-9]{8,14}$"))) {
            postOpenFoodFactsResult(
                JSONObject()
                    .put("found", false)
                    .put("message", "This additional catalogue supports standard retail EAN/UPC barcodes only.")
            )
            return
        }

        Thread({
            var connection: HttpURLConnection? = null
            val result = try {
                val endpoint = URL(
                    "https://world.openfoodfacts.org/api/v3/product/${digits}.json?product_type=all"
                )
                connection = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7000
                    readTimeout = 9000
                    instanceFollowRedirects = true
                    setRequestProperty(
                        "User-Agent",
                        "ObjectDetective/1.0 (barcode lookup; https://github.com/davidfalla37-coder/Object-detective)"
                    )
                    setRequestProperty("Accept", "application/json")
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    JSONObject()
                        .put("found", false)
                        .put("message", "The additional catalogue is temporarily unavailable (HTTP $responseCode).")
                } else {
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val payload = JSONObject(body)
                    val product = payload.optJSONObject("product")
                    val returnedCode = product?.optString("code").orEmpty()
                    val exactBarcodeMatch = returnedCode == digits
                    if (payload.optInt("status", 0) == 1 && product != null && exactBarcodeMatch) {
                        val name = product.optString("product_name").ifBlank {
                            product.optString("product_name_en")
                        }
                        JSONObject()
                            .put("found", name.isNotBlank())
                            .put(
                                "product",
                                JSONObject()
                                    .put("name", name)
                                    .put("brand", product.optString("brands"))
                                    .put("type", product.optString("categories"))
                                    .put("quantity", product.optString("quantity"))
                                    .put("code", returnedCode)
                            )
                            .put("source", "Open Food Facts")
                            .put("sourceUrl", "https://world.openfoodfacts.org/product/$returnedCode")
                            .put("message", if (name.isBlank()) "The barcode exists, but the catalogue has no product name." else "")
                    } else {
                        JSONObject()
                            .put("found", false)
                            .put("message", "No exact barcode record was found in Open Food Facts.")
                    }
                }
            } catch (_: Exception) {
                JSONObject()
                    .put("found", false)
                    .put("message", "Open Food Facts could not be reached. Please try again later.")
            } finally {
                connection?.disconnect()
            }
            postOpenFoodFactsResult(result)
        }, "ObjectDetective-BarcodeLookup").start()
    }

    /**
     * Exact ISBN lookup for books. Google Books is checked first and every
     * result must explicitly contain the scanned ISBN. Open Library is a
     * second edition-level source; neither source is allowed to fuzzy-match.
     */
    @JavascriptInterface
    fun lookupBookByIsbn(isbn: String) {
        val code = isbn.trim().replace("-", "").replace(" ", "").uppercase()
        if (!code.matches(Regex("^(?:97[89][0-9]{10}|[0-9]{9}[0-9X])$"))) {
            postBookIsbnResult(JSONObject().put("found", false).put("message", "Enter a valid-looking ISBN-10 or ISBN-13."))
            return
        }

        Thread({
            val result = try {
                lookupGoogleBooksIsbn(code) ?: lookupOpenLibraryIsbn(code)
                    ?: JSONObject().put("found", false).put("message", "No exact ISBN record was found in Google Books or Open Library.")
            } catch (_: Exception) {
                JSONObject().put("found", false).put("message", "Book catalogues could not be reached. Please try again later.")
            }
            postBookIsbnResult(result)
        }, "ObjectDetective-BookLookup").start()
    }

    private fun lookupGoogleBooksIsbn(isbn: String): JSONObject? {
        val url = URL("https://www.googleapis.com/books/v1/volumes?q=isbn%3A$isbn&maxResults=5")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 9000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ObjectDetective/1.0")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) return null
            val payload = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            val items = payload.optJSONArray("items") ?: return null
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val info = item.optJSONObject("volumeInfo") ?: continue
                val identifiers = info.optJSONArray("industryIdentifiers") ?: continue
                val exact = (0 until identifiers.length()).any { index ->
                    identifiers.optJSONObject(index)?.optString("identifier")?.replace("-", "") == isbn
                }
                if (!exact) continue
                val authorsArray = info.optJSONArray("authors")
                val authors = if (authorsArray != null) {
                    (0 until authorsArray.length()).mapNotNull { authorsArray.optString(it).takeIf(String::isNotBlank) }.joinToString(", ")
                } else ""
                val title = info.optString("title")
                if (title.isBlank()) continue
                val product = JSONObject()
                    .put("name", title)
                    .put("author", authors)
                    .put("publisher", info.optString("publisher"))
                    .put("publishedDate", info.optString("publishedDate"))
                    .put("edition", info.optString("edition"))
                    .put("format", info.optString("printType"))
                    .put("pages", if (info.has("pageCount")) info.optInt("pageCount").toString() else "")
                    .put("code", isbn)
                return JSONObject()
                    .put("found", true)
                    .put("product", product)
                    .put("source", "Google Books")
                    .put("sourceUrl", "https://books.google.com/books?vid=ISBN$isbn")
            }
            return null
        } finally {
            connection.disconnect()
        }
    }

    private fun lookupOpenLibraryIsbn(isbn: String): JSONObject? {
        val url = URL("https://openlibrary.org/isbn/$isbn.json")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 9000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ObjectDetective/1.0 (ISBN lookup)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) return null
            val edition = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            val title = edition.optString("title")
            if (title.isBlank()) return null
            val authorsArray = edition.optJSONArray("authors")
            val authors = mutableListOf<String>()
            if (authorsArray != null) {
                for (i in 0 until minOf(authorsArray.length(), 3)) {
                    val key = authorsArray.optJSONObject(i)?.optJSONObject("key")?.optString("key")
                        ?: authorsArray.optJSONObject(i)?.optString("key").orEmpty()
                    if (key.startsWith("/authors/")) {
                        val authorConnection = (URL("https://openlibrary.org$key.json").openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 4000
                            readTimeout = 5000
                            setRequestProperty("User-Agent", "ObjectDetective/1.0 (ISBN lookup)")
                        }
                        try {
                            if (authorConnection.responseCode in 200..299) {
                                val author = JSONObject(authorConnection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                                author.optString("name").takeIf(String::isNotBlank)?.let(authors::add)
                            }
                        } finally {
                            authorConnection.disconnect()
                        }
                    }
                }
            }
            val publishers = edition.optJSONArray("publishers")
            val publisher = publishers?.optString(0).orEmpty()
            val product = JSONObject()
                .put("name", title)
                .put("author", authors.joinToString(", "))
                .put("publisher", publisher)
                .put("publishedDate", edition.optString("publish_date"))
                .put("edition", edition.optString("edition_name"))
                .put("format", edition.optString("physical_format"))
                .put("pages", if (edition.has("number_of_pages")) edition.optInt("number_of_pages").toString() else "")
                .put("code", isbn)
            return JSONObject()
                .put("found", true)
                .put("product", product)
                .put("source", "Open Library")
                .put("sourceUrl", "https://openlibrary.org/isbn/$isbn")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Runs on-device OCR on a user-selected book-cover image. The image is
     * processed locally; only extracted title/author text is used for lookup.
     */
    @JavascriptInterface
    fun recognizeBookCoverImage(dataUrl: String) {
        Thread({
            val result = try {
                val encoded = dataUrl.substringAfter("base64,", "")
                require(encoded.isNotBlank() && encoded.length <= 18_000_000) { "Image is empty or too large." }
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 2000 || bounds.outHeight / sample > 2000) sample *= 2
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?: throw IllegalArgumentException("The selected cover image could not be read.")
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0))).text
                    JSONObject().put("success", true).put("text", text)
                } finally {
                    recognizer.close()
                    bitmap.recycle()
                }
            } catch (_: Exception) {
                JSONObject().put("success", false).put("message", "Text could not be read from that cover. Try a clearer, front-on photograph.")
            }
            webView.post {
                webView.evaluateJavascript("window.onBookCoverOcrResult && window.onBookCoverOcrResult($result)", null)
            }
        }, "ObjectDetective-BookOCR").start()
    }

    /**
     * Searches by OCR/manual title and author. These are candidate suggestions,
     * never exact edition matches because the ISBN was not verified.
     */
    @JavascriptInterface
    fun searchBooksByTitleAuthor(title: String, author: String) {
        val cleanTitle = title.trim().take(180)
        val cleanAuthor = author.trim().take(120)
        if (cleanTitle.length < 2) {
            postBookTitleSearchResult(JSONObject().put("success", false).put("message", "Enter at least part of the book title."))
            return
        }
        Thread({
            val result = try {
                val candidates = org.json.JSONArray()
                val query = buildString {
                    append("intitle:")
                    append(URLEncoder.encode(cleanTitle, "UTF-8"))
                    if (cleanAuthor.isNotBlank()) {
                        append("+inauthor:")
                        append(URLEncoder.encode(cleanAuthor, "UTF-8"))
                    }
                }
                val google = URL("https://www.googleapis.com/books/v1/volumes?q=$query&maxResults=5")
                val connection = (google.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000; readTimeout = 9000
                    setRequestProperty("User-Agent", "ObjectDetective/1.0")
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    if (connection.responseCode in 200..299) {
                        val payload = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                        val items = payload.optJSONArray("items")
                        if (items != null) for (i in 0 until minOf(items.length(), 5)) {
                            val info = items.optJSONObject(i)?.optJSONObject("volumeInfo") ?: continue
                            val bookTitle = info.optString("title")
                            if (bookTitle.isBlank()) continue
                            val authors = info.optJSONArray("authors")
                            val authorNames = if (authors != null) (0 until authors.length()).map { authors.optString(it) }.filter { it.isNotBlank() }.joinToString(", ") else ""
                            candidates.put(JSONObject()
                                .put("title", bookTitle)
                                .put("author", authorNames)
                                .put("publisher", info.optString("publisher"))
                                .put("publishedDate", info.optString("publishedDate"))
                                .put("format", info.optString("printType"))
                                .put("isbn", info.optJSONArray("industryIdentifiers")?.let { ids ->
                                    (0 until ids.length()).mapNotNull { ids.optJSONObject(it)?.optString("identifier") }.firstOrNull().orEmpty()
                                }.orEmpty())
                                .put("source", "Google Books")
                                .put("sourceUrl", info.optString("infoLink")))
                        }
                    }
                } finally { connection.disconnect() }

                if (candidates.length() == 0) {
                    val params = "title=" + URLEncoder.encode(cleanTitle, "UTF-8") +
                        if (cleanAuthor.isNotBlank()) "&author=" + URLEncoder.encode(cleanAuthor, "UTF-8") else ""
                    val openUrl = URL("https://openlibrary.org/search.json?$params&limit=5")
                    val openConnection = (openUrl.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 7000; readTimeout = 9000
                        setRequestProperty("User-Agent", "ObjectDetective/1.0 (book title search)")
                        setRequestProperty("Accept", "application/json")
                    }
                    try {
                        if (openConnection.responseCode in 200..299) {
                            val payload = JSONObject(openConnection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                            val docs = payload.optJSONArray("docs")
                            if (docs != null) for (i in 0 until minOf(docs.length(), 5)) {
                                val doc = docs.optJSONObject(i) ?: continue
                                candidates.put(JSONObject()
                                    .put("title", doc.optString("title"))
                                    .put("author", doc.optJSONArray("author_name")?.let { a -> (0 until a.length()).joinToString(", ") { a.optString(it) } }.orEmpty())
                                    .put("publisher", doc.optJSONArray("publisher")?.optString(0).orEmpty())
                                    .put("publishedDate", doc.optJSONArray("first_publish_year")?.optString(0).orEmpty())
                                    .put("format", "Book record")
                                    .put("isbn", doc.optJSONArray("isbn")?.optString(0).orEmpty())
                                    .put("source", "Open Library")
                                    .put("sourceUrl", "https://openlibrary.org" + doc.optString("key")))
                            }
                        }
                    } finally { openConnection.disconnect() }
                }
                JSONObject().put("success", true).put("candidates", candidates)
                    .put("message", if (candidates.length() == 0) "No title-and-author candidates were found. Try correcting the cover text or adding another photo." else "")
            } catch (_: Exception) {
                JSONObject().put("success", false).put("message", "Book title catalogues could not be reached. Please try again.")
            }
            postBookTitleSearchResult(result)
        }, "ObjectDetective-BookTitleSearch").start()
    }

    private fun postBookTitleSearchResult(result: JSONObject) {
        webView.post {
            webView.evaluateJavascript("window.onBookTitleSearchResult && window.onBookTitleSearchResult($result)", null)
        }
    }

    private fun postBookIsbnResult(result: JSONObject) {
        webView.post {
            webView.evaluateJavascript(
                "window.onBookIsbnLookup && window.onBookIsbnLookup($result)",
                null
            )
        }
    }

    private fun postOpenFoodFactsResult(result: JSONObject) {
        webView.post {
            webView.evaluateJavascript(
                "window.onOpenFoodFactsFallback && window.onOpenFoodFactsFallback($result)",
                null
            )
        }
    }

    private fun formatName(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        else -> "Barcode"
    }
}
