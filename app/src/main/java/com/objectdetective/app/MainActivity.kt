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

                val captureEnabled = fileChooserParams?.isCaptureEnabled == true

                return try {
                    if (captureEnabled) {
                        val cameraUri = createCameraImageUri()
                        cameraImageUri = cameraUri
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        }
                        startActivityForResult(cameraIntent, FILE_REQUEST_CODE)
                    } else {
                        val allowMultiple =
                            fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
                        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "image/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
                        }
                        startActivityForResult(
                            Intent.createChooser(galleryIntent, "Choose photos"),
                            FILE_REQUEST_CODE
                        )
                    }
                    true
                } catch (_: Exception) {
                    cameraImageUri?.let { contentResolver.delete(it, null, null) }
                    cameraImageUri = null
                    fileCallback?.onReceiveValue(null)
                    fileCallback = null
                    Toast.makeText(
                        this@MainActivity,
                        if (captureEnabled) "Unable to open the camera." else "Unable to open the gallery.",
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

        val selectedUris = mutableListOf<Uri>()
        data?.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) {
                val uri = clip.getItemAt(index).uri
                if (uri !in selectedUris) selectedUris.add(uri)
            }
        }
        data?.data?.let { uri ->
            if (uri !in selectedUris) selectedUris.add(uri)
        }
        if (selectedUris.isEmpty()) {
            cameraImageUri?.let(selectedUris::add)
        }

        try {
            if (selectedUris.isEmpty()) {
                throw IOException("No image was selected.")
            }

            val readyUris = selectedUris.map { uri ->
                if (uri == cameraImageUri) uri else convertToJpeg(uri)
            }
            cameraImageUri = null
            callback?.onReceiveValue(readyUris.toTypedArray())
        } catch (_: Exception) {
            cameraImageUri?.let { contentResolver.delete(it, null, null) }
            cameraImageUri = null
            callback?.onReceiveValue(null)
            Toast.makeText(
                this,
                "That image could not be opened.",
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
