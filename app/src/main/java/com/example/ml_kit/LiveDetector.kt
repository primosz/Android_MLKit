package com.example.ml_kit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.frame.Frame
import kotlinx.android.synthetic.main.activity_live_detector.*


class LiveDetector : AppCompatActivity() {

    private lateinit var content: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_detector)

        val cameraView = findViewById<CameraView>(R.id.camera)
        content = findViewById<ImageView>(R.id.imv)
        cameraView.setLifecycleOwner(this)
        cameraView.addFrameProcessor {
            extractDataFromFrame(it) {
                Log.println(Log.DEBUG, it, "detecting")
            }
        }
    }

    private fun extractDataFromFrame(frame: Frame, callback: (String) -> Unit) {

        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()

        val objectDetector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)

        objectDetector.processImage(getVisionImageFromFrame(frame))
            .addOnSuccessListener {
                    detectedObjects ->
                content.setImageBitmap(Bitmap.createBitmap(content.width, content.height, Bitmap.Config.ARGB_8888 ))
                val markedBitmap =
                    (content.drawable as BitmapDrawable)
                        .bitmap
                        .copy(Bitmap.Config.ARGB_8888, true)

                val canvas = Canvas(markedBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color = Color.parseColor("#99003399")
                tvDetectedItem.setTextColor(Color.BLACK)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                textPaint.color = Color.WHITE
                textPaint.textSize = 22.toFloat()
                detectedObjects.forEach {
                    Log.wtf(it.toString(), "obj")
                    canvas.drawRoundRect(it.boundingBox.left.toFloat(), it.boundingBox.top.toFloat(), it.boundingBox.right.toFloat(), it.boundingBox.bottom.toFloat(), 16.toFloat(), 16.toFloat(), paint)

                    when (it.classificationCategory) {
                        0 -> canvas.drawText("Unknown", 0, 7, it.boundingBox.left.toFloat() +16, it.boundingBox.top.toFloat()+16, textPaint)
                        1 -> canvas.drawText("Home good", 0, 9, it.boundingBox.left.toFloat() +16, it.boundingBox.top.toFloat()+16, textPaint)
                        2 -> canvas.drawText("Fashion good", 0, 12, it.boundingBox.left.toFloat() +16, it.boundingBox.top.toFloat()+16, textPaint)
                        3 -> canvas.drawText("Food", 0, 4, it.boundingBox.left.toFloat() +16, it.boundingBox.top.toFloat()+16, textPaint)
                        4 -> canvas.drawText("Place", 0, 5, it.boundingBox.left.toFloat() +16, it.boundingBox.top.toFloat()+16, textPaint)
                        5 -> canvas.drawText("Plant", 0, 5, it.boundingBox.left.toFloat() +16, it.boundingBox.top.toFloat()+16, textPaint)
                    }
                }
                content.setImageBitmap(markedBitmap)
            }
            .addOnFailureListener {
                callback("Unable to detect an object")
            }
    }

    private fun getVisionImageFromFrame(frame : Frame) : FirebaseVisionImage{
        val data = frame.getData<ByteArray>()

        val imageMetaData = FirebaseVisionImageMetadata.Builder()
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setRotation(FirebaseVisionImageMetadata.ROTATION_90)
            .setHeight(frame.size.height)
            .setWidth(frame.size.width)
            .build()

        return FirebaseVisionImage.fromByteArray(data, imageMetaData)
    }

}
