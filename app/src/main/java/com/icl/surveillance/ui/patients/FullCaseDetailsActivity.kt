package com.icl.surveillance.ui.patients

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.icl.surveillance.R
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.databinding.ActivityFullCaseDetailsBinding
import com.icl.surveillance.utils.FormatterClass

class FullCaseDetailsActivity : AppCompatActivity() {
  private lateinit var binding: ActivityFullCaseDetailsBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    binding = ActivityFullCaseDetailsBinding.inflate(layoutInflater)
    setContentView(binding.root)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_refresh -> {
        FormatterClass().saveSharedPref("questionnaire", "measles-lab.json", this)
        val intent = Intent(this, AddCaseActivity::class.java)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "measles-lab.json")
        startActivity(intent)
        true
      }
      R.id.action_settings -> {
        FormatterClass().saveSharedPref("questionnaire", "measles-lab-results.json", this)
        val intent = Intent(this, AddCaseActivity::class.java)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "measles-lab-results.json")
        startActivity(intent)
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}
