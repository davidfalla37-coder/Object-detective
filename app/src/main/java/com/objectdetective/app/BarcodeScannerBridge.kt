package com.objectdetective.app

import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.android.gms.mlkit.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
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
