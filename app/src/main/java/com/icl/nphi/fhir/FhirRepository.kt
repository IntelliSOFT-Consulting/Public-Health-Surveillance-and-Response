package com.icl.nphi.fhir


class FhirRepository(
    private val viewModel: MpoxUploadViewModel,
) {
    fun handleDataUpload(slug: String) {
        viewModel.prepareUploadData(slug)
    }
}