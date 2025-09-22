package com.icl.surveillance.ui.notifications

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cn.pedant.SweetAlert.SweetAlertDialog
import com.icl.surveillance.MainActivity
import com.icl.surveillance.auth.LoginActivity
import com.icl.surveillance.databinding.FragmentNotificationsBinding
import com.icl.surveillance.utils.FormatterClass
import java.io.File

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