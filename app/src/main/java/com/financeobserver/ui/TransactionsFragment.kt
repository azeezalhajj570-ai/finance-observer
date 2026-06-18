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
import com.financeobserver.model.Transaction
import com.financeobserver.util.CurrencyHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionsFragment : Fragment() {

    private lateinit var app: FinanceObserverApp
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var transactionCountText: TextView
    private lateinit var emptyView: View
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        app = requireActivity().application as FinanceObserverApp

        transactionCountText = view.findViewById(R.id.transactionCountText)
        emptyView = view.findViewById(R.id.emptyView)
        recyclerView = view.findViewById(R.id.transactionRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = TransactionAdapter(emptyList()) { txn -> showTransactionDetail(txn) }
        recyclerView.adapter = transactionAdapter

        loadTransactions()
    }

    fun refresh() {
        loadTransactions()
    }

    private fun loadTransactions() {
        lifecycleScope.launch {
            val transactions = app.transactionRepository.getAllTransactions()
            transactionCountText.text = "${transactions.size}"

            if (transactions.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                transactionAdapter = TransactionAdapter(transactions) { txn -> showTransactionDetail(txn) }
                recyclerView.adapter = transactionAdapter
            }
        }
    }

    private fun showTransactionDetail(txn: Transaction) {
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(R.layout.bottom_sheet_transaction)

        val currency = CurrencyHelper.getSelectedCurrency(requireContext())
        val dateFormat = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault())

        dialog.findViewById<TextView>(R.id.detailMerchant)?.text = txn.merchant
        dialog.findViewById<TextView>(R.id.detailAmount)?.text = CurrencyHelper.formatAmount(txn.amount, currency)
        dialog.findViewById<TextView>(R.id.detailDate)?.text = dateFormat.format(txn.timestamp)
        dialog.findViewById<TextView>(R.id.detailCategory)?.text = txn.category ?: "Uncategorized"
        dialog.findViewById<TextView>(R.id.detailSource)?.text = when (txn.source) {
            com.financeobserver.model.SourceType.NOTIFICATION -> "Notification · ${txn.sourceApp ?: "Unknown"}"
            com.financeobserver.model.SourceType.SMS -> "SMS"
            com.financeobserver.model.SourceType.MANUAL -> "Manual"
        }
        dialog.findViewById<TextView>(R.id.detailCurrency)?.text = txn.currency

        if (txn.isFlagged) {
            dialog.findViewById<View>(R.id.detailFlaggedLayout)?.visibility = View.VISIBLE
            dialog.findViewById<View>(R.id.detailFlaggedDivider)?.visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.detailFlagged)?.text = txn.flagReason ?: "Flagged as anomaly"
        }

        if (!txn.notes.isNullOrEmpty()) {
            dialog.findViewById<View>(R.id.detailNotesLayout)?.visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.detailNotes)?.text = txn.notes
        }

        dialog.show()
    }
}
