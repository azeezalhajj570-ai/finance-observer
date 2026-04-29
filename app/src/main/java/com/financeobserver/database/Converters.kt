package com.financeobserver.database

import androidx.room.TypeConverter
import com.financeobserver.model.BillingCycle
import com.financeobserver.model.SourceType
import com.financeobserver.model.TransactionType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }

    @TypeConverter
    fun fromSourceType(type: SourceType?): String? = type?.name

    @TypeConverter
    fun toSourceType(name: String?): SourceType? = name?.let { SourceType.valueOf(it) }

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? = type?.name

    @TypeConverter
    fun toTransactionType(name: String?): TransactionType? = name?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun fromBillingCycle(cycle: BillingCycle?): String? = cycle?.name

    @TypeConverter
    fun toBillingCycle(name: String?): BillingCycle? = name?.let { BillingCycle.valueOf(it) }
}
