package com.icl.surveillance.ui.patients.data

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.databinding.FragmentLabInformationBinding
import com.icl.surveillance.ui.patients.AddCaseActivity
import com.icl.surveillance.utils.FormatterClass

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass. Use the [LabInformationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LabInformationFragment : Fragment() {
  // TODO: Rename and change types of parameters
  private var param1: String? = null
  private var param2: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      param1 = it.getString(ARG_PARAM1)
      param2 = it.getString(ARG_PARAM2)
    }
  }

  private var _binding: FragmentLabInformationBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding
    get() = _binding!!

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {

    _binding = FragmentLabInformationBinding.inflate(inflater, container, false)
    val root: View = binding.root

    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.apply {
      fab.setOnClickListener {
        FormatterClass().saveSharedPref("questionnaire", "measles-lab.json", requireContext())
        val intent = Intent(requireContext(), AddCaseActivity::class.java)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "measles-lab.json")
        startActivity(intent)
      }
    }
  }

  companion object {
    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LabInformationFragment.
     */
    // TODO: Rename and change types and number of parameters
    @JvmStatic
    fun newInstance(param1: String, param2: String) =
        LabInformationFragment().apply {
          arguments =
              Bundle().apply {
                putString(ARG_PARAM1, param1)
                putString(ARG_PARAM2, param2)
              }
        }
  }
}
