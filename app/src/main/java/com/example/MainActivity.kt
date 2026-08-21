package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.ChatScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.UltronBackground

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var currentScreen by remember { mutableStateOf("chat") }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = UltronBackground
        ) {
          when (currentScreen) {
            "chat" -> {
              ChatScreen(
                onNavigateToSettings = { currentScreen = "settings" }
              )
            }
            "settings" -> {
              SettingsScreen(
                onNavigateToChat = { currentScreen = "chat" }
              )
            }
          }
        }
      }
    }
  }
}


