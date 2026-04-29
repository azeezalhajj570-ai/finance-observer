package com.financeobserver.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financeobserver.FinanceObserverApp
import com.financeobserver.R
import com.financeobserver.model.Subscription
import com.financeobserver.util.LocaleHelper
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class SubscriptionsFragment : Fragment() {

    private lateinit var app: FinanceObserverApp
    private lateinit var subscriptionAdapter: SubscriptionAdapter
    private lateinit var subscriptionTotalText: TextView
    private lateinit var emptyView: View
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscriptions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        app = requireActivity().application as FinanceObserverApp

        subscriptionTotalText = view.findViewById(R.id.subscriptionTotalText)
        emptyView = view.findViewById(R.id.emptyView)
        recyclerView = view.findViewById(R.id.subscriptionRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        subscriptionAdapter = SubscriptionAdapter(emptyList())
        recyclerView.adapter = subscriptionAdapter

        loadSubscriptions()
    }

    fun refresh() {
        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        lifecycleScope.launch {
            val subscriptions = app.subscriptionDetector.getActiveSubscriptions()
            val total = subscriptions.sumOf { it.estimatedAmount }
            subscriptionTotalText.text = "${formatAmount(total)}/mo"

            if (subscriptions.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                subscriptionAdapter = SubscriptionAdapter(subscriptions)
                recyclerView.adapter = subscriptionAdapter
            }
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (LocaleHelper.isArabic(requireContext())) {
            NumberFormat.getNumberInstance(Locale("ar")).format(amount)
        } else {
            NumberFormat.getCurrencyInstance(Locale.US).format(amount)
        }
    }

    inner class SubscriptionAdapter(
        private val subscriptions: List<Subscription>
    ) : RecyclerView.Adapter<SubscriptionAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val card = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_observation, parent, false) as MaterialCardView
            return ViewHolder(card)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val sub = subscriptions[position]
            holder.time.text = sub.cycle.name
            holder.title.text = sub.merchant
            holder.name.text = "${sub.occurrenceCount} occurrences · Confidence: ${(sub.confidence * 100).toInt()}%"
            holder.amount.text = formatAmount(sub.estimatedAmount)
            holder.meta.text = "First: ${dateFormat.format(sub.firstSeen)} · Next: ${dateFormat.format(sub.nextExpectedDate ?: sub.firstSeen)}"
            holder.meta.visibility = View.VISIBLE
        }

        override fun getItemCount() = subscriptions.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val time: TextView = view.findViewById(R.id.obsTime)
            val title: TextView = view.findViewById(R.id.obsTitle)
            val name: TextView = view.findViewById(R.id.obsName)
            val amount: TextView = view.findViewById(R.id.obsAmount)
            val meta: TextView = view.findViewById(R.id.obsMeta)
        }
    }
}
