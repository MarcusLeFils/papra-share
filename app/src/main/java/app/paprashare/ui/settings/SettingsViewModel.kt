package app.paprashare.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.paprashare.data.PapraSettings
import app.paprashare.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    private val _settings = MutableStateFlow(PapraSettings())
    val settings: StateFlow<PapraSettings> = _settings

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    init {
        viewModelScope.launch {
            _settings.value = repository.settings.first()
        }
    }

    fun update(value: PapraSettings) {
        _settings.value = value
    }

    fun save() {
        viewModelScope.launch {
            repository.save(_settings.value)
            _saved.value = true
        }
    }

    fun resetSaved() {
        _saved.value = false
    }
}