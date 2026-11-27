package com.objectdetective.app

import android.graphics.BitmapFactory
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.barcode.BarcodeScanner
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Reads labels, model markings and barcodes from every supplied investigation photo.
 * Processing stays on-device; only extracted text and code values are returned to JS.
 */
class ObjectEvidenceBridge(private val webView: WebView) {
    private val executor = Executors.newSingleThreadExecutor()

    @JavascriptInterface
    fun analyzeImages(imagesJson: String, requestId: String) {
        executor.execute {
            val output = JSONObject()
            val photoResults = JSONArray()
            var recognizer: TextRecognizer? = null
            var scanner: BarcodeScanner? = null
            try {
                val images = JSONArray(imagesJson)
                val textClient = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer = textClient
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                val barcodeClient = BarcodeScanning.getClient(options)
                scanner = barcodeClient

                for (index in 0 until minOf(images.length(), 5)) {
                    val photo = JSONObject()
                    val barcodes = JSONArray()
                    photo.put("photoIndex", index)
                    try {
                        val dataUrl = images.optString(index)
                        val comma = dataUrl.indexOf(',')
                        if (!dataUrl.startsWith("data:image/") || comma < 0) {
                            photo.put("text", "")
                            photo.put("barcodes", barcodes)
                            photo.put("error", "Unsupported image data")
                            photoResults.put(photo)
                            continue
                        }
                        val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap == null) {
                            photo.put("text", "")
                            photo.put("barcodes", barcodes)
                            photo.put("error", "Image could not be decoded")
                            photoResults.put(photo)
                            continue
                        }
                        try {
                            val input = InputImage.fromBitmap(bitmap, 0)
                            val textResult = Tasks.await(textClient.process(input), 12, TimeUnit.SECONDS)
                            photo.put("text", textResult.text.take(5000))
                            val barcodeResults = Tasks.await(barcodeClient.process(input), 12, TimeUnit.SECONDS)
                            for (barcode in barcodeResults.take(10)) {
                                val value = barcode.rawValue?.trim().orEmpty()
                                val productCode = normalizeProductCode(value)
                                // Never send arbitrary QR contents, URLs, Wi-Fi details or contact data to a product catalogue.
                                if (productCode != null) {
                                    barcodes.put(JSONObject().put("value", productCode).put("format", formatName(barcode.format)))
                                }
                            }
                            photo.put("barcodes", barcodes)
                        } finally {
                            bitmap.recycle()
                        }
                    } catch (error: Exception) {
                        photo.put("text", photo.optString("text"))
                        photo.put("barcodes", barcodes)
                        photo.put("error", error.message?.take(180) ?: "Evidence scan failed")
                    }
                    photoResults.put(photo)
                }
                output.put("photos", photoResults)
                output.put("available", true)
            } catch (error: Exception) {
                output.put("photos", photoResults)
                output.put("available", false)
                output.put("error", error.message?.take(180) ?: "Evidence scan unavailable")
            } finally {
                try { recognizer?.close() } catch (_: Exception) {}
                try { scanner?.close() } catch (_: Exception) {}
            }
            val payload = output.toString()
            webView.post {
                webView.evaluateJavascript(
                    "window.onNativeEvidenceReady && window.onNativeEvidenceReady(${JSONObject.quote(requestId)}, ${JSONObject.quote(payload)})",
                    null
                )
            }
        }
    }

    private fun normalizeProductCode(raw: String): String? {
        val value = raw.trim()
        if (value.matches(Regex("^\\d{8}$|^\\d{12,14}$"))) return value
        if (value.matches(Regex("^\\d{9}[\\dXx]$"))) return value.uppercase()
        // GS1 Digital Link QR codes: extract only the GTIN, never transmit the full URL.
        return Regex("(?:^|/)01/(\\d{14})(?:/|$)").find(value)?.groupValues?.get(1)
    }

    private fun formatName(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        else -> "BARCODE"
    }
}
