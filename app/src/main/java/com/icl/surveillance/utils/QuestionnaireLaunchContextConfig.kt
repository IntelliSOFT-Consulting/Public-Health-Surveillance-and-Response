package com.icl.surveillance.utils

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import java.io.Serializable
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource

object QuestionnaireLaunchContextKeys {
    const val QUESTIONNAIRE_LAUNCH_CONTEXTS_KEY = "questionnaire-launch-contexts"
}

data class CodingEntry(
    val system: String?,
    val code: String?,
    val display: String?
) : Serializable {
    fun toCoding(): Coding =
        Coding().apply {
            system = this@CodingEntry.system
            code = this@CodingEntry.code
            display = this@CodingEntry.display
        }

    companion object {
        fun fromCoding(coding: Coding): CodingEntry =
            CodingEntry(coding.system, coding.code, coding.display)
    }
}

data class QuestionnaireLaunchContextEntry(
    val nameCoding: CodingEntry?,
    val resourceJson: String
) : Serializable {
    fun toLaunchContext(): QuestionnaireFragment.QuestionnaireLaunchContext {
        val resource = JSON_PARSER.parseResource(resourceJson) as Resource
        return QuestionnaireFragment.QuestionnaireLaunchContext(
            nameCoding?.toCoding(),
            resource
        )
    }

    companion object {
        private val FHIR_CONTEXT: FhirContext = FhirContext.forCached(FhirVersionEnum.R4)
        private val JSON_PARSER = FHIR_CONTEXT.newJsonParser()

        fun fromResource(
            nameCoding: Coding?,
            resource: Resource
        ): QuestionnaireLaunchContextEntry =
            QuestionnaireLaunchContextEntry(
                nameCoding?.let { CodingEntry.fromCoding(it) },
                JSON_PARSER.encodeResourceToString(resource)
            )

        fun toLaunchContexts(
            entries: List<QuestionnaireLaunchContextEntry>
        ): List<QuestionnaireFragment.QuestionnaireLaunchContext> =
            entries.map { it.toLaunchContext() }
    }
}

object QuestionnaireLaunchContextFactory {
    private val CONTEXT_CODING = Coding().apply {
        system = "http://hl7.org/fhir/uv/sdc/CodeSystem/launchContext"
        code = "client"
        display = "Client as a QuestionnaireResponse"
    }

    fun defaultLocationLaunchContexts(
        context: Context
    ): List<QuestionnaireLaunchContextEntry> {
        val formatter = FormatterClass()
        val countyReference = formatter.getSharedPref("county", context)
        val countyName = formatter.getSharedPref("countyName", context)
        val subCountyReference = formatter.getSharedPref("subCounty", context)
        val subCountyName = formatter.getSharedPref("subCountyName", context)
        val wardReference = formatter.getSharedPref("ward", context)
        val wardName = formatter.getSharedPref("wardName", context)
        val facilityReference = formatter.getSharedPref("facility", context)
        val facilityName = formatter.getSharedPref("facilityName", context)

        val hasReference = listOf(
            countyReference,
            subCountyReference,
            wardReference,
            facilityReference
        ).any { !it.isNullOrBlank() }
        if (!hasReference) {
            return emptyList()
        }

        val response = QuestionnaireResponse().apply {
            status = QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED
            listOf(
                LaunchItem("294367770999", countyReference, countyName),
                LaunchItem("438862163919", countyReference, countyName),
                LaunchItem("a4-county", countyReference, countyName),
                LaunchItem("819946803642", subCountyReference, subCountyName),
                LaunchItem("a3-sub-county", subCountyReference, subCountyName),
                LaunchItem("819943434", wardReference, wardName),
                LaunchItem("819946803677", facilityReference, facilityName)
            ).forEach { item ->
                item.reference?.takeIf { it.isNotBlank() }?.let { referenceValue ->
                    addItem().apply {
                        linkId = item.linkId
                        addAnswer().apply {
                            value = Reference(referenceValue).apply {
                                item.display?.takeIf { it.isNotBlank() }?.let { display = it }
                            }
                        }
                    }
                }
            }
        }

        return listOf(
            QuestionnaireLaunchContextEntry.fromResource(
                CONTEXT_CODING,
                response
            )
        )
    }

    private data class LaunchItem(
        val linkId: String,
        val reference: String?,
        val display: String?
    )
}
