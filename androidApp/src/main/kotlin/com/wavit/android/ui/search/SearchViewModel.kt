package com.wavit.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavit.models.Song
import com.wavit.repository.WavitApiRepository
import com.wavit.repository.WavitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for Search Screen.
 */
data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false
)

class SearchViewModel(
    private val repository: WavitRepository = WavitApiRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, errorMessage = null) }
    }

    fun onSearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, hasSearched = true) }
            repository.searchSongs(query)
                .onSuccess { songs ->
                    _uiState.update { it.copy(isLoading = false, songs = songs, errorMessage = null) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            songs = emptyList(),
                            errorMessage = throwable.message ?: "Failed to search songs"
                        )
                    }
                }
        }
    }

    fun clearSearch() {
        _uiState.update { SearchUiState() }
    }
}

