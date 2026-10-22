package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.InvoiceStatus
import com.example.data.model.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paymentDate DESC")
    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Transaction
    suspend fun recordPaymentAndUpdateInvoice(
        payment: Payment,
        invoiceDao: InvoiceDao
    ): Long {
        val paymentId = insertPayment(payment)
        val invoice = invoiceDao.getInvoiceById(payment.invoiceId)
        if (invoice != null) {
            val newAmountPaid = invoice.amountPaid + payment.amount
            val newStatus = if (newAmountPaid >= invoice.totalAmount) {
                InvoiceStatus.PAID.name
            } else {
                InvoiceStatus.PENDING.name
            }
            invoiceDao.updateInvoice(
                invoice.copy(
                    amountPaid = newAmountPaid,
                    status = newStatus
                )
            )
        }
        return paymentId
    }
}
