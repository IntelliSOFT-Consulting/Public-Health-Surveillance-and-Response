package com.icl.surveillance.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.icl.surveillance.MainActivity
import com.icl.surveillance.auth.LoginActivity
import com.icl.surveillance.databinding.FragmentNotificationsBinding
import com.icl.surveillance.utils.FormatterClass

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            actionButton.apply {
                setOnClickListener {
                    FormatterClass().deleteSharedPref("isLoggedIn", requireContext())
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    startActivity(intent)
                    (activity as MainActivity).finish()
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}