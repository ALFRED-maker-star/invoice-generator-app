package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InvoiceStatus(val label: String) {
    DRAFT("Draft"),
    PENDING("Pending"),
    PAID("Paid"),
    OVERDUE("Overdue");

    companion object {
        fun fromString(value: String): InvoiceStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceNumber: String,
    val clientId: Long,
    val clientName: String,
    val clientCompany: String = "",
    val issueDate: Long,
    val dueDate: Long,
    val status: String = InvoiceStatus.PENDING.name,
    val subtotal: Double = 0.0,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountRate: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingBalance: Double
        get() = (totalAmount - amountPaid).coerceAtLeast(0.0)

    val computedStatus: InvoiceStatus
        get() {
            return when {
                status == InvoiceStatus.DRAFT.name -> InvoiceStatus.DRAFT
                amountPaid >= totalAmount && totalAmount > 0.0 -> InvoiceStatus.PAID
                dueDate < System.currentTimeMillis() && remainingBalance > 0.0 -> InvoiceStatus.OVERDUE
                else -> InvoiceStatus.PENDING
            }
        }
}
