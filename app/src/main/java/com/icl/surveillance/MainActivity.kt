package com.icl.surveillance

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.clients.SyncActivity
import com.icl.surveillance.databinding.ActivityMainBinding
import com.icl.surveillance.fhir.MainActivityViewModel
import com.icl.surveillance.ui.patients.AddCaseActivity
import com.icl.surveillance.utils.FormatterClass

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration =
            AppBarConfiguration(
                setOf(
                    R.id.navigation_home,
                    R.id.navigation_dashboard,
                    R.id.navigation_notifications
                )
            )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Do something when Home is clicked
                    navController.navigate(R.id.navigation_home)
                    true
                }

                R.id.navigation_dashboard -> {
                    // Do something when Dashboard is clicked
                    navController.navigate(R.id.navigation_dashboard)
                    true
                }

                R.id.navigation_notifications -> {
                    // Do something when Notifications is clicked
                    navController.navigate(R.id.navigation_notifications)
                    true
                }

                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sync, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
//                viewModel.triggerOneTimeSync()
//                Toast.makeText(this@MainActivity, "Syncing ...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MainActivity, SyncActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
