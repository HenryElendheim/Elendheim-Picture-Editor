package com.elendheim.pictureeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.pictureeditor.ui.EditorScreen
import com.elendheim.pictureeditor.ui.EditorViewModel
import com.elendheim.pictureeditor.ui.SettingsScreen
import com.elendheim.pictureeditor.ui.SplashScreen
import com.elendheim.pictureeditor.ui.theme.ElendheimTheme

// The three places the app can be. Kept deliberately simple, no nav library.
private enum class Screen { SPLASH, EDITOR, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // One shared view model drives every screen.
            val vm: EditorViewModel = viewModel()

            ElendheimTheme(highContrast = vm.settings.highContrast) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var screen by remember { mutableStateOf(Screen.SPLASH) }

                    // On settings, the system back button returns to the editor.
                    BackHandler(enabled = screen == Screen.SETTINGS) {
                        screen = Screen.EDITOR
                    }

                    when (screen) {
                        Screen.SPLASH -> SplashScreen(
                            reduceMotion = vm.settings.reduceMotion,
                            onDone = { screen = Screen.EDITOR }
                        )
                        Screen.EDITOR -> EditorScreen(
                            vm = vm,
                            onOpenSettings = { screen = Screen.SETTINGS }
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            vm = vm,
                            onBack = { screen = Screen.EDITOR }
                        )
                    }
                }
            }
        }
    }
}
