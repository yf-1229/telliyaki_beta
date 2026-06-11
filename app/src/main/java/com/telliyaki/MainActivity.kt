package com.telliyaki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.telliyaki.ui.navigation.AppNavHost
import com.telliyaki.ui.theme.TelloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TelloTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
