package com.wavit.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wavit.android.ui.navigation.WavitNavHost
import com.wavit.android.ui.theme.WavitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WavitTheme {
                WavitNavHost()
            }
        }
    }
}

