package com.icl.surveillance.fhir

import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Organization

class ResourceCreationHelper {
    fun createLocations(): List<Location> {
        val locations = mutableListOf<Location>()
        return locations
    }

    fun createRespectiveOrganization(): List<Organization> {
        val locations = mutableListOf<Organization>()
        return locations
    }

}