package com.example.mobilecomputinglecture7

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_mobile_vision.*
import java.util.concurrent.Executors

class MobileVision : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        lateinit var tts : TextToSpeech
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mobile_vision)

        viewFinder = findViewById(R.id.camera_feed)

        tts = TextToSpeech( applicationContext, this)
    }

    lateinit var viewFinder : TextureView
    val executor = Executors.newSingleThreadExecutor()

    override fun onResume() {
        super.onResume()

        btn_faces.setOnClickListener {

            Toast.makeText(applicationContext, "Face detection engaged", Toast.LENGTH_SHORT).show()

            CameraX.unbindAll()

            val frontPreviewConfig = PreviewConfig.Builder().apply {
                setLensFacing(CameraX.LensFacing.FRONT)
            }.build()

            val frontPreview = Preview(frontPreviewConfig)
            frontPreview.setOnPreviewOutputUpdateListener {
                val parent = viewFinder.parent as ViewGroup
                parent.removeView(viewFinder)
                parent.addView(viewFinder,0)
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

                fun degreesToFirebase(degrees : Int) : Int = when(degrees) {
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
                                val currentDetect = faces.size > 0
                                if (currentDetect != detected) {
                                    if (currentDetect) {
                                        tts.speak("I can see you!", TextToSpeech.QUEUE_ADD, Bundle.EMPTY, "robot")
                                    } else {
                                        tts.speak("Where did you go?!", TextToSpeech.QUEUE_ADD, Bundle.EMPTY, "robot")
                                    }
                                }

                                val currentSmile = faces.get(0).smilingProbability > 0.8
                                if (currentSmile != smiling) {
                                    if (currentSmile) {
                                        tts.speak("What a beautiful smile!", TextToSpeech.QUEUE_ADD, Bundle.EMPTY, "robot")
                                    }
                                }

                                detected = currentDetect
                                smiling = currentSmile
                            }
                        }
                    }
                }
            })

            CameraX.bindToLifecycle(this, frontPreview, imageAnalysis)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { //good to go, greet the user
            tts.speak("Initialising super computer... DONE!", TextToSpeech.QUEUE_ADD, Bundle.EMPTY, "greeting")
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
