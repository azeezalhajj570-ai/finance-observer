package com.financeobserver.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financeobserver.FinanceObserverApp
import com.financeobserver.R
import com.financeobserver.model.Transaction
import com.financeobserver.service.PaymentNotificationListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var app: FinanceObserverApp
    private lateinit var transactionAdapter: TransactionAdapter

    private lateinit var headerTitle: TextView
    private lateinit var headerStatus: TextView
    private lateinit var totalSpendingText: TextView
    private lateinit var spendingTrend: TextView
    private lateinit var subscriptionCountText: TextView
    private lateinit var subscriptionTotal: TextView
    private lateinit var anomalyCountText: TextView
    private lateinit var observationsContainer: LinearLayout
    private lateinit var transactionCountText: TextView
    private lateinit var emptyView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        app = application as FinanceObserverApp
        initViews()
        setupRecyclerView()
        setupBottomNav()
    }

    private fun initViews() {
        headerTitle = findViewById(R.id.headerTitle)
        headerStatus = findViewById(R.id.headerStatus)
        totalSpendingText = findViewById(R.id.totalSpendingText)
        spendingTrend = findViewById(R.id.spendingTrend)
        subscriptionCountText = findViewById(R.id.subscriptionCountText)
        subscriptionTotal = findViewById(R.id.subscriptionTotal)
        anomalyCountText = findViewById(R.id.anomalyCountText)
        observationsContainer = findViewById(R.id.observationsContainer)
        transactionCountText = findViewById(R.id.transactionCountText)
        emptyView = findViewById(R.id.emptyView)
        recyclerView = findViewById(R.id.transactionRecyclerView)
        bottomNav = findViewById(R.id.bottomNav)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(emptyList())
        recyclerView.adapter = transactionAdapter
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_transactions -> {
                    recyclerView.smoothScrollToPosition(0)
                    true
                }
                R.id.nav_subscriptions -> true
                R.id.nav_settings -> {
                    openNotificationAccessSettings()
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndLoad()
    }

    private fun checkNotificationAccess(): Boolean {
        val enabledPackages = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        return enabledPackages.contains(packageName)
    }

    private fun checkPermissionsAndLoad() {
        val hasNotificationAccess = checkNotificationAccess()
        val hasSmsPermission = checkSelfPermission(
            android.Manifest.permission.READ_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasNotificationAccess || !hasSmsPermission) {
            headerStatus.text = "⊙ Setup needed"
            headerStatus.setTextColor(getColor(R.color.accent_critical))
            showPermissionPrompt()
        } else {
            headerStatus.text = "⊙ Active"
            headerStatus.setTextColor(getColor(R.color.accent_positive))
            loadData()
        }
    }

    private fun showPermissionPrompt() {
        val inflater = LayoutInflater.from(this)
        observationsContainer.removeAllViews()

        val card = inflater.inflate(R.layout.item_observation, observationsContainer, false) as MaterialCardView
        val obsTime = card.findViewById<TextView>(R.id.obsTime)
        val obsTitle = card.findViewById<TextView>(R.id.obsTitle)
        val obsName = card.findViewById<TextView>(R.id.obsName)
        val obsAmount = card.findViewById<TextView>(R.id.obsAmount)

        obsTime.text = "Setup"
        obsTitle.text = "Permissions Required"
        obsName.text = "Finance Observer needs notification and SMS access to track your spending. All data stays on your device."
        obsName.textSize = 13f
        obsAmount.visibility = View.GONE

        observationsContainer.addView(card)
        emptyView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            checkPermissionsAndLoad()
        }
    }

    private fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 1001
    }

    private fun loadData() {
        lifecycleScope.launch {
            val transactions = app.transactionRepository.getRecentTransactions(50)
            val totalSpending = app.transactionRepository.getTotalSpending(
                Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000),
                Date()
            )
            val subscriptions = app.subscriptionDetector.getActiveSubscriptions()
            val subscriptionCount = subscriptions.size
            val anomalyCount = app.anomalyDetector.getFlaggedTransactions().size
            val subTotal = subscriptions.sumOf { it.estimatedAmount }

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            totalSpendingText.text = currencyFormat.format(totalSpending ?: 0.0)
            subscriptionCountText.text = "$subscriptionCount active"
            subscriptionTotal.text = "${currencyFormat.format(subTotal)}/mo"
            anomalyCountText.text = "$anomalyCount"
            transactionCountText.text = "${transactions.size} transactions"

            spendingTrend.text = "vs last month"

            observationsContainer.removeAllViews()
            buildObservations(transactions, subscriptions, anomalyCount)

            if (transactions.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                transactionAdapter = TransactionAdapter(transactions)
                recyclerView.adapter = transactionAdapter
            }
        }
    }

    private fun buildObservations(
        transactions: List<Transaction>,
        subscriptions: List<com.financeobserver.model.Subscription>,
        anomalyCount: Int
    ) {
        val inflater = LayoutInflater.from(this)

        // Show subscription observations
        for (sub in subscriptions.take(3)) {
            val card = inflater.inflate(R.layout.item_observation, observationsContainer, false) as MaterialCardView
            val obsTime = card.findViewById<TextView>(R.id.obsTime)
            val obsTitle = card.findViewById<TextView>(R.id.obsTitle)
            val obsName = card.findViewById<TextView>(R.id.obsName)
            val obsAmount = card.findViewById<TextView>(R.id.obsAmount)
            val obsMeta = card.findViewById<TextView>(R.id.obsMeta)

            obsTime.text = "Subscription"
            obsTitle.text = sub.merchant
            obsName.text = sub.merchant
            obsAmount.text = NumberFormat.getCurrencyInstance(Locale.US).format(sub.estimatedAmount)
            val nextBilling = sub.nextExpectedDate?.let { SimpleDateFormat("MMM dd", Locale.getDefault()).format(it) } ?: "Unknown"
            obsMeta.text = "${sub.cycle.name.lowercase().replaceFirstChar { it.uppercase() }} · Next: $nextBilling"
            obsMeta.visibility = View.VISIBLE

            observationsContainer.addView(card)
        }

        // Show anomaly observations
        if (anomalyCount > 0) {
            val card = inflater.inflate(R.layout.item_observation, observationsContainer, false) as MaterialCardView
            val obsTime = card.findViewById<TextView>(R.id.obsTime)
            val obsTitle = card.findViewById<TextView>(R.id.obsTitle)
            val obsName = card.findViewById<TextView>(R.id.obsName)
            val obsAmount = card.findViewById<TextView>(R.id.obsAmount)

            obsTime.text = "Alert"
            obsTitle.text = "$anomalyCount spending anomal${if (anomalyCount == 1) "y" else "ies"} detected"
            obsTitle.setTextColor(getColor(R.color.accent_critical))
            obsName.text = "Review your recent transactions"
            obsName.textSize = 13f
            obsAmount.visibility = View.GONE

            observationsContainer.addView(card)
        }

        // Show recent transaction observations (latest 2)
        if (transactions.isNotEmpty()) {
            for (txn in transactions.take(2)) {
                val card = inflater.inflate(R.layout.item_observation, observationsContainer, false) as MaterialCardView
                val obsTime = card.findViewById<TextView>(R.id.obsTime)
                val obsTitle = card.findViewById<TextView>(R.id.obsTitle)
                val obsName = card.findViewById<TextView>(R.id.obsName)
                val obsAmount = card.findViewById<TextView>(R.id.obsAmount)
                val obsMeta = card.findViewById<TextView>(R.id.obsMeta)

                obsTime.text = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(txn.timestamp)
                obsTitle.visibility = View.GONE
                obsName.text = txn.merchant
                obsAmount.text = NumberFormat.getCurrencyInstance(Locale.US).format(txn.amount)
                obsMeta.text = when (txn.source) {
                    com.financeobserver.model.SourceType.NOTIFICATION -> "Notification"
                    com.financeobserver.model.SourceType.SMS -> "SMS"
                    com.financeobserver.model.SourceType.MANUAL -> "Manual"
                }
                obsMeta.visibility = View.VISIBLE

                observationsContainer.addView(card)
            }
        }
    }
}

