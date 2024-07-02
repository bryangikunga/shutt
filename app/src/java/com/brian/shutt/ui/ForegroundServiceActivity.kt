package com.addie.timesapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.addie.timesapp.R

/**
 * Activity that displays the reason why foreground service notification is required
 * Launched when foreground service notification is tapped
 */
class ForegroundServiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_foreground_service)
    }
}