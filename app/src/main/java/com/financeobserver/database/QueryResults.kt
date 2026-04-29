package com.financeobserver.database

import androidx.room.ColumnInfo

data class SpendingByCategory(
    val category: String,
    val total: Double
)

data class FrequentMerchant(
    val merchant: String,
    val count: Int,
    val avgAmount: Double
)
