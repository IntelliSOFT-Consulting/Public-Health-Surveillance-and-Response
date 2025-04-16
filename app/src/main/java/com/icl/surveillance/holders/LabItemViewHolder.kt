package com.icl.surveillance.holders

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.icl.surveillance.databinding.LabHolderBinding
import com.icl.surveillance.ui.patients.PatientListViewModel

class LabItemViewHolder(binding: LabHolderBinding) : RecyclerView.ViewHolder(binding.root) {
  private val dateSpecimenReceivedView: TextView = binding.tvSpecimenReceivedDate
  private val specimenConditionView: TextView = binding.tvSpecimenCondition
  private val measlesIgMView: TextView = binding.tvMeaslesIgm
  private val rubellaIgMView: TextView = binding.tvRubellaIgm
  private val dateLabSentResultsView: TextView = binding.tvResultsSentDate
  private val finalClassificationView: TextView = binding.tvFinalClassification
  private val subcountyNameView: TextView = binding.tvSubcountyName
  private val subcountyDesignationView: TextView = binding.tvSubcountyDesignation
  private val subcountyPhoneView: TextView = binding.tvSubcountyPhone
  private val subcountyEmailView: TextView = binding.tvSubcountyEmail
  private val formCompletedByView: TextView = binding.tvFormCompletedBy
  private val nameOfPersonCompletingFormView: TextView = binding.tvFormCompleterName
  private val designationView: TextView = binding.tvDesignation
  private val signView: TextView = binding.tvSignature

  fun bindTo(
      patientItem: PatientListViewModel.CaseLabResultsData,
      onItemClicked: (PatientListViewModel.CaseLabResultsData) -> Unit,
  ) {
    this.dateSpecimenReceivedView.text = patientItem.dateSpecimenReceived
    this.specimenConditionView.text = patientItem.specimenCondition
    this.measlesIgMView.text = patientItem.measlesIgM
    this.rubellaIgMView.text = patientItem.rubellaIgM
    this.dateLabSentResultsView.text = patientItem.dateLabSentResults
    this.finalClassificationView.text = patientItem.finalClassification
    this.subcountyNameView.text = patientItem.subcountyName
    this.subcountyDesignationView.text = patientItem.subcountyDesignation
    this.subcountyPhoneView.text = patientItem.subcountyPhone
    this.subcountyEmailView.text = patientItem.subcountyEmail
    this.formCompletedByView.text = patientItem.formCompletedBy
    this.nameOfPersonCompletingFormView.text = patientItem.nameOfPersonCompletingForm
    this.designationView.text = patientItem.designation
    this.signView.text = patientItem.sign

    this.itemView.setOnClickListener { onItemClicked(patientItem) }
  }
}
