package com.karelisio.azur.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karelisio.azur.data.PlaceRepository
import com.karelisio.azur.data.net.OpenMeteoParser
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val results: List<OpenMeteoParser.GeoResult> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
class PlacesViewModel(private val repo: PlaceRepository) : ViewModel() {
    val places = repo.places

    private val query = MutableStateFlow("")
    private val _search = MutableStateFlow(SearchState())
    val search: StateFlow<SearchState> = _search

    init {
        viewModelScope.launch {
            query.debounce(350).collectLatest { q ->
                if (q.trim().length < 2) {
                    _search.update { it.copy(results = emptyList(), loading = false, error = null) }
                    return@collectLatest
                }
                _search.update { it.copy(loading = true, error = null) }
                runCatching { repo.search(q) }
                    .onSuccess { r -> _search.update { it.copy(results = r, loading = false) } }
                    .onFailure { _search.update { it.copy(loading = false, error = "Recherche impossible (hors ligne ?)") } }
            }
        }
    }

    fun onQuery(q: String) {
        _search.update { it.copy(query = q) }
        query.value = q
    }

    fun add(result: OpenMeteoParser.GeoResult, onAdded: (Long) -> Unit) {
        viewModelScope.launch { onAdded(repo.addFavorite(result)) }
    }

    fun remove(id: Long) = viewModelScope.launch { repo.remove(id) }

    fun reorder(ids: List<Long>) = viewModelScope.launch { repo.reorder(ids) }
}
