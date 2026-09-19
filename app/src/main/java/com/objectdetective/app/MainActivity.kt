package com.objectdetective.app

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val fileRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1002)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = false
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    request.grant(request.resources)
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback

                val chooserIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ).apply {
                    type = "image/*"
                }

                return try {
                    startActivityForResult(chooserIntent, fileRequestCode)
                    true
                } catch (_: ActivityNotFoundException) {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = null
                    Toast.makeText(
                        this@MainActivity,
                        "No gallery app was found.",
                        Toast.LENGTH_LONG
                    ).show()
                    false
                }
            }
        }

        webView.clearCache(true)
        webView.loadUrl("file:///android_asset/index-2.html")
    }

    private fun convertToJpeg(sourceUri: Uri): Uri {
        return try {
            val source = ImageDecoder.createSource(contentResolver, sourceUri)
            val bitmap = ImageDecoder.decodeBitmap(source)

            val values = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "object_detective_${System.currentTimeMillis()}.jpg"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/ObjectDetective"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val outputUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: return sourceUri

            contentResolver.openOutputStream(outputUri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(outputUri, values, null, null)

            outputUri
        } catch (_: Exception) {
            sourceUri
        }
    }

    @Deprecated("Uses the compatible activity-result callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != fileRequestCode) return

        val selectedUri =
            if (resultCode == RESULT_OK) data?.data else null

        val result =
            selectedUri?.let { arrayOf(convertToJpeg(it)) }

        fileCallback?.onReceiveValue(result)
        fileCallback = null
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = false
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback

                val chooserIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    
                    type = "image/*"
                    
                }

                return try {
                    startActivityForResult(chooserIntent, fileRequestCode)
                    true
                } catch (_: ActivityNotFoundException) {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = null
                    Toast.makeText(this@MainActivity, "No photo picker was found", Toast.LENGTH_LONG).show()
                    false
                }
            }
        }

        webView.clearCache(true)
        webView.loadUrl("file:///android_asset/index-2.html")
    }

    @Deprecated("Uses the compatible activity result method for this lightweight project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != fileRequestCode) return

        val selectedUris = if (resultCode == RESULT_OK) {
            data?.clipData?.let { clip ->
                Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
            } ?: data?.data?.let { arrayOf(it) }
        } else {
            null
        }

        fileCallback?.onReceiveValue(selectedUris)
        fileCallback = null
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
