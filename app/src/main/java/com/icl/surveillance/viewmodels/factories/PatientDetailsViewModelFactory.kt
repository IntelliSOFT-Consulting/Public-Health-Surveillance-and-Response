package com.icl.surveillance.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.icl.surveillance.viewmodels.ClientDetailsViewModel

class PatientDetailsViewModelFactory(
    private val application: Application,
    private val fhirEngine: FhirEngine,
    private val patientId: String,
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    require(modelClass.isAssignableFrom(ClientDetailsViewModel::class.java)) {
      "Unknown ViewModel class"
    }
    return ClientDetailsViewModel(application, fhirEngine, patientId) as T
  }
}
