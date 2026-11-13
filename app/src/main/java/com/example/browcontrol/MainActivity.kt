package com.example.browcontrol

import android.app.Activity
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.Gravity
import android.view.ViewGroup
import android.graphics.Color

class MainActivity : Activity() {

    private val cameraRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "Brow Control"
            textSize = 28f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "Control phone scrolling using eyebrow movements"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
        }

        val cameraButton = Button(this).apply {
            text = "Enable Camera"
            setOnClickListener {
                requestCameraPermission()
            }
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(
            cameraButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(layout)
    }

    private fun requestCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                this,
                "Camera permission granted",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                cameraRequestCode
            )
        }
    }
}
