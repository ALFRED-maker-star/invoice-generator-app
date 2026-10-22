package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceStatus
import com.example.data.model.InvoiceWithDetails
import com.example.data.model.Payment
import com.example.data.model.UserProfile
import com.example.data.repository.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppScreen {
    LOGIN,
    DASHBOARD,
    CLIENTS,
    ADD_EDIT_CLIENT,
    INVOICES_LIST,
    CREATE_INVOICE,
    INVOICE_DETAIL
}

data class DashboardStats(
    val totalInvoiced: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val paidThisMonth: Double = 0.0,
    val overdueCount: Int = 0,
    val overdueAmount: Double = 0.0,
    val totalInvoicesCount: Int = 0,
    val paidCount: Int = 0,
    val pendingCount: Int = 0,
    val draftCount: Int = 0
)

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InvoiceRepository(AppDatabase.getInstance(application))

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoicesWithDetails: StateFlow<List<InvoiceWithDetails>> = repository.allInvoicesWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentInvoices: StateFlow<List<Invoice>> = repository.recentInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<Payment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Navigation Screen & Stack
    private val _currentScreen = MutableStateFlow(AppScreen.LOGIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val navigationBackStack = mutableListOf<AppScreen>()

    // Selected items for detail/edit
    private val _selectedInvoiceId = MutableStateFlow<Long?>(null)
    val selectedInvoiceId: StateFlow<Long?> = _selectedInvoiceId.asStateFlow()

    private val _selectedClientForEdit = MutableStateFlow<Client?>(null)
    val selectedClientForEdit: StateFlow<Client?> = _selectedClientForEdit.asStateFlow()

    // Filter and search
    private val _invoiceStatusFilter = MutableStateFlow<InvoiceStatus?>(null)
    val invoiceStatusFilter: StateFlow<InvoiceStatus?> = _invoiceStatusFilter.asStateFlow()

    private val _invoiceSearchQuery = MutableStateFlow("")
    val invoiceSearchQuery: StateFlow<String> = _invoiceSearchQuery.asStateFlow()

    private val _clientSearchQuery = MutableStateFlow("")
    val clientSearchQuery: StateFlow<String> = _clientSearchQuery.asStateFlow()

    // Dashboard Stats combining invoices and payments
    val dashboardStats: StateFlow<DashboardStats> = combine(
        invoicesWithDetails,
        allPayments
    ) { invoices, payments ->
        var totalInvoiced = 0.0
        var totalOutstanding = 0.0
        var overdueCount = 0
        var overdueAmount = 0.0
        var paidCount = 0
        var pendingCount = 0
        var draftCount = 0

        for (item in invoices) {
            val inv = item.invoice
            val status = inv.computedStatus
            totalInvoiced += inv.totalAmount
            if (status == InvoiceStatus.PENDING || status == InvoiceStatus.OVERDUE) {
                totalOutstanding += inv.remainingBalance
            }
            when (status) {
                InvoiceStatus.PAID -> paidCount++
                InvoiceStatus.PENDING -> pendingCount++
                InvoiceStatus.OVERDUE -> {
                    overdueCount++
                    overdueAmount += inv.remainingBalance
                }
                InvoiceStatus.DRAFT -> draftCount++
            }
        }

        // Paid this calendar month
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        var paidThisMonth = 0.0

        for (p in payments) {
            cal.timeInMillis = p.paymentDate
            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                paidThisMonth += p.amount
            }
        }

        DashboardStats(
            totalInvoiced = totalInvoiced,
            totalOutstanding = totalOutstanding,
            paidThisMonth = paidThisMonth,
            overdueCount = overdueCount,
            overdueAmount = overdueAmount,
            totalInvoicesCount = invoices.size,
            paidCount = paidCount,
            pendingCount = pendingCount,
            draftCount = draftCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            navigationBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (navigationBackStack.isNotEmpty()) {
            val prev = navigationBackStack.removeAt(navigationBackStack.size - 1)
            _currentScreen.value = prev
            return true
        }
        return false
    }

    fun viewInvoiceDetail(invoiceId: Long) {
        _selectedInvoiceId.value = invoiceId
        navigateTo(AppScreen.INVOICE_DETAIL)
    }

    fun editInvoice(invoiceId: Long) {
        _selectedInvoiceId.value = invoiceId
        navigateTo(AppScreen.CREATE_INVOICE)
    }

    fun startNewInvoice() {
        _selectedInvoiceId.value = null
        navigateTo(AppScreen.CREATE_INVOICE)
    }

    fun startAddClient() {
        _selectedClientForEdit.value = null
        navigateTo(AppScreen.ADD_EDIT_CLIENT)
    }

    fun startEditClient(client: Client) {
        _selectedClientForEdit.value = client
        navigateTo(AppScreen.ADD_EDIT_CLIENT)
    }

    fun setInvoiceStatusFilter(status: InvoiceStatus?) {
        _invoiceStatusFilter.value = status
    }

    fun setInvoiceSearchQuery(query: String) {
        _invoiceSearchQuery.value = query
    }

    fun setClientSearchQuery(query: String) {
        _clientSearchQuery.value = query
    }

    fun saveUserProfile(profile: UserProfile, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveUserProfile(profile.copy(isLoggedIn = true))
            _currentScreen.value = AppScreen.DASHBOARD
            navigationBackStack.clear()
            onComplete()
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.setLoggedIn(false)
            _currentScreen.value = AppScreen.LOGIN
            navigationBackStack.clear()
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            repository.seedSampleData(force = true)
            _currentScreen.value = AppScreen.DASHBOARD
            navigationBackStack.clear()
        }
    }

    fun saveClient(client: Client, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.saveClient(client)
            navigateBack()
            onComplete()
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    fun saveInvoiceWithItems(
        invoice: Invoice,
        items: List<InvoiceItem>,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val invoiceId = repository.saveInvoiceWithItems(invoice, items)
            _selectedInvoiceId.value = invoiceId
            navigateBack()
            onComplete(invoiceId)
        }
    }

    fun updateInvoiceStatus(invoiceId: Long, status: InvoiceStatus) {
        viewModelScope.launch {
            repository.updateInvoiceStatus(invoiceId, status)
        }
    }

    fun deleteInvoice(invoice: Invoice, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
            navigateBack()
            onComplete()
        }
    }

    fun recordPayment(
        invoiceId: Long,
        invoiceNumber: String,
        amount: Double,
        method: String,
        notes: String,
        paymentDate: Long = System.currentTimeMillis(),
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val payment = Payment(
                invoiceId = invoiceId,
                invoiceNumber = invoiceNumber,
                amount = amount,
                paymentDate = paymentDate,
                paymentMethod = method,
                referenceNotes = notes
            )
            repository.recordPayment(payment)
            onComplete()
        }
    }

    suspend fun getNextInvoiceNumber(): String {
        return repository.getNextInvoiceNumber()
    }
}
