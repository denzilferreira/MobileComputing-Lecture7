package com.example.mobilecomputinglecture7

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_own_camera.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.util.concurrent.Executors

class OwnCamera : AppCompatActivity() {

    val PERMISSIONS_NEEDED_CODE = 12345
    lateinit var overlay : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_own_camera)
        viewFinder = findViewById(R.id.camera_view)
        overlay = findViewById(R.id.overlay_filter)

        if (!isCameraAllowed()) {
            ActivityCompat.requestPermissions(this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSIONS_NEEDED_CODE
            )
            return
        } else {
            viewFinder.post { startCamera() }
        }
    }

    lateinit var viewFinder : TextureView
    val executor = Executors.newSingleThreadExecutor()

    fun startCamera() {

        val previewConfig = PreviewConfig.Builder().build()
        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)
            viewFinder.surfaceTexture = it.surfaceTexture
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
        }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        btn_photo.setOnClickListener {
            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"OwnCamera-${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(file, executor, object: ImageCapture.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    val message = "Photo saved!"
                    Snackbar.make(own_camera, message, Snackbar.LENGTH_SHORT).show()
                }

                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    cause: Throwable?
                ) {
                    Snackbar.make(own_camera, "Failed to save photo. Out of space?", Snackbar.LENGTH_SHORT).show()
                }

            })
        }


        val imageAnalysisConfig = ImageAnalysisConfig.Builder().apply {
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            setImageQueueDepth(1)
        }.build()

        val imageAnalysis = ImageAnalysis(imageAnalysisConfig)

        btn_filter.setOnClickListener {
            if(OpenCVLoader.initDebug()) {
                val black_n_white = Imgproc.COLOR_RGB2GRAY
                imageAnalysis.setAnalyzer(executor, object : ImageAnalysis.Analyzer {
                    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
                        val img = viewFinder.getBitmap()
                        val mat = Mat()
                        Utils.bitmapToMat(img, mat)
                        Imgproc.cvtColor(mat, mat, black_n_white)
                        Utils.matToBitmap(mat, img)
                        runOnUiThread {
                            overlay.setImageBitmap(img)
                        }
                    }
                })
            }
        }

        btn_flash.setOnClickListener {
            if (preview.isTorchOn) {
                preview.enableTorch(false)
            } else preview.enableTorch(true)
        }

        CameraX.bindToLifecycle(this, preview, imageCapture, imageAnalysis)
    }

    fun isCameraAllowed(): Boolean = (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_NEEDED_CODE) {
            if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                Toast.makeText(
                    this,
                    "These permissions are needed to run this app. Bye!",
                    Toast.LENGTH_LONG
                ).show()
                finish() //no permissions, no app!
            } else {
                //start camera!
                viewFinder.post { startCamera() }
            }
        }
    }
}
