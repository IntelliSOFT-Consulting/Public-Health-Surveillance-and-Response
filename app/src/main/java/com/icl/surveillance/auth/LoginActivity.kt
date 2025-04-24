package com.icl.surveillance.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.icl.surveillance.MainActivity
import com.icl.surveillance.R
import com.icl.surveillance.databinding.ActivityLoginBinding
import com.icl.surveillance.models.DbSignIn
import com.icl.surveillance.network.RetrofitCallsAuthentication
import com.icl.surveillance.utils.FormatterClass

class LoginActivity : AppCompatActivity() {

    private var retrofitCallsAuthentication = RetrofitCallsAuthentication()
    private lateinit var binding: ActivityLoginBinding

//    override fun onStart() {
//        super.onStart()
//
//        try {
//            val loggedIn = FormatterClass().getSharedPref("isLoggedIn", this@LoginActivity)
//            if (loggedIn != null) {
//                val intent = Intent(this@LoginActivity, MainActivity::class.java)
//                startActivity(intent)
//                this@LoginActivity.finish()
//            }
//        } catch (e: Exception) {
//
//            e.printStackTrace()
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.apply {
            btnLogin.setOnClickListener {
                val email = etEmail.text.toString()
                val password = etPassword.text.toString()

                if (email.isEmpty()) {
                    binding.emailLayout.error = "Please enter username"
                    return@setOnClickListener
                }
                // check password
                if (password.isEmpty()) {
                    binding.passwordLayout.error = "Please enter password"
                    return@setOnClickListener
                }

                val dbSignIn = DbSignIn(idNumber = email, password = password, "Facility")
                retrofitCallsAuthentication.loginUser(this@LoginActivity, dbSignIn)

            }
        }
    }
}