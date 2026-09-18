package com.objectdetective.app

import android.app.Activity
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.Intent
import android.net.Uri

class MainActivity : Activity() {

    private var fileCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        setContentView(webView)

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    
             type = "image/*"
addCategory(Intent.CATEGORY_OPENABLE)
}

startActivityForResult(intent, 1001)
return true
}
}

webView.loadUrl("file:///android_asset/index.html")
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == 1001) {
        val result = if (resultCode == RESULT_OK) {
            data?.data?.let { arrayOf(it) }
        } else {
            null
        }

        fileCallback?.onReceiveValue(result)
        fileCallback = null
    }
}
}       
