package com.icl.surveillance.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.icl.surveillance.R
import com.icl.surveillance.adapters.NotificationAdapter
import com.icl.surveillance.databinding.ActivityNotificationBinding
import com.icl.surveillance.models.Notification

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var binding: ActivityNotificationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar.apply { title = "Notifications" }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = binding.notificationRecyclerView//findViewById(R.id.notificationRecyclerView)
        emptyStateLayout = binding.emptyStateLayout// findViewById(R.id.emptyStateLayout)

        val notifications = loadNotifications()

        if (notifications.isEmpty()) {
            showEmptyState()
        } else {
            showNotifications(notifications)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun loadNotifications(): List<Notification> {
        // Simulate data loading
        return listOf(
        ) // ← Change to actual data source
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
    }

    private fun showNotifications(notifications: List<Notification>) {
        recyclerView.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = NotificationAdapter(notifications)
    }
}