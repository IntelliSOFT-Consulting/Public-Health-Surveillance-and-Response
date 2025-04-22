package com.icl.surveillance.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.icl.surveillance.databinding.FragmentNotificationsBinding
import com.icl.surveillance.utils.FormatterClass

class NotificationsFragment : Fragment() {

  private var _binding: FragmentNotificationsBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding
    get() = _binding!!

  private lateinit var sharedPrefUtil: FormatterClass

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)

    _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
    val root: View = binding.root

    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    sharedPrefUtil = FormatterClass()

    // Retrieve user data from SharedPreferences
    val firstName = sharedPrefUtil.getSharedPref("firstName", requireContext())
    val lastName = sharedPrefUtil.getSharedPref("lastName", requireContext())
    val role = sharedPrefUtil.getSharedPref("role", requireContext())
    val id = sharedPrefUtil.getSharedPref("id", requireContext())
    val idNumber = sharedPrefUtil.getSharedPref("idNumber", requireContext())
    val fullName = sharedPrefUtil.getSharedPref("fullNames", requireContext())
    val phone = sharedPrefUtil.getSharedPref("phone", requireContext())
    val email = sharedPrefUtil.getSharedPref("email", requireContext())

    binding.apply {
      tvFullName.text = fullName
      tvPhone.text = phone
      tvEmail.text = email

      //      emailTextView.text = "Email: $email"
      //      firstNameTextView.text = "First Name: $firstName"
      //      lastNameTextView.text = "Last Name: $lastName"
      //      roleTextView.text = "Role: $role"
      //      idNumberTextView.text = "ID Number: $idNumber"
      //      phoneTextView.text = "Phone: $phone"
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
