package com.example.bangkitdeploymentfinal

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.FileUtils
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bangkitdeploymentfinal.ml.ModelFinalBangkit
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class MainActivity : AppCompatActivity() {

    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )

    lateinit var  labels:List<String>
    val paint = Paint()
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager
    lateinit var cameraDevice : CameraDevice
    lateinit var  imageView : ImageView
    lateinit var  bitmap: Bitmap

    lateinit var  model : ModelFinalBangkit

    lateinit var textureView: TextureView

    lateinit var  imageProcessor: ImageProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        get_permission()
        labels = FileUtil.loadLabels(this, "label.txt")

        model = ModelFinalBangkit.newInstance(this)
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(320,320, ResizeOp.ResizeMethod.BILINEAR)).build()

        var handlerThread = HandlerThread("VideoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = findViewById(R.id.imageView)

        textureView = findViewById(R.id.textureView)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap = textureView.bitmap!!



                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)


                val outputs = model.process(image)
//                val detectionResult = outputs.detectionResultList.get(0)
                val locations = outputs.locationAsTensorBuffer.floatArray
                val classes = outputs.categoryAsTensorBuffer.floatArray
                val scores = outputs.scoreAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray


//                val location = detectionResult.scoreAsFloat;
//                val category = detectionResult.locationAsRectF;
//                val score = detectionResult.categoryAsString;

                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)


                val h = mutable.height
                val w = mutable.width

                paint.textSize = h/15f
                paint. strokeWidth = h/85f
                var x = 0

                //Bikin bounding box
                scores.forEachIndexed { index, fl ->
                    x = index
                    x*= 4
                    if(fl >0.5){
                        paint.setColor(colors.get(index))
                        paint.style = Paint.Style.STROKE
                        //nge gambar bounding box
                        canvas.drawRect(RectF(locations.get(x+1)*w, locations.get(x)*h, locations.get(x+3)*w, locations.get(x+2)*h), paint)
                        paint.style = Paint.Style.FILL
                        // ngegambar teks kelas ex-> kentang
                        canvas.drawText(labels.get(classes.get(index).toInt()) + "" + fl.toString(), locations.get(x+1)*w, locations.get(x)*h, paint)
                        //kemungkinan bikin tombol dibawah sini
                    }

                }
                imageView.setImageBitmap(mutable)



            }

        }


        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        //terakhir di oncreate
    }


    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

    @SuppressLint("MissingPermission")
    fun open_camera(){
        cameraManager.openCamera(cameraManager.cameraIdList[0], object:CameraDevice.StateCallback(){
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera

                var surfaceTexture = textureView.surfaceTexture
                var surface = Surface(surfaceTexture)

                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.setRepeatingRequest(captureRequest.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        TODO("Not yet implemented")
                    }

                }, handler)

            }

            override fun onDisconnected(camera: CameraDevice) {
                TODO("Not yet implemented")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                TODO("Not yet implemented")
            }

        }, handler )

    }


    fun get_permission(){
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA),101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            get_permission()
        }
    }


    //terakhir di main
}