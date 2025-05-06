package com.icl.surveillance.ui.patients.custom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.Gson
import com.icl.surveillance.R
import com.icl.surveillance.models.OutputGroup
import com.icl.surveillance.models.OutputItem

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [GroupFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GroupFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null


    private lateinit var parentLayout: LinearLayout
    private lateinit var group: OutputGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val groupJson = requireArguments().getString("group")
        group = Gson().fromJson(groupJson, OutputGroup::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentLayout = view.findViewById(R.id.ln_parent)

        // Now dynamically add fields
        addChildItems()
        val textView = view.findViewById<TextView>(R.id.tv_title)
        textView.text = "${group.text}"
    }

    private fun addChildItems() {
        for (item in group.items) {
            val fieldView = createCustomField(item)
            val show = checkRelatedWorkflows(group.text, item, group.items)
            if (show) {
                parentLayout.addView(fieldView)
            }
        }
    }


    private fun checkRelatedWorkflows(
        title: String,
        item: OutputItem,
        items: List<OutputItem>
    ): Boolean {
        var response = true
        val children = listOf("122072333233", "879612276990", "296206902941")

        if (item.linkId in children) {
            response = false
        }
        when (title.trim()) {
            "Vaccination History for disease under investigation" -> {
                response = false
                val parent = "970455623029"
                if (item.linkId == parent) {
                    response = true
                }
                val answer = items.find { it.linkId == parent }?.value
                if (answer == "Yes"){
                    response = true
                }
            }
            "Clinical Information"->{
                val parent = "970455623029"
            }
        }

        //
//        val parent = "measles-igm"
//        val child = "rubella-igm"
//        var parentResponse = getValueBasedOnId(parent, items)
//        var childResponse = getValueBasedOnId(child, items)
//        if (parentResponse.isNotEmpty()) {
//            if (parentResponse == "Positive" && currentId == child) {
//                show = false
//            }
//        }
        return response
    }

    private fun createCustomField(item: OutputItem): View {
        // Create the main LinearLayout to hold the views
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }

        // Create the horizontal LinearLayout for the two TextViews
        val horizontalLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // First TextView (label)
        val label = TextView(requireContext()).apply {
            text = item.text
            textSize = 12f
            setTextColor(android.graphics.Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        horizontalLayout.addView(label)

        // Second TextView (dynamic content)
        val tvEpiLink = TextView(requireContext()).apply {
            id = R.id.tv_epi_link // Set ID if needed for further reference
            text = item.value // Assuming item.text is the dynamic text you want to show
            textSize = 13f
            textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
            setTextColor(android.graphics.Color.BLACK)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        horizontalLayout.addView(tvEpiLink)

        // Add the horizontal layout with two TextViews to the main layout
        layout.addView(horizontalLayout)

        // Add a separator View (divider)
        val divider = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1 // Divider thickness
            ).apply {
                topMargin = 8
                bottomMargin = 8
            }
            setBackgroundColor(android.graphics.Color.parseColor("#CCCCCC"))
        }
        layout.addView(divider)

        return layout
    }


    companion object {
        fun newInstance(group: OutputGroup): GroupFragment {
            val fragment = GroupFragment()
            val bundle = Bundle()
            bundle.putString("group", Gson().toJson(group))
            fragment.arguments = bundle
            return fragment
        }
    }
}