package com.example.macmini6.new_timeruler_2018

import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore.Images.ImageColumns.ORIENTATION
import android.support.annotation.RequiresApi
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import kotlinx.android.synthetic.main.activity_facecamera.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.ArrayList

class MainActivity : AppCompatActivity() {


 override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



     login_btn.setOnClickListener {

         val intent = Intent(this,Facecamera::class.java)

         startActivity(intent)
     }

    }





}
