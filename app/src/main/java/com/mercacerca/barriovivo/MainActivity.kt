package com.mercacerca.barriovivo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mercacerca.barriovivo.ui.navigation.BarrioVivoNavHost
import com.mercacerca.barriovivo.ui.theme.BarrioVivoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarrioVivoTheme { BarrioVivoNavHost() }
        }
    }
}
