package com.icl.surveillance.fhir


class FhirRepository(
    private val viewModel: MpoxUploadViewModel,
) {
    fun handleDataUpload(slug: String) {
        viewModel.prepareUploadData(slug)
    }
}