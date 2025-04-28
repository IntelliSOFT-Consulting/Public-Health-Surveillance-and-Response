package com.icl.surveillance.utils

import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Reference

class QuestionnaireHelper {

    fun codingQuestionnaire(code: String, display: String, text: String): Observation {
        val observation = Observation()
        observation.code.addCoding().setSystem("http://snomed.info/sct").setCode(code).display =
            display
        observation.code.text = text
        observation.valueStringType.value = text
        return observation
    }

    fun codingTimeQuestionnaire(code: String, display: String, text: String): Observation {
        val observation = Observation()
        observation.code.addCoding().setSystem("http://snomed.info/sct").setCode(code).display =
            display
        observation.code.text = text
        val date = FormatHelper().convertStringDate(text)
        observation.valueDateTimeType.value = date
        return observation
    }

    fun generalEncounter(basedOn: String?, encounter: String): Encounter {
        val enc = Encounter()
        enc.id = encounter
        if (basedOn != null) {
            val reference = Reference("Encounter/$basedOn")
            enc.partOf = reference
        }
        return enc
    }

    fun codingTimeAutoQuestionnaire(code: String, display: String, text: String): Observation {
        val observation = Observation()
        observation.code.addCoding().setSystem("http://snomed.info/sct").setCode(code).display =
            display
        observation.code.text = text
        val date = FormatHelper().convertStringDateParent(text)
        observation.valueDateTimeType.value = date
        return observation
    }

    fun quantityQuestionnaire(
        code: String,
        display: String,
        text: String,
        quantity: String,
        units: String
    ): Observation {
        val observation = Observation()
        observation.code.addCoding().setSystem("http://snomed.info/sct").setCode(code).display =
            display
        observation.code.text = text
        observation.value =
            Quantity()
                .setValue(quantity.toBigDecimal())
                .setUnit(units)
                .setSystem("http://unitsofmeasure.org")
        return observation
    }
}
