package run.moritz.anton

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import run.moritz.anton.ui.AntonApp
import run.moritz.anton.ui.AntonViewModel
import run.moritz.anton.ui.Classic

class MainActivity : ComponentActivity() {
    private val model: AntonViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val application = application as AntonApplication
                return AntonViewModel(checkNotNull(application.container)) as T
            }
        }
    }

    @get:VisibleForTesting
    val testModel: AntonViewModel get() = model

    private val requestMicrophone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) model.onMicrophoneGranted() else model.onMicrophoneDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Classic.Title.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Classic.Window.toArgb(), Classic.Window.toArgb())
        )
        super.onCreate(savedInstanceState)
        setContent {
            AntonApp(
                model = model,
                onTalk = ::talk,
                onAllowMicrophone = { requestMicrophone.launch(Manifest.permission.RECORD_AUDIO) },
                onOpenSystemSettings = ::openSystemSettings
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val granted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) model.onMicrophoneGranted()
    }

    /** Leaving the app must never keep the microphone open. */
    override fun onStop() {
        model.talk(false)
        super.onStop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isTalkKey(keyCode)) return super.onKeyDown(keyCode, event)
        if (event.repeatCount == 0) talk(true)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!isTalkKey(keyCode)) return super.onKeyUp(keyCode, event)
        talk(false)
        return true
    }

    private fun isTalkKey(keyCode: Int): Boolean {
        val volumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP
        return volumeKey && model.capturesVolumeKeys
    }

    private fun talk(pressed: Boolean) {
        if (pressed) window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        model.talk(pressed)
    }

    private fun openSystemSettings() {
        val uri = Uri.fromParts("package", packageName, null)
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
    }
}
