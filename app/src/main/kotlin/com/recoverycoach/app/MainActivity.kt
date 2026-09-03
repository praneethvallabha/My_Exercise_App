package com.recoverycoach.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.recoverycoach.app.ui.RecoveryApp
import com.recoverycoach.app.ui.theme.RecoveryCoachTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecoveryCoachTheme {
                RecoveryApp()
            }
        }
    }
}
