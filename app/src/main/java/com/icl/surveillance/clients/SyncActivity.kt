package com.icl.surveillance.clients

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.icl.surveillance.R
import com.icl.surveillance.utils.launchAndRepeatStarted
import com.icl.surveillance.viewmodels.SyncFragmentViewModel

class SyncActivity : AppCompatActivity() {
    private val syncFragmentViewModel: SyncFragmentViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sync)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<Button>(R.id.sync_now_button).setOnClickListener {
            syncFragmentViewModel.triggerOneTimeSync()
        }
        findViewById<Button>(R.id.cancel_sync_button).setOnClickListener {
            syncFragmentViewModel.cancelOneTimeSyncWork()
        }
        observeLastSyncTime()
        launchAndRepeatStarted(
            { syncFragmentViewModel.pollState.collect(::currentSyncJobStatus) },
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun currentSyncJobStatus(currentSyncJobStatus: CurrentSyncJobStatus) {
        println("currentSyncJobStatus: $currentSyncJobStatus")
        // Update status text
        val statusTextView = findViewById<TextView>(R.id.current_sync_status)
        statusTextView.text =
            getString(R.string.current_status, currentSyncJobStatus::class.java.simpleName)

        // Get views once to avoid repeated lookups
        val syncIndicator = findViewById<ProgressBar>(R.id.sync_indicator)
        val syncNowButton = findViewById<Button>(R.id.sync_now_button)
        val cancelSyncButton = findViewById<Button>(R.id.cancel_sync_button)

        // Update view states based on sync status
        when (currentSyncJobStatus) {
            is CurrentSyncJobStatus.Running -> {
                syncIndicator.visibility = View.VISIBLE
                syncNowButton.visibility = View.GONE
                cancelSyncButton.visibility = View.VISIBLE
            }

            is CurrentSyncJobStatus.Succeeded -> {
                syncIndicator.visibility = View.GONE
                syncFragmentViewModel.updateLastSyncTimestamp(currentSyncJobStatus.timestamp)
                syncNowButton.visibility = View.VISIBLE
                cancelSyncButton.visibility = View.GONE
            }

            is CurrentSyncJobStatus.Failed,
            is CurrentSyncJobStatus.Cancelled,
                -> {
                syncIndicator.visibility = View.GONE
                syncNowButton.visibility = View.VISIBLE
                cancelSyncButton.visibility = View.GONE
            }

            is CurrentSyncJobStatus.Enqueued,
            is CurrentSyncJobStatus.Blocked,
                -> {
                syncIndicator.visibility = View.GONE
                syncNowButton.visibility = View.GONE
                cancelSyncButton.visibility = View.VISIBLE
            }
        }
    }

    private fun observeLastSyncTime() {
        syncFragmentViewModel.lastSyncTimestampLiveData.observe(this) {
            findViewById<TextView>(R.id.last_sync_time).text =
                getString(R.string.last_sync_timestamp, it)
        }
    }
}