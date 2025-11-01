
package com.geosnap.industridykk

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var tvUtm: TextView
    private lateinit var tvHeadingAcc: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvPlace: TextView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) startCamera() else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.previewView)
        tvUtm = findViewById(R.id.tvUtm)
        tvHeadingAcc = findViewById(R.id.tvHeadingAcc)
        tvAddress = findViewById(R.id.tvAddress)
        tvPlace = findViewById(R.id.tvPlace)
        findViewById<ImageButton>(R.id.btnCapture).setOnClickListener { captureFlow() }

        cameraExecutor = Executors.newSingleThreadExecutor()
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startCamera()
        } else {
            requestPerms.launch(permissions)
        }
        startHudUpdates()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            imageCapture = ImageCapture.Builder()
                .setTargetRotation(previewView.display.rotation)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try { cameraProvider.unbindAll(); cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture) }
            catch (e: Exception) { Log.e("GeoSnap", "Failed binding camera", e) }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startHudUpdates() {
        scope.launch {
            val fused = LocationServices.getFusedLocationProviderClient(this@MainActivity)
            while (isActive) {
                try {
                    val loc = fused.lastLocation.awaitWithTimeout(3000)
                    if (loc != null) {
                        val utm = UTM.fromLatLon(loc.latitude, loc.longitude)
                        tvUtm.text = "UTM: ${utm.toString()} (±${loc.accuracy.toInt()} m) | Retning: ${Formatters.cardinalFrom(loc.bearing)}"
                        // Try geocode
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val list: List<Address> = Geocoder(this@MainActivity, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1) ?: emptyList()
                                val a = list.firstOrNull()
                                withContext(Dispatchers.Main) {
                                    if (a != null) {
                                        val road = listOfNotNull(a.thoroughfare, a.subThoroughfare).joinToString(" ")
                                        tvAddress.text = if (road.isNotEmpty()) "Adresse: $road" else "Adresse: (ukjent)"
                                        val place = listOfNotNull(a.locality, a.adminArea).joinToString(", ")
                                        tvPlace.text = if (place.isNotEmpty()) "Sted: $place" else "Sted: –"
                                    } else {
                                        tvAddress.text = "Adresse: venter på nett …"
                                        tvPlace.text = "Sted: –"
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(1500)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun captureFlow() {
        scope.launch {
            val comment = Dialogs.promptForComment(this@MainActivity) ?: return@launch
            val fused = LocationServices.getFusedLocationProviderClient(this@MainActivity)
            val loc = fused.currentOrLast(8000)
            val file = File(getExternalFilesDir(null), "geosnap_${System.currentTimeMillis()}.jpg")
            val output = ImageCapture.OutputFileOptions.Builder(file).build()
            imageCapture.takePicture(output, ContextCompat.getMainExecutor(this@MainActivity), object: ImageCapture.OnImageSavedCallback{
                override fun onError(exc: ImageCaptureException) {
                    Log.e("GeoSnap","Capture error", exc)
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            // Write EXIF
                            val exif = ExifInterface(file.absolutePath)
                            if (loc != null) {
                                exif.setLatLong(loc.latitude, loc.longitude)
                                exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, loc.bearing.toString())
                                exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "T")
                                exif.setAttribute(ExifInterface.TAG_GPS_H_POSITIONING_ERROR, loc.accuracy.toString())
                            }
                            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, comment)
                            exif.saveAttributes()

                            // Overlay render
                            val bmp = BitmapFactory.decodeFile(file.absolutePath)
                            val annotated = Overlay.drawOverlay(bmp, Overlay.buildLines(
                                utmText = if (loc!=null) UTM.fromLatLon(loc.latitude, loc.longitude).toString()+" (±"+loc.accuracy.toInt()+" m) | Retning: "+Formatters.cardinalFrom(loc.bearing) else "UTM: –",
                                address = tvAddress.text.toString().removePrefix("Adresse: ").ifBlank { "venter på nett" },
                                place = tvPlace.text.toString().removePrefix("Sted: "),
                                comment = comment,
                                timestamp = System.currentTimeMillis()
                            ))
                            FileOutputStream(file).use { out ->
                                annotated.compress(Bitmap.CompressFormat.JPEG, 92, out)
                            }
                        } catch (e: Exception) {
                            Log.e("GeoSnap","Postprocess failed", e)
                        }
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        cameraExecutor.shutdown()
    }
}

// --- Helpers below ---

private suspend fun com.google.android.gms.location.FusedLocationProviderClient.currentOrLast(timeoutMs: Long) =
    withContext(Dispatchers.IO) {
        runCatching {
            val task = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this@currentOrLast as android.content.Context).lastLocation
            task.awaitWithTimeout(timeoutMs) ?: task.result
        }.getOrNull()
    }

suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitWithTimeout(timeoutMs: Long): T? = suspendCancellableCoroutine { cont ->
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val r = Runnable { if (cont.isActive) cont.resume(null) {} }
    addOnSuccessListener { if (cont.isActive) cont.resume(it) {} }
    addOnFailureListener { if (cont.isActive) cont.resume(null) {} }
    handler.postDelayed(r, timeoutMs)
}
