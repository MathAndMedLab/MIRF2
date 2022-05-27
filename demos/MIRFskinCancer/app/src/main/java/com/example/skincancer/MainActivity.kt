package com.example.skincancer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import core.data.Data
import core.data.MirfData
import core.data.ParametrizedData
import core.pipeline.AlgorithmHostBlock
import core.pipeline.PipeStarter
import core.pipeline.Pipeline
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import kotlin.math.roundToInt
import android.net.Uri

/**
 * MainActivity contains buttons for taking images from the camera or from the gallery,
 * ImageView to display the photo that the user has selected, TextView to display result
 * of the algorithm.
 */
class MainActivity : AppCompatActivity() {
    private var btnBack: Button? = null
    private var btnCamera: Button? = null
    private var btnGallery: Button? = null
    private var selectedView: ImageView? = null
    private var text: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnBack = findViewById(R.id.back_button)
        btnCamera = findViewById(R.id.cameraBtn)
        btnGallery = findViewById(R.id.galleryBtn)
        selectedView = findViewById(R.id.displayImageView)
        text = findViewById(R.id.resText)

        btnGallery!!.setOnClickListener {
            getImageFromGallery()
        }

        btnCamera!!.setOnClickListener {
            askCameraPermission()
        }

        btnBack!!.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getImageFromCamera()
            } else {
                Toast.makeText(
                    this,
                    "Application needs camera permission for correct work",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * onActivityResult set the image from the camera or from the gallery to the selectedView
     * and run algorithm with this image
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val image = data!!.extras!!.get("data") as Bitmap
                selectedView!!.setImageBitmap(image)
                detectMole()
            } else {
                return
            }
        } else if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val selectedImage: Uri? = data!!.data
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

                val cursor: Cursor? = contentResolver.query(
                    selectedImage!!,
                    filePathColumn, null, null, null
                )
                cursor!!.moveToFirst()

                cursor.close()

                val bmp = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImage)

                selectedView!!.setImageBitmap(bmp)
                detectMole()
            } else {
                return
            }
        }
    }

    /**
     * getImageFromGallery creates an intent to open a gallery.
     * [onActivityResult] will be invoked after [startActivityForResult] with intent gallery and
     * and request code [GALLERY_REQUEST_CODE]
     */
    private fun getImageFromGallery() {
        try {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(gallery, GALLERY_REQUEST_CODE)
        } catch (e: IOException) {
            Toast.makeText(this, "Something wrong with taking image", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * getImageFromCamera creates an intent to open a camera.
     * [onActivityResult] will be invoked after [startActivityForResult] with intent camera and
     * and request code [CAMERA_REQUEST_CODE]
     */
    private fun getImageFromCamera() {
        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(camera, CAMERA_REQUEST_CODE)
    }

    /**
     * askCameraPermission will request permission for using camera if application doesn't have it.
     * If permission is granted getImageFromCamera will be invoked.
     */
    private fun askCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERM_CODE
            )
        } else {
            getImageFromCamera()
        }
    }

    /**
     * detectMole creates Pipeline: Data -> BitmapRawImage -> ParametrizedData
     * After all calculations result is wrote to [text]
     */
    private fun detectMole() {
        val modelName = "skin_cancer_model.pb"
        val tensorflowModel = TensorflowModel(
            assets, modelName, "conv2d_1_input_1", "activation_5_1/Sigmoid", 1, 1
        )
        val pipe = Pipeline("Detect moles")
        val imageReader = AlgorithmHostBlock<Data, BitmapRawImage>(
            {
                val rawImg = selectedView!!.drawable.toBitmap()
                return@AlgorithmHostBlock BitmapRawImage(rawImg)
            },
            pipelineKeeper = pipe
        )
        val tensorflowModelRunner = AlgorithmHostBlock<BitmapRawImage, ParametrizedData<Int>>(
            {
                val res = tensorflowModel.runModel(
                    it.getFloatImageArray(128, 128),
                    1,
                    128,
                    128,
                    3
                )[0].roundToInt()
                val content = "The mole is " + formatResult(res)
                text!!.text = content
                return@AlgorithmHostBlock ParametrizedData(res)

            },
            pipelineKeeper = pipe
        )
        //run
        val root = PipeStarter()
        root.dataReady += imageReader::inputReady
        imageReader.dataReady += tensorflowModelRunner::inputReady

        pipe.rootBlock = root
        pipe.run(MirfData.empty)
    }

    private fun formatResult(res: Int): String {
        return if (res == 1) {
            "malignant"
        } else {
            "benign"
        }
    }

    companion object {
        const val CAMERA_PERM_CODE = 101
        const val CAMERA_REQUEST_CODE = 102
        const val GALLERY_REQUEST_CODE = 105
    }
}
