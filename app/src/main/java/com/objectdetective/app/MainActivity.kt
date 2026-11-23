package com.objectdetective.app

import android.app.Activity
import android.app.AlertDialog
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
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import java.io.IOException
import kotlin.math.roundToInt

// Preview build: keep Android navigation-area spacing density-correct.
class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null
    private lateinit var appUpdateManager: AppUpdateManager
    private var updatePromptShown = false
    private var downloadedUpdateDialogShown = false
    private var systemBottomInsetPx = 0
    private var systemTopInsetPx = 0

    private val updateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            runOnUiThread { showDownloadedUpdateDialog() }
        }
    }

    companion object {
        private const val FILE_REQUEST_CODE = 1001
        private const val UPDATE_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(updateListener)

        webView = WebView(this)
        setContentView(webView)

        webView.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                // Android 15/16 can report a zero visible navigation inset while
                // the gesture/navigation area still occupies screen space.
                val stableNavigation = insets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.navigationBars()
                )
                systemBottomInsetPx = maxOf(bars.bottom, stableNavigation.bottom)
                systemTopInsetPx = bars.top
                view.setPadding(bars.left, bars.top, bars.right, 0)
            } else {
                @Suppress("DEPRECATION")
                run {
                    systemBottomInsetPx = insets.systemWindowInsetBottom
                    systemTopInsetPx = insets.systemWindowInsetTop
                    view.setPadding(
                        insets.systemWindowInsetLeft,
                        insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight,
                        0
                    )
                }
            }
            applyWebInsets()
            insets
        }
        webView.requestApplyInsets()

        webView.addJavascriptInterface(WebAppInterface(this, webView), "ObjectDetectiveAndroid")

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
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                applyWebInsets()
            }

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
        webView.post { checkForAppUpdate() }
    }

    private fun applyWebInsets() {
        if (!::webView.isInitialized) return
        // WindowInsets are physical pixels; CSS pixels in WebView are density-independent.
        val density = resources.displayMetrics.density
        val bottom = (systemBottomInsetPx / density).roundToInt()
        val top = (systemTopInsetPx / density).roundToInt()
        webView.evaluateJavascript(
            "document.documentElement.style.setProperty('--android-bottom-inset', '" + bottom + "px');" +
                "document.documentElement.style.setProperty('--android-top-inset', '" + top + "px');",
            null
        )
    }

    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { updateInfo ->
                if (updateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    showDownloadedUpdateDialog()
                    return@addOnSuccessListener
                }

                val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                if (
                    updateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE ||
                    !updateInfo.isUpdateTypeAllowed(options) ||
                    updatePromptShown || isFinishing || isDestroyed
                ) {
                    return@addOnSuccessListener
                }

                updatePromptShown = true
                AlertDialog.Builder(this)
                    .setTitle("Update available")
                    .setMessage("A newer version of Object Detective is ready. You can download it now while continuing to use the app.")
                    .setPositiveButton("Update now") { _, _ ->
                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                updateInfo,
                                this,
                                options,
                                UPDATE_REQUEST_CODE
                            )
                        } catch (_: Exception) {
                            Toast.makeText(
                                this,
                                "The update could not be started. Please try again later.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .setNegativeButton("Not now", null)
                    .show()
            }
    }

    private fun showDownloadedUpdateDialog() {
        if (downloadedUpdateDialogShown || isFinishing || isDestroyed) return
        downloadedUpdateDialogShown = true

        AlertDialog.Builder(this)
            .setTitle("Update ready")
            .setMessage("Object Detective has downloaded the update. Restart the app to finish installing it.")
            .setPositiveButton("Restart now") { _, _ -> appUpdateManager.completeUpdate() }
            .setNegativeButton("Later") { _, _ -> downloadedUpdateDialogShown = false }
            .setOnCancelListener { downloadedUpdateDialogShown = false }
            .show()
    }

    override fun onDestroy() {
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(updateListener)
        }
        super.onDestroy()
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
