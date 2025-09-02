package com.icl.surveillance.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.get
import com.google.gson.Gson
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.ui.patients.responses.EditChecklistActivity
import com.icl.surveillance.utils.readFileFromAssets
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource


class EditSupervisorChecklistViewModel(
    application: Application,
    private val questionnaireId: String,
    private val questionnaire: String
) :
    AndroidViewModel(application) {
    private val fhirEngine: FhirEngine = FhirApplication.fhirEngine(application.applicationContext)

    val liveEditData = liveData { emit(prepareEditRecord()) }

    private suspend fun prepareEditRecord(): Pair<String, String> {
        // This is actually a QuestionnaireResponse, not a Patient
        val questionnaireResponse = fhirEngine.get<QuestionnaireResponse>(questionnaireId)

        // Read the original Questionnaire from assets
        val questionnaireJson =
            getApplication<Application>()
                .readFileFromAssets(questionnaire)
                .trimIndent()

        // Parse the Questionnaire
        val parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
        val questionnaire =
            parser.parseResource(Questionnaire::class.java, questionnaireJson) as Questionnaire

        // Convert the existing QuestionnaireResponse to JSON string
        val questionnaireResponseJson = parser.encodeResourceToString(questionnaireResponse)


        return questionnaireJson to questionnaireResponseJson
    }


    val isResourcesSaved = MutableLiveData<Boolean>()

    /**
     * Update patient registration questionnaire response into the application database.
     *
     * @param questionnaireResponse patient registration questionnaire response
     */
    fun updatePatient(questionnaireResponse: QuestionnaireResponse) {
        viewModelScope.launch {
            questionnaireResponse.id = questionnaireId
            fhirEngine.update(questionnaireResponse)
            isResourcesSaved.value = true
            return@launch
        }
    }


}