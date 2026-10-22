package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.InvoiceStatus
import com.example.ui.screens.AddEditClientScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.CreateInvoiceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoiceDetailScreen
import com.example.ui.screens.InvoicesListScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.InvoiceItTheme
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.InvoiceViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InvoiceItTheme {
                InvoiceItApp()
            }
        }
    }
}

@Composable
fun InvoiceItApp(viewModel: InvoiceViewModel = viewModel()) {
    val userProfile by viewModel.userProfile.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val invoicesWithDetails by viewModel.invoicesWithDetails.collectAsState()
    val recentInvoices by viewModel.recentInvoices.collectAsState()
    val invoiceStatusFilter by viewModel.invoiceStatusFilter.collectAsState()
    val invoiceSearchQuery by viewModel.invoiceSearchQuery.collectAsState()
    val clientSearchQuery by viewModel.clientSearchQuery.collectAsState()
    val selectedInvoiceId by viewModel.selectedInvoiceId.collectAsState()
    val selectedClientForEdit by viewModel.selectedClientForEdit.collectAsState()

    val scope = rememberCoroutineScope()
    val currency = userProfile?.currency ?: "$"

    // If user is not logged in or on LOGIN screen, show LoginScreen
    val isUserLoggedIn = userProfile != null && userProfile!!.isLoggedIn

    if (!isUserLoggedIn || currentScreen == AppScreen.LOGIN) {
        LoginScreen(
            currentProfile = userProfile,
            onLoginSuccess = { profile ->
                viewModel.saveUserProfile(profile)
            },
            onLoadDemoData = {
                viewModel.loadDemoData()
            }
        )
        return
    }

    // Adaptive Layout: Check width for NavigationRail (Desktop/Tablet) vs NavigationBar (Phone)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        val currentNavTab = when (currentScreen) {
            AppScreen.DASHBOARD -> 0
            AppScreen.INVOICES_LIST, AppScreen.CREATE_INVOICE, AppScreen.INVOICE_DETAIL -> 1
            AppScreen.CLIENTS, AppScreen.ADD_EDIT_CLIENT -> 2
            else -> 0
        }

        val showBottomNav = !isWideScreen &&
                (currentScreen == AppScreen.DASHBOARD ||
                        currentScreen == AppScreen.INVOICES_LIST ||
                        currentScreen == AppScreen.CLIENTS)

        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        NavigationBarItem(
                            selected = currentNavTab == 0,
                            onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (currentNavTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                    contentDescription = "Dashboard"
                                )
                            },
                            label = { Text("Dashboard") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Slate900,
                                unselectedIconColor = Slate600,
                                unselectedTextColor = Slate600,
                                indicatorColor = Slate700
                            ),
                            modifier = Modifier.testTag("nav_tab_dashboard")
                        )

                        NavigationBarItem(
                            selected = currentNavTab == 1,
                            onClick = { viewModel.navigateTo(AppScreen.INVOICES_LIST) },
                            icon = {
                                Icon(
                                    imageVector = if (currentNavTab == 1) Icons.Default.ReceiptLong else Icons.Outlined.ReceiptLong,
                                    contentDescription = "Invoices"
                                )
                            },
                            label = { Text("Invoices") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Slate900,
                                unselectedIconColor = Slate600,
                                unselectedTextColor = Slate600,
                                indicatorColor = Slate700
                            ),
                            modifier = Modifier.testTag("nav_tab_invoices")
                        )

                        NavigationBarItem(
                            selected = currentNavTab == 2,
                            onClick = { viewModel.navigateTo(AppScreen.CLIENTS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentNavTab == 2) Icons.Default.People else Icons.Outlined.People,
                                    contentDescription = "Clients"
                                )
                            },
                            label = { Text("Clients") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Slate900,
                                unselectedIconColor = Slate600,
                                unselectedTextColor = Slate600,
                                indicatorColor = Slate700
                            ),
                            modifier = Modifier.testTag("nav_tab_clients")
                        )

                        NavigationBarItem(
                            selected = false,
                            onClick = { viewModel.navigateTo(AppScreen.LOGIN) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Business,
                                    contentDescription = "Profile"
                                )
                            },
                            label = { Text("Profile") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Slate900,
                                unselectedIconColor = Slate600,
                                unselectedTextColor = Slate600,
                                indicatorColor = Slate700
                            ),
                            modifier = Modifier.testTag("nav_tab_profile")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Wide Screen Navigation Rail
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = Color.White,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRailItem(
                            selected = currentNavTab == 0,
                            onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (currentNavTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                    contentDescription = "Dashboard"
                                )
                            },
                            label = { Text("Dashboard") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Slate900,
                                unselectedIconColor = Slate600,
                                unselectedTextColor = Slate600,
                                indicatorColor = Slate700
                            ),
                            modifier = Modifier.testTag("rail_tab_dashboard")
                        )

                        NavigationRailItem(
                            selected = currentNavTab == 1,
                            onClick = { viewModel.navigateTo(AppScreen.INVOICES_LIST) },
                            icon = {
                                Icon(
                                    imageVector = if (currentNavTab == 1) Icons.Default.ReceiptLong else Icons.Outlined.ReceiptLong,
                                    contentDescription = "Invoices"
                                )
                            },
                            label = { Text("Invoices") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Slate900,
                                unselectedIconColor = Slate600,
                                unselectedTextColor = Slate600,
                                indicatorColor = Slate700
                            ),
                            modifier = Modifier.testTag("rail_tab_invoices")
                        )

                        NavigationRailItem(
                            selected = currentNavTab == 2,
                            onClick = { viewModel.navigateTo(AppScreen.CLIENTS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentNavTab == 2) Icons.Default.People else Icons.Outlined.People,
                                    contentDescription = "Clients"
                                )
                            },
                            label = { Text("Clients") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Slate900,
                                unselectedIconColor = Slate600,
                                unselectedTextColor = Slate600,
                                indicatorColor = Slate700
                            ),
                            modifier = Modifier.testTag("rail_tab_clients")
                        )

                        NavigationRailItem(
                            selected = false,
                            onClick = { viewModel.navigateTo(AppScreen.LOGIN) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Business,
                                    contentDescription = "Profile"
                                )
                            },
                            label = { Text("Profile") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Slate900,
                                unselectedIconColor = Slate600,
                                unselectedTextColor = Slate600,
                                indicatorColor = Slate700
                            ),
                            modifier = Modifier.testTag("rail_tab_profile")
                        )
                    }
                }

                // Main Screen Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (currentScreen) {
                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                profile = userProfile,
                                stats = dashboardStats,
                                recentInvoices = recentInvoices,
                                onNewInvoice = { viewModel.startNewInvoice() },
                                onAddClient = { viewModel.startAddClient() },
                                onViewInvoiceDetail = { id -> viewModel.viewInvoiceDetail(id) },
                                onViewAllInvoices = { viewModel.navigateTo(AppScreen.INVOICES_LIST) },
                                onProfileClick = { viewModel.navigateTo(AppScreen.LOGIN) }
                            )
                        }

                        AppScreen.INVOICES_LIST -> {
                            InvoicesListScreen(
                                invoicesWithDetails = invoicesWithDetails,
                                currencySymbol = currency,
                                selectedStatusFilter = invoiceStatusFilter,
                                searchQuery = invoiceSearchQuery,
                                onFilterChange = { filter -> viewModel.setInvoiceStatusFilter(filter) },
                                onSearchChange = { query -> viewModel.setInvoiceSearchQuery(query) },
                                onNewInvoice = { viewModel.startNewInvoice() },
                                onInvoiceClick = { id -> viewModel.viewInvoiceDetail(id) }
                            )
                        }

                        AppScreen.CLIENTS -> {
                            ClientsScreen(
                                clients = clients,
                                invoicesWithDetails = invoicesWithDetails,
                                currencySymbol = currency,
                                searchQuery = clientSearchQuery,
                                onSearchChange = { query -> viewModel.setClientSearchQuery(query) },
                                onAddClient = { viewModel.startAddClient() },
                                onEditClient = { client -> viewModel.startEditClient(client) },
                                onSelectClientInvoices = { client ->
                                    viewModel.setInvoiceSearchQuery(client.name)
                                    viewModel.navigateTo(AppScreen.INVOICES_LIST)
                                }
                            )
                        }

                        AppScreen.ADD_EDIT_CLIENT -> {
                            AddEditClientScreen(
                                clientToEdit = selectedClientForEdit,
                                onBack = { viewModel.navigateBack() },
                                onSaveClient = { client ->
                                    viewModel.saveClient(client) {}
                                },
                                onDeleteClient = if (selectedClientForEdit != null) {
                                    { client -> viewModel.deleteClient(client) }
                                } else null
                            )
                        }

                        AppScreen.CREATE_INVOICE -> {
                            val invoiceWithDetails = selectedInvoiceId?.let { id ->
                                invoicesWithDetails.find { it.invoice.id == id }
                            }
                            var nextNumber = "INV-2026-001"
                            scope.launch {
                                nextNumber = viewModel.getNextInvoiceNumber()
                            }

                            CreateInvoiceScreen(
                                invoiceToEdit = invoiceWithDetails,
                                clients = clients,
                                profile = userProfile,
                                nextSuggestedInvoiceNumber = nextNumber,
                                onBack = { viewModel.navigateBack() },
                                onAddNewClient = { viewModel.startAddClient() },
                                onSaveInvoice = { invoice, items ->
                                    viewModel.saveInvoiceWithItems(invoice, items) { savedId ->
                                        viewModel.viewInvoiceDetail(savedId)
                                    }
                                }
                            )
                        }

                        AppScreen.INVOICE_DETAIL -> {
                            val invoiceWithDetails = selectedInvoiceId?.let { id ->
                                invoicesWithDetails.find { it.invoice.id == id }
                            }

                            InvoiceDetailScreen(
                                invoiceWithDetails = invoiceWithDetails,
                                profile = userProfile,
                                onBack = { viewModel.navigateBack() },
                                onEditInvoice = { id -> viewModel.editInvoice(id) },
                                onDeleteInvoice = { invoice ->
                                    viewModel.deleteInvoice(invoice) {}
                                },
                                onRecordPayment = { amount, date, method, notes ->
                                    if (invoiceWithDetails != null) {
                                        viewModel.recordPayment(
                                            invoiceId = invoiceWithDetails.invoice.id,
                                            invoiceNumber = invoiceWithDetails.invoice.invoiceNumber,
                                            amount = amount,
                                            method = method,
                                            notes = notes,
                                            paymentDate = date
                                        ) {}
                                    }
                                },
                                onMarkAsPaid = {
                                    if (invoiceWithDetails != null) {
                                        val remaining = invoiceWithDetails.invoice.remainingBalance
                                        viewModel.recordPayment(
                                            invoiceId = invoiceWithDetails.invoice.id,
                                            invoiceNumber = invoiceWithDetails.invoice.invoiceNumber,
                                            amount = remaining,
                                            method = "Bank Transfer",
                                            notes = "Marked as paid in full",
                                            paymentDate = System.currentTimeMillis()
                                        ) {}
                                    }
                                }
                            )
                        }

                        AppScreen.LOGIN -> {
                            LoginScreen(
                                currentProfile = userProfile,
                                onLoginSuccess = { profile ->
                                    viewModel.saveUserProfile(profile)
                                },
                                onLoadDemoData = {
                                    viewModel.loadDemoData()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
