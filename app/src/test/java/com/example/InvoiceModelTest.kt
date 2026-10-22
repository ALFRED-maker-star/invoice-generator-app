package com.example

import com.example.data.model.Invoice
import com.example.data.model.InvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceModelTest {

    @Test
    fun testInvoiceRemainingBalanceAndStatus() {
        val invoice = Invoice(
            id = 1L,
            invoiceNumber = "INV-2026-001",
            clientId = 1L,
            clientName = "Sarah Jenkins",
            issueDate = System.currentTimeMillis() - 100000,
            dueDate = System.currentTimeMillis() + 1000000,
            status = InvoiceStatus.PENDING.name,
            totalAmount = 1500.0,
            amountPaid = 500.0
        )

        assertEquals(1000.0, invoice.remainingBalance, 0.001)
        assertEquals(InvoiceStatus.PENDING, invoice.computedStatus)

        // Fully paid
        val paidInvoice = invoice.copy(amountPaid = 1500.0)
        assertEquals(0.0, paidInvoice.remainingBalance, 0.001)
        assertEquals(InvoiceStatus.PAID, paidInvoice.computedStatus)

        // Overdue
        val overdueInvoice = invoice.copy(
            dueDate = System.currentTimeMillis() - 50000,
            amountPaid = 200.0
        )
        assertEquals(InvoiceStatus.OVERDUE, overdueInvoice.computedStatus)
    }
}
