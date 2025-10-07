package com.icl.surveillance.ui.profile

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import cn.pedant.SweetAlert.SweetAlertDialog
import com.icl.surveillance.R
import com.icl.surveillance.auth.LoginActivity
import com.icl.surveillance.databinding.FragmentProfileBinding
import com.icl.surveillance.databinding.ItemLabelValueBinding
import com.icl.surveillance.models.UserProfilePrefs
import com.icl.surveillance.utils.FormatterClass
import java.io.File

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }

    fun setLabelValue(bindingSection: ItemLabelValueBinding, labelText: String, valueText: String) {
        bindingSection.tvLabel.text = labelText
        bindingSection.tvValue.text = valueText
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (child in children!!) {
                val success = deleteDir(File(dir, child))
                if (!success) return false
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            mapUserData()

            btnClearCache.setOnClickListener {
                showConfirmationDialog(
                    title = "Confirmation?",
                    message = "Are you sure you want to clear App Cache?",
                    onConfirm = { clearAppCache() }
                )
            }

            btnClearData.setOnClickListener {
                showConfirmationDialog(
                    title = "Confirmation?",
                    message = "Are you sure you want to clear App Data?",
                    onConfirm = { clearAppData() }
                )
            }

            btnLogout.setOnClickListener {
                showConfirmationDialog(
                    title = "Logout Confirmation?",
                    message = "Are you sure you want to Logout?",
                    onConfirm = { logoutUser() }
                )
            }

        }
    }

    fun getUserPrefs(context: Context): UserProfilePrefs {
        val f = FormatterClass()
        return UserProfilePrefs(
            f.getSharedPref("firstName", context) ?: "",
            f.getSharedPref("lastName", context) ?: "",
            f.getSharedPref("fullNames", context) ?: "",
            f.getSharedPref("email", context) ?: "",
            f.getSharedPref("phone", context) ?: "-",
            f.getSharedPref("idNumber", context) ?: "",
            f.getSharedPref("role", context) ?: "",
            f.getSharedPref("county", context) ?: "",
            f.getSharedPref("countyName", context) ?: "",
            f.getSharedPref("subCounty", context) ?: "",
            f.getSharedPref("subCountyName", context) ?: "",
            f.getSharedPref("ward", context) ?: "",
            f.getSharedPref("wardName", context) ?: "",
            f.getSharedPref("facility", context) ?: "",
            f.getSharedPref("facilityName", context) ?: ""
        )
    }


    private fun mapUserData() {
        try {
            binding.apply {
                val formatter = FormatterClass()
                val firstName = formatter.getSharedPref("firstName", requireContext())
                val lastName = formatter.getSharedPref("lastName", requireContext())

                val phone = formatter.getSharedPref("phone", requireContext())
                val email = formatter.getSharedPref("email", requireContext())
                val role = formatter.getSharedPref("role", requireContext())

                tvUserName.text = "$firstName $lastName"
                tvEmail.text = " $email"
                tvPhone.text = " $phone"


                // Set reusable items
                val user = getUserPrefs(requireContext())


                setLabelValue(binding.idItem, "ID Number:", user.idNumber)
                setLabelValue(binding.roleItem, "Role:", user.role)
                setLabelValue(
                    binding.countyItem,
                    "County:", user.countyName
                )
                setLabelValue(
                    binding.subCountyItem,
                    "Sub-county:", user.subCountyName
                )
                setLabelValue(
                    binding.wardItem,
                    "Ward:", user.wardName
                )
                setLabelValue(
                    binding.facilityItem,
                    "Facility:", user.facilityName
                )

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showConfirmationDialog(
        title: String,
        message: String,
        confirmText: String = "Yes, Proceed!",
        onConfirm: () -> Unit
    ) {
        SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE).apply {
            setTitleText(title)
            setContentText(message)
            setConfirmText(confirmText)
            setConfirmClickListener { sDialog ->
                onConfirm()
                sDialog.dismissWithAnimation()
            }
            show()
        }
    }

    private fun clearAppCache() {
        val cacheDir = requireActivity().cacheDir
        if (deleteDir(cacheDir)) {
            Toast.makeText(requireContext(), "Cache cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAppData() {
        val activityManager =
            requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.clearApplicationUserData()
    }

    private fun logoutUser() {
        FormatterClass().deleteSharedPref("isLoggedIn", requireContext())
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}