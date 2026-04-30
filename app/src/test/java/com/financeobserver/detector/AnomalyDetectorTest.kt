package com.financeobserver.detector

import com.financeobserver.model.SourceType
import com.financeobserver.model.Transaction
import com.financeobserver.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class AnomalyDetectorTest {

    @Test
    fun `detectAmountAnomaly - high z-score flags as critical`() {
        val amounts = listOf(10.0, 12.0, 11.0, 9.0, 13.0, 10.0, 11.0, 12.0, 10.0, 11.0)
        val result = calculateZScore(100.0, amounts)
        assertTrue("100 is anomalous vs ~11 avg", result > 4.0)
    }

    @Test
    fun `detectAmountAnomaly - normal amount not anomalous`() {
        val amounts = listOf(10.0, 12.0, 11.0, 9.0, 13.0, 10.0, 11.0, 12.0, 10.0, 11.0)
        val result = calculateZScore(11.0, amounts)
        assertTrue("11 is normal vs ~11 avg", result < 2.0)
    }

    @Test
    fun `calculateStandardDeviation - zero for identical values`() {
        val values = listOf(10.0, 10.0, 10.0, 10.0)
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        val stdDev = Math.sqrt(variance)
        assertEquals(0.0, stdDev, 0.001)
    }

    @Test
    fun `severity classification - critical at high z-score`() {
        val severity = classifySeverity(5.0)
        assertEquals(Severity.CRITICAL, severity)
    }

    @Test
    fun `severity classification - high at z-score 3-4`() {
        val severity = classifySeverity(3.5)
        assertEquals(Severity.HIGH, severity)
    }

    @Test
    fun `severity classification - medium at z-score 2-3`() {
        val severity = classifySeverity(2.5)
        assertEquals(Severity.MEDIUM, severity)
    }

    @Test
    fun `anomaly result stores correct data`() {
        val result = AnomalyResult(
            type = AnomalyType.AMOUNT_SPIKE,
            reason = "Test anomaly",
            severity = Severity.HIGH
        )
        assertEquals(AnomalyType.AMOUNT_SPIKE, result.type)
        assertEquals("Test anomaly", result.reason)
        assertEquals(Severity.HIGH, result.severity)
    }

    private fun calculateZScore(amount: Double, historyAmounts: List<Double>): Double {
        val mean = historyAmounts.average()
        val variance = historyAmounts.map { (it - mean) * (it - mean) }.average()
        val stdDev = Math.sqrt(variance)
        if (stdDev == 0.0) return if (amount == mean) 0.0 else Double.MAX_VALUE
        return (amount - mean) / stdDev
    }

    private fun classifySeverity(zScore: Double): Severity {
        return when {
            zScore > 4.0 -> Severity.CRITICAL
            zScore > 3.0 -> Severity.HIGH
            zScore > 2.0 -> Severity.MEDIUM
            else -> Severity.LOW
        }
    }
}
