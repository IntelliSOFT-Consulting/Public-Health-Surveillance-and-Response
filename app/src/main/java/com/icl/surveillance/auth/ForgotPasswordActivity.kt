package com.icl.surveillance.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.icl.surveillance.R
import com.icl.surveillance.databinding.ActivityForgotPasswordBinding
import com.icl.surveillance.utils.FormatterClass

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
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
                    emailLayout.error = "Please enter Email Address"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }

                // check is valid email
                if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                    binding.emailLayout.error = "Enter a valid email"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }

                binding.emailLayout.error = null
//                    sendResetCode(email)

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