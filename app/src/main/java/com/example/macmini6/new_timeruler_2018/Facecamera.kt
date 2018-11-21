package com.example.macmini6.new_timeruler_2018

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Camera
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.support.annotation.NonNull
import android.support.v7.app.AppCompatActivity
import android.support.annotation.RequiresApi
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_facecamera.*
import java.io.*
import java.util.*
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CaptureRequest
import android.support.v4.app.ActivityCompat


@Suppress("DEPRECATED_IDENTITY_EQUALS")
class Facecamera : AppCompatActivity(), TextureView.SurfaceTextureListener {
    private val TAG = "AndroidCameraApi"
    private var takePictureButton: Button? = null
    private var textureView: TextureView? = null
    private val ORIENTATIONS = SparseIntArray()


    private var cameraId: String? = null
    protected var cameraDevice: CameraDevice? = null
    protected var cameraCaptureSessions: CameraCaptureSession? = null
    protected var captureRequest: CaptureRequest? = null
    protected var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    private var file: File? = null
    private val REQUEST_CAMERA_PERMISSION = 200
    private var mFlashSupported: Boolean = false
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null

    val ORIENTATION = SparseIntArray()


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facecamera)
        getOrientation()

        textureView = findViewById(R.id.texTureView)
        textureView?.surfaceTextureListener = this

        startBackgroundThread()

        btnCapture.setOnClickListener {
            takePicture()
        }

    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        return false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                openCamera()

    }


    private fun getOrientation() {
        ORIENTATION.append(Surface.ROTATION_0, 90)
        ORIENTATION.append(Surface.ROTATION_90, 0)
        ORIENTATION.append(Surface.ROTATION_180, 270)
        ORIENTATION.append(Surface.ROTATION_270, 180)

    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun takePicture() {
        if (cameraDevice == null)
            return
            var manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

            try {


                val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
                var jpegSizes: Array<Size>? = null

                if (characteristics != null)
                    jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG)


                var width = 640
                var height = 480
                if (jpegSizes != null && jpegSizes.isNotEmpty()) {
                    width = jpegSizes[0].width
                    height = jpegSizes[0].height

                }


                val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
                var outputSurface = ArrayList<Surface>(2)
                outputSurface.add(reader.surface)
                outputSurface.add(Surface(textureView!!.surfaceTexture))

                val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captureBuilder.addTarget(reader.surface)
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)


                // Orientation
                val rotation = windowManager.defaultDisplay.rotation
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));

                val file = File("${Environment.getExternalStorageDirectory()}/pic.jpg")
                val readerListener = ImageReader.OnImageAvailableListener { reader ->
                    var image: Image? = null
                    @Throws(IOException::class)
                    fun save(bytes: ByteArray) {
                        var output: OutputStream? = null
                        try {
                            output = FileOutputStream(file)
                            output?.write(bytes)
                        } finally {
                            if (null != output) {
                                output!!.close()
                            }
                        }

                    }
                    try {
                        image = reader.acquireLatestImage()
                        val buffer = image!!.getPlanes()[0].getBuffer()
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)
                        save(bytes)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        if (image != null) {
                            image?.close()
                        }
                    }

                }


                reader.setOnImageAvailableListener(readerListener, mBackgroundHandler!!)
                var captureListener = object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        Toast.makeText(this@Facecamera, "Saved:$file", Toast.LENGTH_SHORT).show()
                        createCameraPreview()
                    }
                }
                cameraDevice!!.createCaptureSession(outputSurface, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            cameraCaptureSessions!!.capture(captureBuilder.build(), captureListener, mBackgroundHandler)
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }


                }, mBackgroundHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()


        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createCameraPreview() {
        try {
            val texture = textureView!!.surfaceTexture
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(surface)
            cameraDevice!!.createCaptureSession(
                Arrays.asList(surface), @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(@NonNull cameraCaptureSession: CameraCaptureSession) {
                        if (cameraDevice == null)
                            return
                        cameraCaptureSessions = cameraCaptureSession
                        updatePreview()
                    }

                    override fun onConfigureFailed(@NonNull cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(this@Facecamera, "Changed", Toast.LENGTH_SHORT).show()
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun updatePreview() {
        if (cameraDevice == null)
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions?.setRepeatingRequest(captureRequestBuilder!!.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    var stateCallback: CameraDevice.StateCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, i: Int) {
            cameraDevice.close()
            cameraDevice = null!!
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            //Check realtime permission if run higher API 23
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf<String>(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CAMERA_PERMISSION
                )
                return
            }
            manager.openCamera(cameraId, stateCallback, null)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {

            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show()
                finish()


            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
     override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView!!.isAvailable)
            openCamera()
        else
            textureView!!.surfaceTextureListener = this

    }

     @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
     override fun onPause() {
        stopBackgroundThread()
        super.onPause()
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()

        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }
}



