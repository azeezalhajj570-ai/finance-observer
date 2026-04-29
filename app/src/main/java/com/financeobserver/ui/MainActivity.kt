package com.financeobserver.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financeobserver.FinanceObserverApp
import com.financeobserver.R
import com.financeobserver.model.Transaction
import com.financeobserver.service.PaymentNotificationListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Activity - Dashboard showing financial overview and recent transactions.
 * 
 * Screens:
 * 1. Permission setup (if permissions not granted)
 * 2. Dashboard (financial health + recent activity)
 * 3. Transaction list
 */
class MainActivity : AppCompatActivity() {

    private lateinit var app: FinanceObserverApp
    private lateinit var transactionAdapter: TransactionAdapter

    // UI elements
    private lateinit var permissionCard: MaterialCardView
    private lateinit var setupButton: MaterialButton
    private lateinit var dashboardView: View
    private lateinit var emptyView: TextView
    private lateinit var totalSpendingText: TextView
    private lateinit var transactionCountText: TextView
    private lateinit var subscriptionCountText: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        app = application as FinanceObserverApp

        // Initialize views
        permissionCard = findViewById(R.id.permissionCard)
        setupButton = findViewById(R.id.setupButton)
        dashboardView = findViewById(R.id.dashboardView)
        emptyView = findViewById(R.id.emptyView)
        totalSpendingText = findViewById(R.id.totalSpendingText)
        transactionCountText = findViewById(R.id.transactionCountText)
        subscriptionCountText = findViewById(R.id.subscriptionCountText)
        recyclerView = findViewById(R.id.transactionRecyclerView)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(emptyList())
        recyclerView.adapter = transactionAdapter

        // Setup button click
        setupButton.setOnClickListener {
            openNotificationAccessSettings()
        }

        // Check permissions and load data
        checkPermissionsAndLoad()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        val hasNotificationAccess = checkNotificationAccess()
        val hasSmsPermission = checkSelfPermission(android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasNotificationAccess || !hasSmsPermission) {
            permissionCard.visibility = View.VISIBLE
            dashboardView.visibility = View.GONE
            setupButton.text = when {
                !hasNotificationAccess && !hasSmsPermission -> "Grant Permissions"
                !hasNotificationAccess -> "Enable Notification Access"
                else -> "Grant SMS Permission"
            }
        } else {
            permissionCard.visibility = View.GONE
            dashboardView.visibility = View.VISIBLE
            loadData()
        }
    }

    private fun checkNotificationAccess(): Boolean {
        val enabledPackages = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_NOTIFICATION_LISTENERS
        ) ?: ""
        return enabledPackages.contains(packageName)
    }

    private fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun loadData() {
        lifecycleScope.launch {
            val transactions = app.transactionRepository.getRecentTransactions(50)
            val totalSpending = app.transactionRepository.getTotalSpending(
                Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000),
                Date()
            )
            val subscriptionCount = app.subscriptionDetector.getActiveSubscriptions().size

            // Update UI
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            totalSpendingText.text = currencyFormat.format(totalSpending ?: 0.0)
            transactionCountText.text = "${transactions.size} transactions"
            subscriptionCountText.text = "${subscriptionCount} subscriptions"

            if (transactions.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                emptyView.text = """
                    I'm watching for your financial activity.
                    
                    When you make a purchase, I'll capture it from:
                    • Bank app notifications
                    • Payment app alerts (Venmo, PayPal, etc.)
                    • SMS receipts
                    
                    Try making a test purchase to see me in action!
                """.trimIndent()
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                transactionAdapter = TransactionAdapter(transactions)
                recyclerView.adapter = transactionAdapter
            }
        }
    }
}

/**
 * Adapter for displaying transactions in a RecyclerView.
 */
class TransactionAdapter(
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val merchantText: TextView = view.findViewById(R.id.merchantText)
        val amountText: TextView = view.findViewById(R.id.amountText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val sourceText: TextView = view.findViewById(R.id.sourceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.merchantText.text = transaction.merchant
        holder.amountText.text = currencyFormat.format(transaction.amount)
        holder.dateText.text = dateFormat.format(transaction.timestamp)
        holder.sourceText.text = when (transaction.source) {
            com.financeobserver.model.SourceType.NOTIFICATION -> "Notification"
            com.financeobserver.model.SourceType.SMS -> "SMS"
            com.financeobserver.model.SourceType.MANUAL -> "Manual"
        }

        // Color code by amount
        if (transaction.isFlagged) {
            holder.amountText.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
        }
    }

    override fun getItemCount() = transactions.size
}
