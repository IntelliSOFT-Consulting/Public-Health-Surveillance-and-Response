package com.icl.surveillance.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.search
import com.icl.surveillance.ui.patients.PatientListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import kotlin.let

class ResponseDetailsViewModel(
    application: Application,
    private val fhirEngine: FhirEngine,
    private val questionnaireId: String,
) : AndroidViewModel(application) {
    val liveSummaryData = MutableLiveData<PatientListViewModel.CaseDetailSummaryData>()
    fun getInfoSummaryData(slug: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val patientData = getInfoSummary(slug)
            withContext(Dispatchers.Main) { liveSummaryData.value = patientData }
        }
    }

    private suspend fun getInfoSummary(slug: String): PatientListViewModel.CaseDetailSummaryData {
        val searchResult =
            fhirEngine.search<QuestionnaireResponse> {
                filter(
                    Resource.RES_ID,
                    { value = of(questionnaireId) })
            }
        var logicalId = ""
        var observations = mutableListOf<PatientListViewModel.ObservationItem>()
        searchResult.first().let {
            logicalId = it.resource.logicalId
            it.resource.item.forEach { k ->
                k.item.forEach { j ->
                    val answer = j.answerFirstRep
                    val value = when {
                        answer.hasValueReference() -> answer.valueReference.display
                        answer.hasValueCoding() -> answer.valueCoding.display
                        answer.hasValueStringType() -> answer.valueStringType.value
                        else -> "N/A"
                    }

                    val obs = PatientListViewModel.ObservationItem(
                        id = j.linkId,
                        code = j.linkId,
                        value = value ?: "N/A",
                        created = j.linkId
                    )
                    observations.add(obs)
                }
            }

        }
        return PatientListViewModel.CaseDetailSummaryData(
            logicalId = logicalId,
            encounterId = "encounterId",
            name = "name",
            dob = "dob",
            sex = "sex",
            observations = observations,
            epidNo = "epidNo"
        )
    }
}