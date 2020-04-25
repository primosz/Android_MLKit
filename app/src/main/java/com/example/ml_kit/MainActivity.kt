package com.example.ml_kit

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import java.util.concurrent.ThreadLocalRandom

class MainActivity : AppCompatActivity() {

    private val pickPhotoRequestCode: Int = 139
    private val takePhotoRequestCode: Int = 140
    private val takePhotoForRecognitionRequestCode: Int = 141
    private val takePhotoForDetectionRequestCode: Int = 142
    private lateinit var contentIV: ImageView
    private lateinit var tagsTV: TextView
    private lateinit var options: FirebaseVisionObjectDetectorOptions
    private lateinit var tagsWrapper: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tagsTV = findViewById(R.id.label)
        contentIV = findViewById(R.id.imageView)
        tagsWrapper = findViewById(R.id.tagsWrapper)
        val button = findViewById<Button>(R.id.button)
        val button2 = findViewById<Button>(R.id.button2)
        val button3 = findViewById<Button>(R.id.button3)
        val button4 = findViewById<Button>(R.id.button4)
        val button5 = findViewById<Button>(R.id.button5)


        button.setOnClickListener { view ->
            pickImage()
        }
        button2.setOnClickListener {view ->
            dispatchTakePictureIntent(takePhotoRequestCode)
        }
        button3.setOnClickListener {view ->
            dispatchTakePictureIntent(takePhotoForRecognitionRequestCode)
        }
        button4.setOnClickListener {view ->
            dispatchTakePictureIntent(takePhotoForDetectionRequestCode)
        }
        button5.setOnClickListener {view ->
            val intent = Intent(this, LiveDetector::class.java)
            startActivity(intent)
        }

        options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()

    }

    private fun dispatchTakePictureIntent(code: Int) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, code)
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, pickPhotoRequestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                pickPhotoRequestCode -> {
                    val bitmap = getImageFromData(data)
                    bitmap?.apply {
                        contentIV?.setImageBitmap(this)
                        processImageTagging(bitmap)
                    }
                }

                takePhotoRequestCode -> {
                    val bitmap = data?.extras?.get("data") as Bitmap;
                    bitmap?.apply {
                        contentIV?.setImageBitmap(this)
                        processImageTagging(bitmap)
                    }
                }
                takePhotoForRecognitionRequestCode -> {
                    val bitmap = data?.extras?.get("data") as Bitmap;
                    bitmap?.apply {
                        contentIV?.setImageBitmap(this)
                        processTextRecognition(bitmap)
                    }
                }
                takePhotoForDetectionRequestCode -> {
                    val bitmap = data?.extras?.get("data") as Bitmap;
                    bitmap?.apply {
                        contentIV?.setImageBitmap(this)
                        processObjectDetecting(bitmap)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getImageFromData(data: Intent?): Bitmap? {
        val selectedImage = data?.data
        return MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImage)
    }

    private fun processImageTagging(bitmap: Bitmap) {
        this.tagsWrapper.removeAllViews()
        val visionImg = FirebaseVisionImage.fromBitmap(bitmap)
        val labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler().
                processImage(visionImg)
                .addOnSuccessListener { tags ->
                    tagsTV.text = tags.joinToString(" ") {it.text }
                }
                .addOnFailureListener { ex ->
                    Log.wtf("LAB", ex)
                }
    }

    private fun processTextRecognition(bitmap: Bitmap) {
        this.tagsWrapper.removeAllViews()
        val visionImg = FirebaseVisionImage.fromBitmap(bitmap)
        val fbVisionTxtDetect = FirebaseVision.getInstance().onDeviceTextRecognizer
        fbVisionTxtDetect.processImage(visionImg)
            .addOnSuccessListener { result ->
                tagsTV.text = result.text
            }
            .addOnFailureListener {ex ->
                Log.wtf("LAB", ex)
            }
    }

    private fun processObjectDetecting(bitmap: Bitmap) {
        contentIV.setImageBitmap(bitmap)
        val visionImg = FirebaseVisionImage.fromBitmap(bitmap)
        val objectDetector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)
        objectDetector.processImage(visionImg)
            .addOnSuccessListener { detectedObjects ->
                var markedBitmap =
                    (contentIV.drawable as BitmapDrawable)
                        .bitmap
                        .copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(markedBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                this.tagsWrapper.removeAllViews()
                detectedObjects.forEach {
                    val rand = String.format("%06d", ThreadLocalRandom.current().nextInt(1, 1000000))
                    paint.color = Color.parseColor("#99$rand")
                    canvas.drawRect(it.boundingBox, paint)
                    val lparams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lparams.gravity = Gravity.CENTER_HORIZONTAL
                    var tv = TextView(this)
                    tv.layoutParams = lparams
                    tagsTV.text=""
                    when (it.classificationCategory) {
                        //Firebase only supports this much categories
                        0 -> tv.text =  "Unknown\n"
                        1 -> tv.text =   "Home good\n"
                        2 -> tv.text =  "Fashion good\n"
                        3 -> tv.text = "Food\n"
                        4 -> tv.text = "Place\n"
                        5 -> tv.text = "Plant\n"
                    }
                    this.tagsWrapper.addView(tv)
                    tv.setTextColor(paint.color)
                }
                contentIV.setImageBitmap(markedBitmap)

            }
            .addOnFailureListener {ex ->
                Log.wtf("LAB", ex)
            }
    }
}
