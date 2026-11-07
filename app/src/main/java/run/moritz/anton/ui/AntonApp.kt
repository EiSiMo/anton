package run.moritz.anton.ui

import androidx.compose.runtime.Composable

/** Chooses the screen. Everything before the first case waits for the microphone and the speech model. */
@Composable
fun AntonApp(
    model: AntonViewModel,
    onTalk: (Boolean) -> Unit,
    onAllowMicrophone: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    if (model.speech != SpeechState.Ready) {
        FirstStartScreen(model.speech, onAllowMicrophone, onOpenSystemSettings)
        return
    }
    when (model.screen) {
        Screen.STATION -> StationScreen(model)
        Screen.CASE -> CaseScreen(model, onTalk)
        Screen.TABLE -> SpellingTableScreen(model)
        Screen.SETTINGS -> SettingsScreen(model)
    }
}
