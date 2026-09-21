package com.vyrncore.palestra.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Plays (or, for a curated YouTube search, shows) a video tutorial inside the app via WebView,
 * instead of handing off to the YouTube app/browser - the allievo never leaves the workout flow
 * to watch a demo. Only used for YouTube content already vetted by [com.vyrncore.palestra.util.ExerciseMedia],
 * so loading arbitrary external pages here isn't a concern.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppVideoDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Video tutorial") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Chiudi")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White, navigationIconContentColor = Color.White),
                    )
                },
                containerColor = Color.Black,
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                webChromeClient = WebChromeClient()
                                loadUrl(url)
                            }
                        },
                    )
                }
            }
        }
    }
}
