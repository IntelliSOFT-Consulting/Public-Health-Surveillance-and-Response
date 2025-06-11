package com.icl.surveillance

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.clients.SyncActivity
import com.icl.surveillance.databinding.ActivityMainBinding
import com.icl.surveillance.fhir.MainActivityViewModel
import com.icl.surveillance.ui.patients.AddCaseActivity
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.utils.launchAndRepeatStarted
import com.icl.surveillance.viewmodels.SyncFragmentViewModel


import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : AppCompatActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val UPDATE_REQUEST_CODE = 123
    private lateinit var binding: ActivityMainBinding
    private val viewModel: SyncFragmentViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForAppUpdate()

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

    private fun checkForAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                when {
                    // Try flexible first
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.FLEXIBLE,
                                this,
                                UPDATE_REQUEST_CODE
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            Log.e("AppUpdate", "Flexible update error: ${e.message}")
                        }
                    }

                    // Fallback to immediate
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                UPDATE_REQUEST_CODE
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            Log.e("AppUpdate", "Immediate update error: ${e.message}")
                        }
                    }

                    else -> {
                        Log.d("AppUpdate", "Update available but not allowed")
                    }
                }
            } else {
                Log.d("AppUpdate", "No update available")
            }
        }.addOnFailureListener {
            Log.e("AppUpdate", "Failed to check for update: ${it.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Log.e("AppUpdate", "Update flow failed! Result code: $resultCode")
                // Handle retry logic if necessary
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Resume update if it was started before (for FLEXIBLE only)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }

        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
                    // Prompt the user to restart the app
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "An update has just been downloaded.",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Restart") {
                        appUpdateManager.completeUpdate()
                    }.show()
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
                viewModel.triggerOneTimeSync()
                try {
                    launchAndRepeatStarted(
                        { viewModel.pollState.collect(::currentSyncJobStatus) },
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                startActivity(Intent(this@MainActivity, SyncActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun currentSyncJobStatus(currentSyncJobStatus: CurrentSyncJobStatus) {

        // Update view states based on sync status
        when (currentSyncJobStatus) {
            is CurrentSyncJobStatus.Running -> {
                println("Sync Running ")
            }

            is CurrentSyncJobStatus.Succeeded -> {
                println("Sync Succeeded ")
            }

            is CurrentSyncJobStatus.Failed,
            is CurrentSyncJobStatus.Cancelled,
                -> {
                println("Sync Failed vs Cancelled")
            }

            is CurrentSyncJobStatus.Enqueued,
            is CurrentSyncJobStatus.Blocked,
                -> {
                println("Sync Enqueued vs Blocked")
            }
        }
    }
}
