package com.tudorc.openair

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tudorc.openair.ui.OpenAirApp
import com.tudorc.openair.ui.theme.OpenAirTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val attributed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            newBase.createAttributionContext(ATTRIBUTION_TAG)
        } else {
            newBase
        }
        super.attachBaseContext(attributed)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenAirTheme {
                OpenAirApp()
            }
        }
    }
}

private const val ATTRIBUTION_TAG = "openair"
