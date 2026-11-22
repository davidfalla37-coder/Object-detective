package com.objectdetective.app

import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import org.json.JSONObject

class WebAppInterface(
    private val activity: Activity,
    private val webView: WebView
) {
    @JavascriptInterface
    fun scanBarcode() {
        activity.runOnUiThread {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .enableAutoZoom()
                .build()

            GmsBarcodeScanning.getClient(activity, options)
                .startScan()
                .addOnSuccessListener { barcode ->
                    val value = barcode.rawValue?.trim().orEmpty()
                    if (value.isNotEmpty()) {
                        sendResult("onBarcodeScanned", value, formatName(barcode.format))
                    } else {
                        sendResult("onBarcodeScanError", "The code was detected but contained no readable text.")
                    }
                }
                .addOnCanceledListener {
                    sendResult("onBarcodeScanCancelled", "")
                }
                .addOnFailureListener { error ->
                    sendResult(
                        "onBarcodeScanError",
                        error.localizedMessage ?: "The barcode scanner could not start. Please try again."
                    )
                }
        }
    }

    private fun formatName(format: Int): String = when (format) {
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_CODABAR -> "Codabar"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_QR_CODE -> "QR Code"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "Aztec"
        else -> "Other supported format ($format)"
    }

    private fun sendResult(callback: String, value: String, formatName: String? = null) {
        val safeValue = JSONObject.quote(value)
        val safeFormat = formatName?.let(JSONObject::quote)
        val arguments = if (safeFormat == null) safeValue else "$safeValue, $safeFormat"
        webView.post {
            webView.evaluateJavascript("window.$callback?.($arguments);", null)
        }
    }
}
