package com.idlix.mobile.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idlix.data.api.ApiClient
import com.idlix.data.repository.SubdlRepository
import com.idlix.data.repository.SubsourceRepository
import com.idlix.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val activeBackendUrl: String = "",
    val activeSubdlKey: String = "",
    val backendUrl: String = "",
    val subdlKey: String = "",
    val activeDnsProvider: String = ApiClient.DEFAULT_DNS_PROVIDER,
    val selectedDnsProvider: String = ApiClient.DEFAULT_DNS_PROVIDER,
    val isTestingBackend: Boolean = false,
    val backendTestResult: String? = null,
    val isTestingSubdl: Boolean = false,
    val subdlTestResult: String? = null,
    val activeSubsourceKey: String = "",
    val subsourceKey: String = "",
    val isTestingSubsource: Boolean = false,
    val subsourceTestResult: String? = null,
    val isTestingDns: Boolean = false,
    val dnsTestResult: String? = null,
    val cacheCleared: Boolean = false
)

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            activeBackendUrl = ApiClient.getBaseUrl(),
            activeSubdlKey = SubdlRepository.getApiKey(),
            backendUrl = ApiClient.getBaseUrl(),
            subdlKey = SubdlRepository.getApiKey(),
            activeSubsourceKey = SubsourceRepository.getApiKey(),
            subsourceKey = SubsourceRepository.getApiKey(),
            activeDnsProvider = ApiClient.getDnsProvider(),
            selectedDnsProvider = ApiClient.getDnsProvider()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateBackendUrl(url: String) {
        _uiState.value = _uiState.value.copy(backendUrl = url, backendTestResult = null)
    }

    fun updateSubdlKey(key: String) {
        _uiState.value = _uiState.value.copy(subdlKey = key, subdlTestResult = null)
    }

    fun resetBackendUrl() {
        val defaultUrl = ApiClient.getDefaultUrl()
        _uiState.value = _uiState.value.copy(backendUrl = defaultUrl, backendTestResult = null)
    }

    fun resetSubdlKey() {
        val defaultKey = SubdlRepository.DEFAULT_API_KEY
        _uiState.value = _uiState.value.copy(subdlKey = defaultKey, subdlTestResult = null)
    }

    fun updateSubsourceKey(key: String) {
        _uiState.value = _uiState.value.copy(subsourceKey = key, subsourceTestResult = null)
    }

    fun resetSubsourceKey() {
        _uiState.value = _uiState.value.copy(subsourceKey = "", subsourceTestResult = null)
    }

    fun saveBackendUrl() {
        val app = getApplication<Application>()
        val targetUrl = _uiState.value.backendUrl.trim()
        if (targetUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(backendTestResult = app.getString(R.string.url_empty_error))
            return
        }
        ApiClient.setBaseUrl(targetUrl)
        val newActive = ApiClient.getBaseUrl()
        _uiState.value = _uiState.value.copy(
            activeBackendUrl = newActive,
            backendUrl = newActive,
            backendTestResult = app.getString(R.string.url_saved_success)
        )
    }

    fun saveSubdlKey() {
        val app = getApplication<Application>()
        val targetKey = _uiState.value.subdlKey.trim()
        SubdlRepository.setApiKey(targetKey)
        val newActive = SubdlRepository.getApiKey()
        _uiState.value = _uiState.value.copy(
            activeSubdlKey = newActive,
            subdlKey = newActive,
            subdlTestResult = app.getString(R.string.subdl_key_saved_success)
        )
    }

    fun saveSubsourceKey() {
        val app = getApplication<Application>()
        val targetKey = _uiState.value.subsourceKey.trim()
        SubsourceRepository.setApiKey(targetKey)
        val newActive = SubsourceRepository.getApiKey()
        _uiState.value = _uiState.value.copy(
            activeSubsourceKey = newActive,
            subsourceKey = newActive,
            subsourceTestResult = app.getString(R.string.subsource_key_saved_success)
        )
    }

    fun testBackend() {
        val app = getApplication<Application>()
        val targetUrl = _uiState.value.backendUrl.trim()
        if (targetUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isTestingBackend = false,
                backendTestResult = app.getString(R.string.url_empty_error)
            )
            return
        }
        if (!targetUrl.startsWith("http://", ignoreCase = true) && !targetUrl.startsWith("https://", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(
                isTestingBackend = false,
                backendTestResult = app.getString(R.string.url_invalid_protocol)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingBackend = true, backendTestResult = null)
            val result = ApiClient.testBackendConnection(targetUrl)
            _uiState.value = _uiState.value.copy(
                isTestingBackend = false,
                backendTestResult = result.fold(
                    onSuccess = { it },
                    onFailure = { "Failed: ${it.localizedMessage ?: "Unknown network error"}" }
                )
            )
        }
    }

    fun testSubsource() {
        val app = getApplication<Application>()
        val targetKey = _uiState.value.subsourceKey.trim()
        if (targetKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isTestingSubsource = false,
                subsourceTestResult = app.getString(R.string.subsource_key_empty_error)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingSubsource = true, subsourceTestResult = null)
            val result = SubsourceRepository.testApiKey(targetKey)
            _uiState.value = _uiState.value.copy(
                isTestingSubsource = false,
                subsourceTestResult = result.fold(
                    onSuccess = { it },
                    onFailure = { "Failed: ${it.localizedMessage ?: "Unknown network error"}" }
                )
            )
        }
    }

    fun testSubdl() {
        val app = getApplication<Application>()
        val targetKey = _uiState.value.subdlKey.trim()
        if (targetKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isTestingSubdl = false,
                subdlTestResult = app.getString(R.string.subdl_key_empty_error)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingSubdl = true, subdlTestResult = null)
            val result = SubdlRepository.testApiKey(targetKey)
            _uiState.value = _uiState.value.copy(
                isTestingSubdl = false,
                subdlTestResult = result.fold(
                    onSuccess = { it },
                    onFailure = { "Failed: ${it.localizedMessage ?: "Unknown network error"}" }
                )
            )
        }
    }

    fun updateDnsProvider(provider: String) {
        _uiState.value = _uiState.value.copy(selectedDnsProvider = provider, dnsTestResult = null)
    }

    fun saveDnsProvider() {
        val app = getApplication<Application>()
        val target = _uiState.value.selectedDnsProvider
        ApiClient.setDnsProvider(target)
        val active = ApiClient.getDnsProvider()
        _uiState.value = _uiState.value.copy(
            activeDnsProvider = active,
            selectedDnsProvider = active,
            dnsTestResult = app.getString(R.string.dns_saved_success)
        )
    }

    fun resetDnsProvider() {
        val defaultProvider = ApiClient.DEFAULT_DNS_PROVIDER
        _uiState.value = _uiState.value.copy(selectedDnsProvider = defaultProvider, dnsTestResult = null)
    }

    fun testDns() {
        val target = _uiState.value.selectedDnsProvider
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingDns = true, dnsTestResult = null)
            val result = ApiClient.testDns(target)
            _uiState.value = _uiState.value.copy(
                isTestingDns = false,
                dnsTestResult = result.fold(
                    onSuccess = { it },
                    onFailure = { "Failed: ${it.localizedMessage ?: "DNS resolution failed"}" }
                )
            )
        }
    }

    fun clearImageCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                val cacheDir = context.cacheDir.resolve("image_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(cacheCleared = true)
                }
            } catch (_: Exception) {}
        }
    }
}