class TransactionAdapter(
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    private val categoryColors = mapOf(
        "food" to R.color.cat_food,
        "transport" to R.color.cat_transport,
        "shopping" to R.color.cat_shopping,
        "subscription" to R.color.cat_subscription,
        "streaming" to R.color.cat_streaming,
        "default" to R.color.cat_default
    )

    private fun getCategoryLabel(txn: Transaction): String = when {
        txn.category?.contains("food", true) == true || txn.category?.contains("restaurant", true) == true -> "FD"
        txn.category?.contains("transport", true) == true -> "TR"
        txn.category?.contains("shopping", true) == true -> "SH"
        txn.category?.contains("subscription", true) == true -> "SB"
        txn.merchant.contains("Netflix", true) || txn.merchant.contains("Spotify", true) || txn.merchant.contains("Hulu", true) -> "ST"
        txn.merchant.contains("Amazon", true) -> "SH"
        txn.merchant.contains("Starbucks", true) || txn.merchant.contains("Coffee", true) -> "FD"
        txn.merchant.contains("Uber", true) || txn.merchant.contains("Lyft", true) -> "TR"
        else -> "TX"
    }

    private fun getCategoryColor(txn: Transaction): Int {
        val key = when {
            txn.category?.contains("food", true) == true || txn.category?.contains("restaurant", true) == true -> "food"
            txn.category?.contains("transport", true) == true -> "transport"
            txn.category?.contains("shopping", true) == true -> "shopping"
            txn.category?.contains("subscription", true) == true || txn.merchant.contains("Netflix", true) -> "subscription"
            else -> "default"
        }
        return categoryColors[key] ?: R.color.cat_default
    }

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txnCategoryBg: View = view.findViewById(R.id.txnCategoryBg)
        val txnCategoryIcon: TextView = view.findViewById(R.id.txnCategoryIcon)
        val txnName: TextView = view.findViewById(R.id.txnName)
        val txnMeta: TextView = view.findViewById(R.id.txnMeta)
        val txnAmount: TextView = view.findViewById(R.id.txnAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val txn = transactions[position]
        val ctx = holder.itemView.context

        holder.txnName.text = txn.merchant
        holder.txnAmount.text = currencyFormat.format(txn.amount)
        holder.txnMeta.text = "${dateFormat.format(txn.timestamp)} · ${
            when (txn.source) {
                com.financeobserver.model.SourceType.NOTIFICATION -> "Notification"
                com.financeobserver.model.SourceType.SMS -> "SMS"
                com.financeobserver.model.SourceType.MANUAL -> "Manual"
            }
        }"

        holder.txnCategoryIcon.text = getCategoryLabel(txn)
        holder.txnCategoryIcon.setTextColor(ctx.getColor(R.color.text_primary))
        holder.txnCategoryBg.background.setTint(ctx.getColor(getCategoryColor(txn)))

        if (txn.isFlagged) {
            holder.txnAmount.setTextColor(ctx.getColor(android.R.color.holo_red_dark))
        } else {
            holder.txnAmount.setTextColor(ctx.getColor(R.color.text_primary))
        }
    }

    override fun getItemCount() = transactions.size
}
