package com.icl.surveillance.ui.patients.data

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
  override fun getItemCount(): Int = 6

  override fun createFragment(position: Int): Fragment {
    return when (position) {
      0 -> ReportingSiteFragment()
      1 -> IdentificationFragment()
      3 -> ClinicalInformationFragment()
      2 -> CaseInformationFragment()
      4 -> LabInformationFragment()
      5 -> LabResultsFragment()
      else -> ReportingSiteFragment()
    }
  }
}
