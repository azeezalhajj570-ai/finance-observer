package com.financeobserver.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.financeobserver.R
import com.financeobserver.model.SourceType
import com.financeobserver.model.Transaction
import com.financeobserver.util.CurrencyHelper
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onItemClick: ((Transaction) -> Unit)? = null
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())

    private val categoryColors = mapOf(
        "food" to R.color.cat_food,
        "transport" to R.color.cat_transport,
        "shopping" to R.color.cat_shopping,
        "subscription" to R.color.cat_subscription,
        "streaming" to R.color.cat_streaming,
        "default" to R.color.cat_default
    )

    private val categoryTextColors = mapOf(
        "food" to R.color.cat_food_text,
        "transport" to R.color.cat_transport_text,
        "shopping" to R.color.cat_shopping_text,
        "subscription" to R.color.cat_subscription_text,
        "streaming" to R.color.cat_streaming_text,
        "default" to R.color.cat_default_text
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
        val key = getCategoryColorKey(txn)
        return categoryColors[key] ?: R.color.cat_default
    }

    private fun getCategoryColorKey(txn: Transaction): String {
        return when {
            txn.category?.contains("food", true) == true || txn.category?.contains("restaurant", true) == true -> "food"
            txn.category?.contains("transport", true) == true -> "transport"
            txn.category?.contains("shopping", true) == true -> "shopping"
            txn.category?.contains("subscription", true) == true || txn.merchant.contains("Netflix", true) -> "subscription"
            else -> "default"
        }
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
        val currency = CurrencyHelper.getSelectedCurrency(ctx)

        holder.txnName.text = txn.merchant
        holder.txnAmount.text = CurrencyHelper.formatAmount(txn.amount, currency)
        holder.txnMeta.text = "${dateFormat.format(txn.timestamp)} · ${
            when (txn.source) {
                SourceType.NOTIFICATION -> "Notification"
                SourceType.SMS -> "SMS"
                SourceType.MANUAL -> "Manual"
            }
        }"

        holder.txnCategoryIcon.text = getCategoryLabel(txn)
        holder.txnCategoryIcon.setTextColor(ctx.getColor(categoryTextColors[getCategoryColorKey(txn)] ?: R.color.cat_default_text))
        holder.txnCategoryBg.background.setTint(ctx.getColor(getCategoryColor(txn)))

        if (txn.isFlagged) {
            holder.txnAmount.setTextColor(ctx.getColor(R.color.semantic_red))
        } else {
            holder.txnAmount.setTextColor(ctx.getColor(R.color.text_primary))
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(txn)
        }
    }

    override fun getItemCount() = transactions.size
}
