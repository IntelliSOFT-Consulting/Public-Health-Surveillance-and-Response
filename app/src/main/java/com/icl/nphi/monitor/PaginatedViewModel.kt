package com.icl.nphi.monitor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Resource

class PaginatedViewModel(private val fhirEngine: FhirEngine) : ViewModel() {

    private val repository = FhirPaginatedRepository(fhirEngine)

    private val _resources = MutableStateFlow<List<Resource>>(emptyList())
    val resources: StateFlow<List<Resource>> = _resources.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var currentResourceType = "Patient"

    fun loadFirstPage(resourceType: String) {
        currentResourceType = resourceType
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val firstPage = repository.getFirstPage(resourceType)
                _resources.value = firstPage
                _hasMore.value = repository.hasMore(firstPage)
            } catch (e: Exception) {
                Log.e("PaginatedVM", "Error loading first page: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || !_hasMore.value) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val nextPage = repository.getNextPage(currentResourceType)
                if (nextPage.isNotEmpty()) {
                    val currentList = _resources.value.toMutableList()
                    currentList.addAll(nextPage)
                    _resources.value = currentList
                    _hasMore.value = repository.hasMore(nextPage)
                } else {
                    _hasMore.value = false
                }
            } catch (e: Exception) {
                Log.e("PaginatedVM", "Error loading next page: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changeResourceType(resourceType: String) {
        if (resourceType != currentResourceType) {
            loadFirstPage(resourceType)
        }
    }
}