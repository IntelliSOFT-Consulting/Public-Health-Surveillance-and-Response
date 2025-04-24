package com.icl.surveillance.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.icl.surveillance.MainActivity
import com.icl.surveillance.R
import com.icl.surveillance.databinding.ActivityLauncherBinding
import com.icl.surveillance.databinding.ActivityLoginBinding
import com.icl.surveillance.network.RetrofitCallsAuthentication
import com.icl.surveillance.utils.FormatterClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        lifecycleScope.launch {
            delay(3000) // 3 seconds
            val loggedIn = FormatterClass().getSharedPref("isLoggedIn", this@LauncherActivity)
            if (loggedIn != null) {
                val intent = Intent(this@LauncherActivity, MainActivity::class.java)
                startActivity(intent)
                this@LauncherActivity.finish()
            } else {
                binding.getStartedButton.visibility = View.VISIBLE
            }
        }
        binding.apply {
            getStartedButton.apply {
                setOnClickListener {
                    val intent = Intent(this@LauncherActivity, LoginActivity::class.java)
                    startActivity(intent)
                    this@LauncherActivity.finish()
                }
            }
        }
    }
}