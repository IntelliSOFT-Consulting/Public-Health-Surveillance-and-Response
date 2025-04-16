package com.icl.surveillance.ui.patients.data

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
  override fun getItemCount(): Int = 5

  override fun createFragment(position: Int): Fragment {
    return when (position) {
      0 -> ReportingSiteFragment()
      1 -> IdentificationFragment()
      2 -> ClinicalInformationFragment()
      3 -> CaseInformationFragment()
      4 -> LabInformationFragment()
      else -> ReportingSiteFragment()
    }
  }
}
