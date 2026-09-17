package com.vyrncore.palestra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.RootViewModel
import com.vyrncore.palestra.ui.navigation.PalestraNavGraph
import com.vyrncore.palestra.ui.theme.PalestraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val rootViewModel: RootViewModel = hiltViewModel()
            val themeMode by rootViewModel.themeMode.collectAsState()
            PalestraTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PalestraNavGraph(rootViewModel = rootViewModel)
                }
            }
        }
    }
}
