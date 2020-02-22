package com.example.mobilecomputinglecture7

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Camera
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.android.synthetic.main.activity_reuse_camera.*

class ReuseCamera : AppCompatActivity() {

    val RESULT_CAMERA_PHOTO = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reuse_camera)

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, RESULT_CAMERA_PHOTO)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_CAMERA_PHOTO && resultCode == Activity.RESULT_OK) {
            val pic = data?.extras?.get("data") as Bitmap
            img_photo.setImageBitmap(pic)

            //selfie camera is rotated 90 degrees to the right, so we are rotating it back
            img_photo.rotation = -90f
        }
    }

}
