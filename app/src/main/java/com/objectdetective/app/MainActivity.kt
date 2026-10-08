package com.objectdetective.app

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowInsets
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.io.IOException

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null

    companion object {
        private const val FILE_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }
            insets
        }
        webView.requestApplyInsets()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            textZoom = 100
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
    val uri = request.url
    val scheme = uri.scheme?.lowercase()

    return if (scheme == "http" || scheme == "https") {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            false
        }
    } else {
        false
    }
            }
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
                cameraImageUri?.let { contentResolver.delete(it, null, null) }
                cameraImageUri = null

                val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                val cameraIntent = try {
                    val outputUri = createCameraImageUri()
                    cameraImageUri = outputUri
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                } catch (_: Exception) {
                    null
                }

                return try {
                    val chooser = Intent.createChooser(galleryIntent, "Choose a photo")
                    cameraIntent?.let {
                        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(it))
                    }
                    startActivityForResult(chooser, FILE_REQUEST_CODE)
                    true
                } catch (_: Exception) {
                    cameraImageUri?.let { contentResolver.delete(it, null, null) }
                    cameraImageUri = null
                    fileCallback?.onReceiveValue(null)
                    fileCallback = null
                    Toast.makeText(
                        this@MainActivity,
                        "Unable to open photos or camera.",
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }
            }
        }

        webView.loadUrl("file:///android_asset/index-2.html")
    }

    private fun createCameraImageUri(): Uri {
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "ObjectDetective_${System.currentTimeMillis()}.jpg"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/ObjectDetective"
            )
        }

        return contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw IOException("A camera image could not be created.")
    }

    private fun decodeBitmap(sourceUri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, sourceUri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
                    ?: throw IOException("The selected image could not be decoded.")
            } ?: throw IOException("The selected image could not be opened.")
        }
    }

    private fun convertToJpeg(sourceUri: Uri): Uri {
        val bitmap = decodeBitmap(sourceUri)
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "ObjectDetective_${System.currentTimeMillis()}.jpg"
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
        ) ?: throw IOException("A JPEG copy could not be created.")

        try {
            contentResolver.openOutputStream(outputUri)?.use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
                    throw IOException("The JPEG conversion failed.")
                }
            } ?: throw IOException("The JPEG copy could not be opened.")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(outputUri, values, null, null)
            return outputUri
        } catch (error: Exception) {
            contentResolver.delete(outputUri, null, null)
            throw error
        } finally {
            bitmap.recycle()
        }
    }

    @Deprecated("Uses the compatibility activity-result callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != FILE_REQUEST_CODE) return

        val callback = fileCallback
        fileCallback = null

        if (resultCode != RESULT_OK) {
            cameraImageUri?.let { contentResolver.delete(it, null, null) }
            cameraImageUri = null
            callback?.onReceiveValue(null)
            return
        }

        try {
            val cameraUri = cameraImageUri
            val selectedUri = data?.data
            val sourceUri = selectedUri ?: cameraUri
                ?: throw IOException("No photograph was selected.")
            val selectedFromCamera = cameraUri != null &&
                (selectedUri == null || selectedUri == cameraUri)

            if (!selectedFromCamera) {
                cameraUri?.let { contentResolver.delete(it, null, null) }
            }
            val jpegUri = if (selectedFromCamera) sourceUri else convertToJpeg(sourceUri)
            cameraImageUri = null
            callback?.onReceiveValue(arrayOf(jpegUri))
        } catch (_: Exception) {
            cameraImageUri?.let { contentResolver.delete(it, null, null) }
            cameraImageUri = null
            callback?.onReceiveValue(null)
            Toast.makeText(
                this,
                "That image could not be converted.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Uses the compatibility back callback")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
