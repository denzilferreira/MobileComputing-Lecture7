package com.example.mobilecomputinglecture7

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.TextureView
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_mobile_vision.*
import java.util.concurrent.Executors

class MobileVision : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        lateinit var tts: TextToSpeech
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mobile_vision)

        viewFinder = findViewById(R.id.camera_feed)

        tts = TextToSpeech(applicationContext, this)
    }

    lateinit var viewFinder: TextureView
    val executor = Executors.newSingleThreadExecutor()

    override fun onResume() {
        super.onResume()

        btn_faces.setOnClickListener {

            Snackbar.make(vision_container, "Face Detection Engaged", Snackbar.LENGTH_SHORT).show()

            CameraX.unbindAll()

            val frontPreviewConfig = PreviewConfig.Builder().apply {
                setLensFacing(CameraX.LensFacing.FRONT)
            }.build()

            val frontPreview = Preview(frontPreviewConfig)
            frontPreview.setOnPreviewOutputUpdateListener {
                val parent = viewFinder.parent as ViewGroup
                parent.removeView(viewFinder)
                parent.addView(viewFinder, 0)
                viewFinder.surfaceTexture = it.surfaceTexture
            }

            val imageAnalysisConfig = ImageAnalysisConfig.Builder().apply {
                setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                setImageQueueDepth(1)
                setLensFacing(CameraX.LensFacing.FRONT)
            }.build()
            val imageAnalysis = ImageAnalysis(imageAnalysisConfig)

            imageAnalysis.setAnalyzer(executor, object : ImageAnalysis.Analyzer {

                var detected = false
                var smiling = false

                val options = FirebaseVisionFaceDetectorOptions.Builder().apply {
                    setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                    setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                    setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                }.build()

                fun degreesToFirebase(degrees: Int): Int = when (degrees) {
                    0 -> FirebaseVisionImageMetadata.ROTATION_0
                    90 -> FirebaseVisionImageMetadata.ROTATION_90
                    180 -> FirebaseVisionImageMetadata.ROTATION_180
                    270 -> FirebaseVisionImageMetadata.ROTATION_270
                    else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
                }

                override fun analyze(imageProxy: ImageProxy?, rotationDegrees: Int) {
                    val mediaImage = imageProxy?.image
                    val imageRotation = degreesToFirebase(rotationDegrees)
                    if (mediaImage != null) {
                        val image = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
                        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
                        detector.detectInImage(image).apply {
                            addOnSuccessListener { faces ->
                                val currentDetect = (faces.size > 0)
                                if (currentDetect != detected) {
                                    if (currentDetect) {
                                        tts.speak(
                                            "I can see you!",
                                            TextToSpeech.QUEUE_ADD,
                                            Bundle.EMPTY,
                                            "robot"
                                        )
                                    } else {
                                        tts.speak(
                                            "Where did you go?!",
                                            TextToSpeech.QUEUE_ADD,
                                            Bundle.EMPTY,
                                            "robot"
                                        )
                                    }
                                }

                                if (currentDetect) { //only check if someone is smiling if face is detected
                                    val isSmiling = faces.get(0).smilingProbability > 0.8
                                    if (isSmiling != smiling) {
                                        tts.speak(
                                            "What a beautiful smile!",
                                            TextToSpeech.QUEUE_ADD,
                                            Bundle.EMPTY,
                                            "robot"
                                        )
                                    }
                                    smiling = isSmiling
                                }

                                detected = currentDetect
                            }
                        }
                    }
                }
            })

            CameraX.bindToLifecycle(this, frontPreview, imageAnalysis)
        }

        btn_barcode.setOnClickListener {

            Snackbar.make(vision_container, "Barcodes Engaged", Snackbar.LENGTH_SHORT).show()

            CameraX.unbindAll()

            val backPreviewConfig = PreviewConfig.Builder().apply {
                setLensFacing(CameraX.LensFacing.BACK)
            }.build()

            val backPreview = Preview(backPreviewConfig)
            backPreview.setOnPreviewOutputUpdateListener {
                val parent = viewFinder.parent as ViewGroup
                parent.removeView(viewFinder)
                parent.addView(viewFinder, 0)
                viewFinder.surfaceTexture = it.surfaceTexture
            }

            val imageAnalysisConfig = ImageAnalysisConfig.Builder().apply {
                setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                setImageQueueDepth(1)
                setLensFacing(CameraX.LensFacing.BACK)
            }.build()
            val imageAnalysis = ImageAnalysis(imageAnalysisConfig)

            imageAnalysis.setAnalyzer(executor, object : ImageAnalysis.Analyzer {

                val options = FirebaseVisionBarcodeDetectorOptions.Builder().apply {
                    setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_ALL_FORMATS
                    )
                }.build()

                fun degreesToFirebase(degrees: Int): Int = when (degrees) {
                    0 -> FirebaseVisionImageMetadata.ROTATION_0
                    90 -> FirebaseVisionImageMetadata.ROTATION_90
                    180 -> FirebaseVisionImageMetadata.ROTATION_180
                    270 -> FirebaseVisionImageMetadata.ROTATION_270
                    else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
                }

                override fun analyze(imageProxy: ImageProxy?, rotationDegrees: Int) {
                    val mediaImage = imageProxy?.image
                    val imageRotation = degreesToFirebase(rotationDegrees)
                    if (mediaImage != null) {
                        val image = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
                        val detector =
                            FirebaseVision.getInstance().getVisionBarcodeDetector(options)
                        detector.detectInImage(image).apply {
                            addOnSuccessListener { barcodes ->
                                if (barcodes.size > 0) {
                                    Snackbar.make(
                                        vision_container,
                                        "Barcode: ${barcodes.get(0).rawValue}",
                                        Snackbar.LENGTH_INDEFINITE
                                    ).show()
                                }
                            }
                        }
                    }
                }
            })

            CameraX.bindToLifecycle(this, backPreview, imageAnalysis)
        }

        btn_ocr.setOnClickListener {

            Snackbar.make(vision_container, "OCR Engaged", Snackbar.LENGTH_SHORT).show()

            CameraX.unbindAll()

            val backPreviewConfig = PreviewConfig.Builder().apply {
                setLensFacing(CameraX.LensFacing.BACK)
            }.build()

            val backPreview = Preview(backPreviewConfig)
            backPreview.setOnPreviewOutputUpdateListener {
                val parent = viewFinder.parent as ViewGroup
                parent.removeView(viewFinder)
                parent.addView(viewFinder, 0)
                viewFinder.surfaceTexture = it.surfaceTexture
            }

            val imageAnalysisConfig = ImageAnalysisConfig.Builder().apply {
                setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                setImageQueueDepth(1)
                setLensFacing(CameraX.LensFacing.BACK)
            }.build()
            val imageAnalysis = ImageAnalysis(imageAnalysisConfig)

            imageAnalysis.setAnalyzer(executor, object : ImageAnalysis.Analyzer {

                fun degreesToFirebase(degrees: Int): Int = when (degrees) {
                    0 -> FirebaseVisionImageMetadata.ROTATION_0
                    90 -> FirebaseVisionImageMetadata.ROTATION_90
                    180 -> FirebaseVisionImageMetadata.ROTATION_180
                    270 -> FirebaseVisionImageMetadata.ROTATION_270
                    else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
                }

                override fun analyze(imageProxy: ImageProxy?, rotationDegrees: Int) {
                    val mediaImage = imageProxy?.image
                    val imageRotation = degreesToFirebase(rotationDegrees)
                    if (mediaImage != null) {
                        val image = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
                        val textDetector = FirebaseVision.getInstance().onDeviceTextRecognizer
                        textDetector.processImage(image).apply {
                            addOnSuccessListener { texts ->
                                ocr_text.text = texts.text
                                ocr_text.setBackgroundColor(Color.WHITE)
                                ocr_text.alpha = .7f
                            }
                        }
                    }
                }
            })

            CameraX.bindToLifecycle(this, backPreview, imageAnalysis)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { //good to go, greet the user
            tts.speak(
                "Initialising super computer... DONE!",
                TextToSpeech.QUEUE_ADD,
                Bundle.EMPTY,
                "greeting"
            )
        } else {
            //Ask device to install missing Text-To-Speech library
            startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CameraX.unbindAll()
        tts.shutdown()
    }
}
