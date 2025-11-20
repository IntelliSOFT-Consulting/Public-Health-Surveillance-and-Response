package com.icl.surveillance.auth

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.icl.surveillance.R
import com.icl.surveillance.databinding.ActivityForgotPasswordBinding
import com.icl.surveillance.models.DbResetPasswordData
import com.icl.surveillance.network.RetrofitCallsAuthentication
import com.icl.surveillance.utils.FormatterClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private var retrofitCallsAuthentication = RetrofitCallsAuthentication()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.apply {
            title = "Forgot Password"
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.apply {
            btnSubmit.setOnClickListener {

                val emailAddress = etEmail.text.toString()
                if (emailAddress.isEmpty()) {
                    emailLayout.error = "Please enter Username"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }



                binding.emailLayout.error = null
//                    sendResetCode(email)
                val payload = DbResetPasswordData(
                    idNumber = emailAddress,
                    email = emailAddress
                )
                lifecycleScope.launch {
                    val progressDialog = ProgressDialog(this@ForgotPasswordActivity)
                    progressDialog.setTitle("Please wait..")
                    progressDialog.setMessage("Authentication in progress..")
                    progressDialog.setCanceledOnTouchOutside(false)
                    progressDialog.show()

                    try {
                        val (messageCode, messageToast) = withContext(Dispatchers.IO) {
                            FormatterClass().saveSharedPref(
                                "idNumber",
                                emailAddress,
                                this@ForgotPasswordActivity
                            )
                            retrofitCallsAuthentication
                                .getResetPassword(this@ForgotPasswordActivity, payload)
                        }

                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            messageToast,
                            Toast.LENGTH_SHORT
                        ).show()
                        if (messageCode == 200 || messageCode == 201) {
                            val intent = Intent(
                                this@ForgotPasswordActivity,
                                ResetPasswordActivity::class.java
                            )
                            startActivity(intent)
                            this@ForgotPasswordActivity.finish()
                        }
                    } finally {
                        progressDialog.dismiss()
                    }
                }

            }
            haveCodeTextView.setOnClickListener {
                startActivity(
                    Intent(
                        this@ForgotPasswordActivity,
                        ResetPasswordActivity::class.java
                    )
                )
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}