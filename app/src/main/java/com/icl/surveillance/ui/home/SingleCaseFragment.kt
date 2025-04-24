package com.icl.surveillance.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.icl.surveillance.R
import com.icl.surveillance.adapters.DiseasesRecyclerViewAdapter
import com.icl.surveillance.adapters.HomeRecyclerViewAdapter
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.clients.AddParentCaseActivity
import com.icl.surveillance.databinding.FragmentSingleCaseBinding
import com.icl.surveillance.utils.FormatterClass
import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass. Use the [SingleCaseFragment.newInstance] factory method to create
 * an instance of this fragment.
 */
class SingleCaseFragment : Fragment() {
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

    private var _binding: FragmentSingleCaseBinding? = null

    private val viewModel: HomeViewModel by viewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSingleCaseBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleName = FormatterClass().getSharedPref("title", requireContext())

        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        // Let the Fragment receive menu callbacks
        setHasOptionsMenu(true)

        binding.apply {
            greeting.text = titleName
        }
        val adapter =
            DiseasesRecyclerViewAdapter(::onItemClick).apply { submitList(viewModel.getDiseasesList()) }
        val recyclerView = requireView().findViewById<RecyclerView>(R.id.sdcLayoutsRecyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(context, 1)
    }

    private fun onItemClick(layout: HomeViewModel.Diseases) {
        val title = context?.getString(layout.textId) ?: ""
        when (layout.count) {
            0 -> {
                val bundle =
                    Bundle().apply { putString(QUESTIONNAIRE_FILE_PATH_KEY, "add-case.json") }

                FormatterClass()
                    .saveSharedPref(
                        "title", title,
                        requireContext()
                    )

                findNavController().navigate(
                    R.id.action_singleCaseFragment_to_caseSelectionFragment,
                    bundle
                )
            }

            else -> {

                val bundle =
                    Bundle().apply { putString(QUESTIONNAIRE_FILE_PATH_KEY, "afp-case.json") }

                FormatterClass()
                    .saveSharedPref(
                        "title", title,
                        requireContext()
                    )

                findNavController().navigate(
                    R.id.action_singleCaseFragment_to_caseSelectionFragment,
                    bundle
                )
            }
        }

    }
//        binding.apply {
//            val bundle =
//                Bundle().apply { putString(QUESTIONNAIRE_FILE_PATH_KEY, "add-case.json") }
//            btnAdd.setOnClickListener {
//
//                testActions()
////                FormatterClass().saveSharedPref("title", "Add Case", requireContext())
////                findNavController().navigate(
////                    R.id.action_singleCaseFragment_to_addClientFragment,
////                    bundle
////                )
//
//
////                FormatterClass().saveSharedPref(
////                    "questionnaire",
////                    "add-case.json",
////                    requireContext()
////                )
////                val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
////                intent.putExtra("title", "Add Case")
////                intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "add-case.json")
////                startActivity(intent)
//            }
//            btnList.setOnClickListener {
//                findNavController().navigate(
//                    R.id.action_singleCaseFragment_to_navigation_dashboard,
//                    bundle
//                )
//            }
//        }
//    }

//    private fun testActions() {
//        val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_alert_dialog, null)
//
//        val alertDialog = AlertDialog.Builder(requireContext())
//            .setView(dialogView)
//            .create()
//
//        val titleText = dialogView.findViewById<TextView>(R.id.customTitle)
//        val messageText = dialogView.findViewById<TextView>(R.id.customMessage)
//        val afpButton = dialogView.findViewById<MaterialButton>(R.id.afpButton)
//        val measlesButton = dialogView.findViewById<MaterialButton>(R.id.measlesButton)
//
//        titleText.text = getString(R.string.case_selection)
//        messageText.text = getString(R.string.please_select_an_option_below_to_proceed)
//
//        afpButton.setOnClickListener {
//            FormatterClass().saveSharedPref("title", "Add AFP Case", requireContext())
//            FormatterClass().saveSharedPref(
//                "questionnaire",
//                "afp-case.json",
//                requireContext()
//            )
//            val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
//            intent.putExtra("title", "Add AFP Case")
//            intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "afp-case.json")
//            startActivity(intent)
//            alertDialog.dismiss()
//        }
//        measlesButton.setOnClickListener {
//            FormatterClass().saveSharedPref("title", "Add Measles Case", requireContext())
//            FormatterClass().saveSharedPref(
//                "questionnaire",
//                "add-case.json",
//                requireContext()
//            )
//            val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
//            intent.putExtra("title", "Add Measles Case")
//            intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "add-case.json")
//            startActivity(intent)
//            alertDialog.dismiss()
//        }
//
//        alertDialog.show()
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle the back button in the toolbar
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of this fragment using the provided
         * parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SingleCaseFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SingleCaseFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
            }
    }
}
