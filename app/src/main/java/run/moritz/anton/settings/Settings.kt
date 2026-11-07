package run.moritz.anton.settings

import android.content.Context
import androidx.core.content.edit
import run.moritz.anton.cases.Sex

/** The player's choices. [titleForm] picks the grammatical form of rank titles. */
data class Settings(val volumeKeysTalk: Boolean = true, val radioSounds: Boolean = true, val titleForm: Sex = Sex.MALE)

/** Keeps [Settings] across app starts. */
interface SettingsStore {
    fun load(): Settings

    fun save(settings: Settings)
}

class SharedPreferencesSettingsStore(context: Context) : SettingsStore {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override fun load() = Settings(
        volumeKeysTalk = preferences.getBoolean(VOLUME_KEYS, true),
        radioSounds = preferences.getBoolean(RADIO_SOUNDS, true),
        titleForm = Sex.valueOf(preferences.getString(TITLE_FORM, null) ?: Sex.MALE.name)
    )

    override fun save(settings: Settings) = preferences.edit {
        putBoolean(VOLUME_KEYS, settings.volumeKeysTalk)
        putBoolean(RADIO_SOUNDS, settings.radioSounds)
        putString(TITLE_FORM, settings.titleForm.name)
    }

    private companion object {
        const val VOLUME_KEYS = "volume_keys_talk"
        const val RADIO_SOUNDS = "radio_sounds"
        const val TITLE_FORM = "title_form"
    }
}
