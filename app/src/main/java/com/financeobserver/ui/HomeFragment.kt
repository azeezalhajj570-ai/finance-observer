package com.financeobserver.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financeobserver.FinanceObserverApp
import com.financeobserver.R
import com.financeobserver.util.LocaleHelper
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var app: FinanceObserverApp
    private lateinit var transactionAdapter: TransactionAdapter

    private lateinit var totalSpendingText: TextView
    private lateinit var spendingTrend: TextView
    private lateinit var subscriptionCountText: TextView
    private lateinit var subscriptionTotal: TextView
    private lateinit var anomalyCountText: TextView
    private lateinit var observationsContainer: LinearLayout
    private lateinit var transactionCountText: TextView
    private lateinit var emptyView: View
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        app = requireActivity().application as FinanceObserverApp

        totalSpendingText = view.findViewById(R.id.totalSpendingText)
        spendingTrend = view.findViewById(R.id.spendingTrend)
        subscriptionCountText = view.findViewById(R.id.subscriptionCountText)
        subscriptionTotal = view.findViewById(R.id.subscriptionTotal)
        anomalyCountText = view.findViewById(R.id.anomalyCountText)
        observationsContainer = view.findViewById(R.id.observationsContainer)
        transactionCountText = view.findViewById(R.id.transactionCountText)
        emptyView = view.findViewById(R.id.emptyView)
        recyclerView = view.findViewById(R.id.recentRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = TransactionAdapter(emptyList())
        recyclerView.adapter = transactionAdapter

        loadData()
    }

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val transactions = app.transactionRepository.getRecentTransactions(20)
            val totalSpending = app.transactionRepository.getTotalSpending(
                Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000),
                Date()
            )
            val subscriptions = app.subscriptionDetector.getActiveSubscriptions()
            val subTotal = subscriptions.sumOf { it.estimatedAmount }
            val flaggedCount = app.anomalyDetector.getFlaggedTransactions().size

            val currencyFormat = if (LocaleHelper.isArabic(requireContext())) {
                NumberFormat.getNumberInstance(Locale("ar"))
            } else {
                NumberFormat.getCurrencyInstance(Locale.US)
            }

            totalSpendingText.text = formatAmount(totalSpending ?: 0.0)
            subscriptionCountText.text = "${subscriptions.size} active"
            subscriptionTotal.text = "${formatAmount(subTotal)}/mo"
            anomalyCountText.text = "$flaggedCount"
            transactionCountText.text = "${transactions.size} transactions"
            spendingTrend.text = "vs last month"

            observationsContainer.removeAllViews()
            buildObservations(transactions, subscriptions, flaggedCount)

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
        transactions: List<com.financeobserver.model.Transaction>,
        subscriptions: List<com.financeobserver.model.Subscription>,
        anomalyCount: Int
    ) {
        val inflater = LayoutInflater.from(requireContext())
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

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
            obsAmount.text = formatAmount(sub.estimatedAmount)
            obsMeta.text = "${sub.cycle.name} · Next: ${dateFormat.format(sub.nextExpectedDate ?: sub.firstSeen)}"
            obsMeta.visibility = View.VISIBLE

            observationsContainer.addView(card)
        }

        if (anomalyCount > 0) {
            val card = inflater.inflate(R.layout.item_observation, observationsContainer, false) as MaterialCardView
            val obsTime = card.findViewById<TextView>(R.id.obsTime)
            val obsTitle = card.findViewById<TextView>(R.id.obsTitle)
            val obsName = card.findViewById<TextView>(R.id.obsName)
            val obsAmount = card.findViewById<TextView>(R.id.obsAmount)

            obsTime.text = "Alert"
            obsTitle.text = "$anomalyCount spending anomal${if (anomalyCount == 1) "y" else "ies"} detected"
            obsTitle.setTextColor(requireContext().getColor(R.color.accent_critical))
            obsName.text = "Review your recent transactions"
            obsName.textSize = 13f
            obsAmount.visibility = View.GONE

            observationsContainer.addView(card)
        }

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
            obsAmount.text = formatAmount(txn.amount)
            obsMeta.text = when (txn.source) {
                com.financeobserver.model.SourceType.NOTIFICATION -> "Notification"
                com.financeobserver.model.SourceType.SMS -> "SMS"
                com.financeobserver.model.SourceType.MANUAL -> "Manual"
            }
            obsMeta.visibility = View.VISIBLE

            observationsContainer.addView(card)
        }
    }

    private fun formatAmount(amount: Double): String {
        val currency = if (LocaleHelper.isArabic(requireContext())) {
            NumberFormat.getNumberInstance(Locale("ar"))
        } else {
            NumberFormat.getCurrencyInstance(Locale.US)
        }
        return when (amount) {
            0.0 -> if (LocaleHelper.isArabic(requireContext())) "0" else "$0.00"
            else -> currency.format(amount)
        }
    }
}
