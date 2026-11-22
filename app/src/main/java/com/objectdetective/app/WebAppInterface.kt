package com.objectdetective.app

import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
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
                .enableAutoZoom()
                .build()

            GmsBarcodeScanning.getClient(activity, options)
                .startScan()
                .addOnSuccessListener { barcode ->
                    val value = barcode.rawValue?.trim().orEmpty()
                    if (value.isNotEmpty()) {
                        sendResult("onBarcodeScanned", value)
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

    private fun sendResult(callback: String, value: String) {
        val safeValue = JSONObject.quote(value)
        webView.post {
            webView.evaluateJavascript("window.$callback?.($safeValue);", null)
        }
    }
}
