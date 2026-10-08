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
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var webViewAssetLoader: WebViewAssetLoader
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null

    companion object {
        private const val FILE_REQUEST_CODE = 1001
        private const val APP_ASSET_HOST = "appassets.androidplatform.net"
        private const val SUPABASE_HOST = "gboyflbwcobhzbvpdtat.supabase.co"
        private const val SUPABASE_FUNCTION_PATH = "/functions/v1/investigate-object"
    }

    private fun blockedWebResponse(): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "UTF-8",
            403,
            "Blocked",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0))
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        convertedPhotosDirectory().deleteRecursively()
        webViewAssetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

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
            allowFileAccess = false
            allowContentAccess = true
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            textZoom = 100
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val uri = request.url
                val scheme = uri.scheme?.lowercase()
                val host = uri.host?.lowercase()

                if (scheme == "https" && host == APP_ASSET_HOST) {
                    return webViewAssetLoader.shouldInterceptRequest(uri)
                        ?: blockedWebResponse()
                }
                if (
                    scheme == "https" &&
                    host == SUPABASE_HOST &&
                    uri.path == SUPABASE_FUNCTION_PATH
                ) {
                    return null
                }
                return blockedWebResponse()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase()

                if (
                    scheme == "https" &&
                    uri.host?.lowercase() == APP_ASSET_HOST &&
                    uri.path?.startsWith("/assets/") == true
                ) {
                    return false
                }

                if (scheme == "https") {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            "No browser can open this link.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.deny() }
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
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
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

        webView.loadUrl("https://appassets.androidplatform.net/assets/index-2.html")
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

    private fun convertedPhotosDirectory(): File =
        File(cacheDir, "converted-photos")

    private fun convertToJpeg(sourceUri: Uri): Uri {
        val bitmap = decodeBitmap(sourceUri)
        val outputDirectory = convertedPhotosDirectory()
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            bitmap.recycle()
            throw IOException("Temporary photo storage could not be created.")
        }

        val outputFile = File(
            outputDirectory,
            "ObjectDetective_${UUID.randomUUID()}.jpg"
        )

        try {
            FileOutputStream(outputFile).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
                    throw IOException("The JPEG conversion failed.")
                }
            }
            return FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                outputFile
            )
        } catch (error: Exception) {
            outputFile.delete()
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
            val selectedUris = mutableListOf<Uri>()
            val clipData = data?.clipData
            if (clipData != null) {
                for (index in 0 until clipData.itemCount) {
                    selectedUris.add(clipData.getItemAt(index).uri)
                }
            } else {
                data?.data?.let { selectedUris.add(it) }
            }

            val selectedFromCamera = cameraUri != null &&
                (selectedUris.isEmpty() || selectedUris.size == 1 && selectedUris[0] == cameraUri)
            val sourceUris = selectedUris.ifEmpty {
                listOf(cameraUri ?: throw IOException("No photograph was selected."))
            }
            if (!selectedFromCamera) {
                cameraUri?.let { contentResolver.delete(it, null, null) }
            }
            if (sourceUris.size > 3) {
                Toast.makeText(this, "Only the first three photos will be used.", Toast.LENGTH_SHORT).show()
            }
            val jpegUris = sourceUris.take(3).map { sourceUri ->
                if (selectedFromCamera && sourceUri == cameraUri) sourceUri else convertToJpeg(sourceUri)
            }
            cameraImageUri = null
            callback?.onReceiveValue(jpegUris.toTypedArray())
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

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        convertedPhotosDirectory().deleteRecursively()
        super.onDestroy()
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
