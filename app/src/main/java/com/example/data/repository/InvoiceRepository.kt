package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceStatus
import com.example.data.model.InvoiceWithDetails
import com.example.data.model.Payment
import com.example.data.model.UserProfile
import com.example.util.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class InvoiceRepository(private val database: AppDatabase) {

    private val userProfileDao = database.userProfileDao()
    private val clientDao = database.clientDao()
    private val invoiceDao = database.invoiceDao()
    private val paymentDao = database.paymentDao()

    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()
    val allClients: Flow<List<Client>> = clientDao.getAllClients()
    val allInvoicesWithDetails: Flow<List<InvoiceWithDetails>> = invoiceDao.getAllInvoicesWithDetails()
    val recentInvoices: Flow<List<Invoice>> = invoiceDao.getRecentInvoices()
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments()

    fun getInvoiceWithDetails(id: Long): Flow<InvoiceWithDetails?> {
        return invoiceDao.getInvoiceWithDetailsById(id)
    }

    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<Payment>> {
        return paymentDao.getPaymentsForInvoice(invoiceId)
    }

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    suspend fun setLoggedIn(loggedIn: Boolean) = withContext(Dispatchers.IO) {
        userProfileDao.setLoggedIn(loggedIn)
    }

    suspend fun saveClient(client: Client): Long = withContext(Dispatchers.IO) {
        if (client.id == 0L) {
            clientDao.insertClient(client)
        } else {
            clientDao.updateClient(client)
            client.id
        }
    }

    suspend fun deleteClient(client: Client) = withContext(Dispatchers.IO) {
        clientDao.deleteClient(client)
    }

    suspend fun saveInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>): Long = withContext(Dispatchers.IO) {
        invoiceDao.saveInvoiceWithItems(invoice, items)
    }

    suspend fun updateInvoiceStatus(invoiceId: Long, status: InvoiceStatus) = withContext(Dispatchers.IO) {
        val invoice = invoiceDao.getInvoiceById(invoiceId)
        if (invoice != null) {
            val updated = invoice.copy(status = status.name)
            invoiceDao.updateInvoice(updated)
        }
    }

    suspend fun deleteInvoice(invoice: Invoice) = withContext(Dispatchers.IO) {
        invoiceDao.deleteInvoice(invoice)
    }

    suspend fun recordPayment(payment: Payment): Long = withContext(Dispatchers.IO) {
        paymentDao.recordPaymentAndUpdateInvoice(payment, invoiceDao)
    }

    suspend fun getNextInvoiceNumber(): String = withContext(Dispatchers.IO) {
        val count = invoiceDao.getInvoiceCount()
        com.example.util.Formatters.generateInvoiceNumber(count)
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = invoiceDao.getInvoiceCount()
        if (count == 0) {
            seedSampleData(force = false)
        }
    }

    suspend fun seedSampleData(force: Boolean = false) = withContext(Dispatchers.IO) {
        val currentProfile = userProfileDao.getUserProfileOnce()
        if (currentProfile == null || force) {
            userProfileDao.insertOrUpdateProfile(SampleData.defaultProfile)
        }
        val existingClients = clientDao.getClientCount()
        if (existingClients == 0 || force) {
            clientDao.insertAll(SampleData.getSampleClients())
            val bundles = SampleData.getSampleInvoiceBundles()
            for (bundle in bundles) {
                val invId = invoiceDao.insertInvoice(bundle.invoice)
                val itemsWithId = bundle.items.map { it.copy(invoiceId = invId) }
                invoiceDao.insertInvoiceItems(itemsWithId)
                for (payment in bundle.payments) {
                    paymentDao.insertPayment(payment.copy(invoiceId = invId))
                }
            }
        }
    }
}
