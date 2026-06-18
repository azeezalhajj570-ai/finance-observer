package com.financeobserver.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financeobserver.FinanceObserverApp
import com.financeobserver.R
import com.financeobserver.model.Account
import com.financeobserver.util.CurrencyHelper
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class AccountsFragment : Fragment() {

    private var accountAdapter: AccountAdapter? = null
    private var recyclerView: RecyclerView? = null
    private var emptyView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.fragment_accounts, container, false)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading accounts: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            recyclerView = view.findViewById(R.id.accountRecyclerView)
            emptyView = view.findViewById(R.id.emptyView)
            recyclerView?.layoutManager = LinearLayoutManager(requireContext())
            loadAccounts()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun refresh() {
        try {
            loadAccounts()
        } catch (_: Exception) {}
    }

    private fun loadAccounts() {
        try {
            lifecycleScope.launch {
                val app = requireActivity().application as FinanceObserverApp
                val accounts = app.database.accountDao().getActiveAccounts()
                if (accounts.isEmpty()) {
                    emptyView?.visibility = View.VISIBLE
                    recyclerView?.visibility = View.GONE
                } else {
                    emptyView?.visibility = View.GONE
                    recyclerView?.visibility = View.VISIBLE
                    accountAdapter = AccountAdapter(accounts)
                    recyclerView?.adapter = accountAdapter
                }
            }
        } catch (_: Exception) {}
    }

    inner class AccountAdapter(
        private val accounts: List<Account>
    ) : RecyclerView.Adapter<AccountAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val card = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_account, parent, false) as MaterialCardView
            return ViewHolder(card)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val account = accounts[position]
            val ctx = holder.itemView.context
            val currency = CurrencyHelper.getSelectedCurrency(ctx)
            holder.name.text = account.name
            holder.bank.text = account.bankName
            holder.initial.text = account.bankName.take(2).uppercase()
            holder.balance.text = CurrencyHelper.formatAmount(account.balance, currency)
        }

        override fun getItemCount() = accounts.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val initial: TextView = view.findViewById(R.id.accountInitial)
            val name: TextView = view.findViewById(R.id.accountName)
            val bank: TextView = view.findViewById(R.id.accountBank)
            val balance: TextView = view.findViewById(R.id.accountBalance)
        }
    }
}
