package com.icl.surveillance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.icl.surveillance.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val navView: BottomNavigationView = binding.navView

    val navController = findNavController(R.id.nav_host_fragment_activity_main)
    // Passing each menu ID as a set of Ids because each
    // menu should be considered as top level destinations.
    val appBarConfiguration =
        AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
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
}
