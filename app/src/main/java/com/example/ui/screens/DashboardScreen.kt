package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.data.model.InvoiceStatus
import com.example.data.model.UserProfile
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.InvoiceCardItem
import com.example.ui.components.StatCard
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending
import com.example.ui.viewmodel.DashboardStats
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profile: UserProfile?,
    stats: DashboardStats,
    recentInvoices: List<Invoice>,
    onNewInvoice: () -> Unit,
    onAddClient: () -> Unit,
    onViewInvoiceDetail: (Long) -> Unit,
    onViewAllInvoices: () -> Unit,
    onProfileClick: () -> Unit
) {
    val currency = profile?.currency ?: "$"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Dashboard Greeting & Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = profile?.businessName?.ifBlank { "Workspace Overview" } ?: "Workspace Overview",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                }

                InitialsAvatar(
                    name = profile?.businessName?.ifBlank { profile.ownerName } ?: "InvoiceIt",
                    modifier = Modifier
                        .clickable(onClick = onProfileClick)
                        .testTag("dashboard_profile_avatar")
                )
            }
        }

        // Overdue Alert Banner if any overdue
        if (stats.overdueCount > 0) {
            item {
                Surface(
                    color = StatusOverdueBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StatusOverdue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StatusOverdue.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StatusOverdue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${stats.overdueCount} Overdue Invoice${if (stats.overdueCount > 1) "s" else ""}",
                                fontWeight = FontWeight.Bold,
                                color = StatusOverdue,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${Formatters.formatCurrency(stats.overdueAmount, currency)} pending collection. Send reminders to clients.",
                                color = Slate700,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 3 Key Stat Cards (Mobile: Stack / Tablet: Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = "Total Invoiced",
                        amount = Formatters.formatCurrency(stats.totalInvoiced, currency),
                        icon = Icons.Default.Receipt,
                        accentColor = Slate700,
                        subtitle = "${stats.totalInvoicesCount} invoices created",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Outstanding",
                        amount = Formatters.formatCurrency(stats.totalOutstanding, currency),
                        icon = Icons.Default.HourglassTop,
                        accentColor = StatusPending,
                        subtitle = "${stats.pendingCount + stats.overdueCount} unpaid",
                        modifier = Modifier.weight(1f)
                    )
                }

                StatCard(
                    title = "Paid This Month",
                    amount = Formatters.formatCurrency(stats.paidThisMonth, currency),
                    icon = Icons.Default.AttachMoney,
                    accentColor = StatusPaid,
                    subtitle = "Collected in current month",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Quick Actions Row
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onNewInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("dashboard_new_invoice_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Invoice", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddClient,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("dashboard_add_client_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Slate700, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Client", color = Slate700, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Status Breakdown Chart Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                    .testTag("status_breakdown_chart")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Invoice Status Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Distribution across all active invoices",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val total = stats.totalInvoicesCount
                    if (total == 0) {
                        Text(
                            text = "No invoices yet. Create your first invoice above!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        // Segmented Distribution Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate100)
                        ) {
                            if (stats.paidCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(stats.paidCount.toFloat())
                                        .fillMaxSize()
                                        .background(StatusPaid)
                                )
                            }
                            if (stats.pendingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(stats.pendingCount.toFloat())
                                        .fillMaxSize()
                                        .background(StatusPending)
                                )
                            }
                            if (stats.overdueCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(stats.overdueCount.toFloat())
                                        .fillMaxSize()
                                        .background(StatusOverdue)
                                )
                            }
                            if (stats.draftCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(stats.draftCount.toFloat())
                                        .fillMaxSize()
                                        .background(StatusDraft)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusLegendItem(label = "Paid", count = stats.paidCount, total = total, color = StatusPaid)
                            StatusLegendItem(label = "Pending", count = stats.pendingCount, total = total, color = StatusPending)
                            StatusLegendItem(label = "Overdue", count = stats.overdueCount, total = total, color = StatusOverdue)
                            StatusLegendItem(label = "Draft", count = stats.draftCount, total = total, color = StatusDraft)
                        }
                    }
                }
            }
        }

        // Recent Invoices Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Recent Invoices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "View All (${stats.totalInvoicesCount})",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate700,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onViewAllInvoices)
                        .testTag("dashboard_view_all_invoices")
                )
            }
        }

        // Recent Invoices List
        if (recentInvoices.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No invoices created yet",
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                    }
                }
            }
        } else {
            items(recentInvoices, key = { it.id }) { invoice ->
                InvoiceCardItem(
                    invoice = invoice,
                    currencySymbol = currency,
                    onClick = { onViewInvoiceDetail(invoice.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatusLegendItem(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val pct = if (total > 0) ((count.toFloat() / total) * 100).toInt() else 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Slate600,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "$count ($pct%)",
            fontSize = 11.sp,
            color = Slate900,
            fontWeight = FontWeight.Bold
        )
    }
}
