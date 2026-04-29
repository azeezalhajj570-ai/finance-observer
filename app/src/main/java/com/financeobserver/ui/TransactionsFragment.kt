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
import kotlinx.coroutines.launch
import java.text.NumberFormat
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
        transactionAdapter = TransactionAdapter(emptyList())
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
                transactionAdapter = TransactionAdapter(transactions)
                recyclerView.adapter = transactionAdapter
            }
        }
    }
}
