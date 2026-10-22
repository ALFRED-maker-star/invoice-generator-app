package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun formatCurrency(amount: Double, currencySymbol: String = "$"): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$currencySymbol${formatter.format(amount)}"
    }

    fun formatDate(timestamp: Long, pattern: String = "MMM dd, yyyy"): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return formatDate(timestamp, "MM/dd/yy")
    }

    fun generateInvoiceNumber(existingCount: Int): String {
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val index = (existingCount + 1).toString().padStart(3, '0')
        return "INV-$year-$index"
    }
}
