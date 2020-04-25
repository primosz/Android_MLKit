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
import com.otaliastudios.cameraview.overlay.OverlayLayout
import kotlinx.android.synthetic.main.activity_live_detector.*
import java.util.concurrent.ThreadLocalRandom


class LiveDetector : AppCompatActivity() {

    private lateinit var content: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_detector)

        val cameraView = findViewById<CameraView>(R.id.camera)
        val tvDetectedObject = findViewById<TextView>(R.id.tvDetectedItem)
        content = findViewById<ImageView>(R.id.imv)

        cameraView.setLifecycleOwner(this) //Automatically handles the camera lifecycle

        cameraView.addFrameProcessor {
            extractDataFromFrame(it) { result ->



            }
        }

    }

    private fun extractDataFromFrame(frame: Frame, callback: (String) -> Unit) {

        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()  //Add this if you want to detect multiple objects at once
            .enableClassification()  // Add this if you want to classify the detected objects into categories
            .build()

        val objectDetector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)

        objectDetector.processImage(getVisionImageFromFrame(frame))
            .addOnSuccessListener {
                    detectedObjects ->

                content.setImageBitmap(Bitmap.createBitmap(content.width, content.height, Bitmap.Config.ARGB_8888 ))
                tvDetectedItem.text = ""
                var markedBitmap =
                    (content.drawable as BitmapDrawable)
                        .bitmap
                        .copy(Bitmap.Config.ARGB_8888, true)

                val canvas = Canvas(markedBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                tvDetectedItem.setTextColor(Color.BLACK)
                paint.color = Color.parseColor("#99003399")
                detectedObjects.forEach {
                    Log.wtf(it.toString(), "obj")
                    canvas.drawRect(it.boundingBox, paint)
                    when (it.classificationCategory) {
                        //Firebase only supports this much categories
                        0 -> tvDetectedItem.text = tvDetectedItem.text.toString() + "Unknown\n"
                        1 -> tvDetectedItem.text = tvDetectedItem.text.toString() + "Home good\n"
                        2 -> tvDetectedItem.text = tvDetectedItem.text.toString() + "Fashion good\n"
                        3 -> tvDetectedItem.text = tvDetectedItem.text.toString() + "Food\n"
                        4 -> tvDetectedItem.text = tvDetectedItem.text.toString() + "Place\n"
                        5 -> tvDetectedItem.text = tvDetectedItem.text.toString() + "Plant\n"
                    }
                }
                content.setImageBitmap(markedBitmap)
            }
            .addOnFailureListener {
                callback("Unable to detect an object")
            }
    }

    private fun getVisionImageFromFrame(frame : Frame) : FirebaseVisionImage{
        //ByteArray for the captured frame
        val data = frame.getData<ByteArray>()

        //Metadata that gives more information on the image that is to be converted to FirebaseVisionImage
        val imageMetaData = FirebaseVisionImageMetadata.Builder()
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setRotation(FirebaseVisionImageMetadata.ROTATION_90)
            .setHeight(frame.size.height)
            .setWidth(frame.size.width)
            .build()

        val image = FirebaseVisionImage.fromByteArray(data, imageMetaData)

        return image
    }

}
