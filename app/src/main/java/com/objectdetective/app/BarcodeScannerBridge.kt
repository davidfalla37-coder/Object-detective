package com.objectdetective.app

import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONObject

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

    @JavascriptInterface
    fun recognizeText(dataUrl: String, requestId: String, photoIndex: Int) {
        try {
            val encoded = dataUrl.substringAfter(",", "")
            if (encoded.isBlank()) {
                sendOcrResult(requestId, photoIndex, "", "Invalid image data.")
                return
            }
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap == null) {
                sendOcrResult(requestId, photoIndex, "", "Image could not be decoded.")
                return
            }

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { result ->
                        sendOcrResult(requestId, photoIndex, result.text.take(4000), "")
                    }
                    .addOnFailureListener {
                        sendOcrResult(requestId, photoIndex, "", "Text recognition unavailable.")
                    }
                    .addOnCompleteListener {
                        bitmap.recycle()
                        recognizer.close()
                    }
            } catch (error: Exception) {
                bitmap.recycle()
                recognizer.close()
                sendOcrResult(requestId, photoIndex, "", "Text recognition unavailable.")
            }
        } catch (error: Exception) {
            sendOcrResult(requestId, photoIndex, "", "Image processing failed.")
        }
    }

    private fun sendOcrResult(requestId: String, photoIndex: Int, text: String, error: String) {
        val script = "window.onNativeOcrResult?.(${JSONObject.quote(requestId)}, $photoIndex, ${JSONObject.quote(text)}, ${JSONObject.quote(error)})"
        webView.post { webView.evaluateJavascript(script, null) }
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
