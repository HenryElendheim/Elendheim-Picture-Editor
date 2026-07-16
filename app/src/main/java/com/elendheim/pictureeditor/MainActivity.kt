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
import com.elendheim.pictureeditor.ui.CropScreen
import com.elendheim.pictureeditor.ui.EditorScreen
import com.elendheim.pictureeditor.ui.EditorViewModel
import com.elendheim.pictureeditor.ui.SettingsScreen
import com.elendheim.pictureeditor.ui.SplashScreen
import com.elendheim.pictureeditor.ui.theme.ElendheimTheme

// The places the app can be. Kept deliberately simple, no nav library.
private enum class Screen { SPLASH, EDITOR, SETTINGS, CROP }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: EditorViewModel = viewModel()

            ElendheimTheme(highContrast = vm.settings.highContrast) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // The intro only plays on the very first launch. After that
                    // the app opens straight into the editor.
                    var screen by remember {
                        mutableStateOf(if (vm.settings.introSeen) Screen.EDITOR else Screen.SPLASH)
                    }

                    BackHandler(enabled = screen == Screen.SETTINGS || screen == Screen.CROP) {
                        screen = Screen.EDITOR
                    }

                    when (screen) {
                        Screen.SPLASH -> SplashScreen(
                            reduceMotion = vm.settings.reduceMotion,
                            onDone = {
                                vm.markIntroSeen()
                                screen = Screen.EDITOR
                            }
                        )
                        Screen.EDITOR -> EditorScreen(
                            vm = vm,
                            onOpenSettings = { screen = Screen.SETTINGS },
                            onOpenCrop = { if (vm.previewBitmap != null) screen = Screen.CROP }
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            vm = vm,
                            onBack = { screen = Screen.EDITOR }
                        )
                        Screen.CROP -> {
                            val bmp = vm.previewBitmap
                            if (bmp == null) {
                                screen = Screen.EDITOR
                            } else {
                                CropScreen(
                                    source = bmp,
                                    transform = vm.editState.transform,
                                    onApply = { rect ->
                                        vm.setCrop(rect)
                                        screen = Screen.EDITOR
                                    },
                                    onCancel = { screen = Screen.EDITOR }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
