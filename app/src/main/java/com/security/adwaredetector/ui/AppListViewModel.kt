package com.security.adwaredetector.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.security.adwaredetector.model.RiskResult
import com.security.adwaredetector.scanner.AdwareScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado de la pantalla de auditoría manual.
 */
data class AppListUiState(
    val isLoading: Boolean = true,
    val results: List<RiskResult> = emptyList()
)

/**
 * ViewModel que orquesta el escaneo completo de apps instaladas usando AdwareScanner.
 * El escaneo se ejecuta en Dispatchers.IO porque recorre PackageManager para
 * cada paquete instalado, lo cual puede ser una operación relativamente costosa.
 */
class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = AdwareScanner(application.applicationContext)

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                scanner.scanAllInstalledApps()
            }
            _uiState.value = AppListUiState(isLoading = false, results = results)
        }
    }

    /** Elimina localmente un resultado de la lista, útil tras iniciar la desinstalación. */
    fun removeFromList(packageName: String) {
        _uiState.value = _uiState.value.copy(
            results = _uiState.value.results.filterNot { it.packageName == packageName }
        )
    }
}
