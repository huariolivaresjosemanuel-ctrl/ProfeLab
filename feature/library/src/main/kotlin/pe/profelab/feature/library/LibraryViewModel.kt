package pe.profelab.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.profelab.core.model.*

data class LibraryState(val loading: Boolean=true, val sources: List<SourceReference> = emptyList(), val error: String?=null)
class LibraryViewModel(private val repository: EducationalSourceRepository) : ViewModel() {
    private val _state=MutableStateFlow(LibraryState())
    val state=_state.asStateFlow()
    init { reload() }
    fun reload() { viewModelScope.launch {
        _state.value=LibraryState()
        try { _state.value=LibraryState(false,repository.sources()) }
        catch(e: CancellationException) { throw e }
        catch (_: Exception) { _state.value=LibraryState(false,error="No se pudo abrir el catálogo local. Inténtalo de nuevo.") }
    } }
}
