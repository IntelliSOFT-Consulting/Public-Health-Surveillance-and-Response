package com.icl.surveillance.ui.home

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.icl.surveillance.R
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.databinding.FragmentHomeBinding
import com.icl.surveillance.utils.FormatterClass

class HomeFragment : Fragment() {

  private var _binding: FragmentHomeBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding
    get() = _binding!!

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    val root: View = binding.root

    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setUpActionBar()
    binding.apply {
      val fullText = "Hello, John Mdoe"
      val name = "John Mdoe"
      val spannable = SpannableString(fullText)
      val start = fullText.indexOf(name)
      val end = start + name.length
      spannable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      spannable.setSpan(RelativeSizeSpan(0.8f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

      // Set the styled text to the TextView
      greeting.text = spannable
      actionCard.setOnClickListener { handleActionClick() }

      //      btnList.setOnClickListener { handleActionClick() }
    }
  }

  private fun handleActionClick() {
    val bundle = Bundle().apply { putString(QUESTIONNAIRE_FILE_PATH_KEY, "add-client.json") }

    FormatterClass()
        .saveSharedPref("title", "MOH 502 - Case Based Reporting Form", requireContext())
    findNavController().navigate(R.id.action_navigation_home_to_single_case_fragment, bundle)
  }

  private fun setUpActionBar() {
    (requireActivity() as AppCompatActivity).supportActionBar?.apply {
      title = requireContext().getString(R.string.app_name)
      setDisplayHomeAsUpEnabled(true)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
