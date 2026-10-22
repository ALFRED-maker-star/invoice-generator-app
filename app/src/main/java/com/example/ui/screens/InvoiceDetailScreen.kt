package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.data.model.InvoiceStatus
import com.example.data.model.InvoiceWithDetails
import com.example.data.model.UserProfile
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.InvoiceStatusBadge
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending
import com.example.util.Formatters
import com.example.util.PdfGenerator

@Composable
fun InvoiceDetailScreen(
    invoiceWithDetails: InvoiceWithDetails?,
    profile: UserProfile?,
    onBack: () -> Unit,
    onEditInvoice: (Long) -> Unit,
    onDeleteInvoice: (Invoice) -> Unit,
    onRecordPayment: (amount: Double, date: Long, method: String, notes: String) -> Unit,
    onMarkAsPaid: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    if (invoiceWithDetails == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
        ) {
            Text("Invoice not found", color = Slate700)
        }
        return
    }

    val invoice = invoiceWithDetails.invoice
    val client = invoiceWithDetails.client
    val items = invoiceWithDetails.items
    val payments = invoiceWithDetails.payments
    val currency = profile?.currency ?: "$"
    val status = invoice.computedStatus

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Action Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("invoice_detail_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate900
                        )
                    }
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEditInvoice(invoice.id) },
                        modifier = Modifier.testTag("detail_edit_invoice_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate700)
                    }
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("detail_delete_invoice_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusOverdue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Document Preview (Scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Formal Invoice Document Sheet Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .testTag("formal_invoice_document")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Top Accent Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Slate700)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Header: Business info & "INVOICE"
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.businessName?.ifBlank { "InvoiceIt Studio" } ?: "InvoiceIt Studio",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                if (!profile?.ownerName.isNullOrBlank()) {
                                    Text(
                                        text = profile?.ownerName ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                                if (!profile?.email.isNullOrBlank()) {
                                    Text(
                                        text = profile?.email ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                                if (!profile?.phone.isNullOrBlank()) {
                                    Text(
                                        text = profile?.phone ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                                if (!profile?.taxId.isNullOrBlank()) {
                                    Text(
                                        text = "Tax/VAT: ${profile?.taxId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "INVOICE",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate700,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = invoice.invoiceNumber,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                InvoiceStatusBadge(status = status)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Slate200)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Meta details: Bill To & Dates Grid
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Bill To
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = "BILL TO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = client?.name ?: invoice.clientName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                val company = client?.company?.ifBlank { invoice.clientCompany } ?: invoice.clientCompany
                                if (company.isNotBlank()) {
                                    Text(
                                        text = company,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                                if (!client?.email.isNullOrBlank()) {
                                    Text(
                                        text = client?.email ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                                if (!client?.address.isNullOrBlank()) {
                                    Text(
                                        text = client?.address ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                            }

                            // Dates
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text(
                                    text = "Issue Date:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500
                                )
                                Text(
                                    text = Formatters.formatDate(invoice.issueDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate900
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Due Date:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500
                                )
                                Text(
                                    text = Formatters.formatDate(invoice.dueDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (status == InvoiceStatus.OVERDUE) StatusOverdue else Slate900
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Itemized Table
                        Text(
                            text = "ITEMIZED SERVICES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Table Header
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text("Description", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate700, modifier = Modifier.weight(2f))
                                Text("Qty", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate700, modifier = Modifier.weight(0.6f))
                                Text("Price", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate700, modifier = Modifier.weight(1f))
                                Text("Total", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate700, modifier = Modifier.weight(1f))
                            }
                        }

                        // Table Rows
                        items.forEachIndexed { index, item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 1) Slate50 else Color.White)
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Text(item.description, style = MaterialTheme.typography.bodySmall, color = Slate900, modifier = Modifier.weight(2f))
                                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                                Text(qtyStr, style = MaterialTheme.typography.bodySmall, color = Slate600, modifier = Modifier.weight(0.6f))
                                Text(Formatters.formatCurrency(item.unitPrice, currency), style = MaterialTheme.typography.bodySmall, color = Slate600, modifier = Modifier.weight(1f))
                                Text(Formatters.formatCurrency(item.lineTotal, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Slate900, modifier = Modifier.weight(1f))
                            }
                            HorizontalDivider(color = Slate100, thickness = 0.5.dp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Financial Summary Section (Right Aligned)
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.width(260.dp)
                            ) {
                                Text("Subtotal:", color = Slate600, fontSize = 13.sp)
                                Text(Formatters.formatCurrency(invoice.subtotal, currency), fontWeight = FontWeight.Medium, color = Slate900, fontSize = 13.sp)
                            }

                            if (invoice.discountAmount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.width(260.dp)
                                ) {
                                    Text("Discount (${invoice.discountRate}%):", color = Slate600, fontSize = 13.sp)
                                    Text("-${Formatters.formatCurrency(invoice.discountAmount, currency)}", color = Slate700, fontSize = 13.sp)
                                }
                            }

                            if (invoice.taxAmount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.width(260.dp)
                                ) {
                                    Text("Tax (${invoice.taxRate}%):", color = Slate600, fontSize = 13.sp)
                                    Text(Formatters.formatCurrency(invoice.taxAmount, currency), color = Slate700, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(260.dp)
                                    .height(1.dp)
                                    .background(Slate300)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.width(260.dp)
                            ) {
                                Text("Grand Total:", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                                Text(Formatters.formatCurrency(invoice.totalAmount, currency), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                            }

                            if (invoice.amountPaid > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.width(260.dp)
                                ) {
                                    Text("Amount Paid:", color = StatusPaid, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("-${Formatters.formatCurrency(invoice.amountPaid, currency)}", color = StatusPaid, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.width(260.dp)
                                ) {
                                    Text("Balance Due:", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                                    Text(Formatters.formatCurrency(invoice.remainingBalance, currency), fontWeight = FontWeight.Bold, color = if (invoice.remainingBalance > 0) StatusPending else StatusPaid, fontSize = 15.sp)
                                }
                            }
                        }

                        // Notes & Payment Instructions
                        val instructions = invoice.notes.ifBlank { profile?.paymentInstructions.orEmpty() }
                        if (instructions.isNotBlank()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Surface(
                                color = Slate50,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "PAYMENT INSTRUCTIONS & NOTES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = instructions,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate700
                                    )
                                }
                            }
                        }
                    }
                }

                // Payment History Card if payments exist
                if (payments.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = StatusPaid, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Payment History (${payments.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                }
                                Text(
                                    text = "Total: ${Formatters.formatCurrency(invoice.amountPaid, currency)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPaid
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            payments.forEach { payment ->
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "${payment.paymentMethod} • ${Formatters.formatDate(payment.paymentDate)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate900
                                        )
                                        if (payment.referenceNotes.isNotBlank()) {
                                            Text(
                                                text = payment.referenceNotes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Slate500
                                            )
                                        }
                                    }
                                    Text(
                                        text = Formatters.formatCurrency(payment.amount, currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusPaid
                                    )
                                }
                                HorizontalDivider(color = Slate100)
                            }
                        }
                    }
                }

                // Action Buttons Grid (PDF, Mark Paid / Payment, Send Reminder)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Invoice Actions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )

                        // 1. Download / Share PDF
                        Button(
                            onClick = {
                                PdfGenerator.generateAndShareInvoicePdf(context, invoiceWithDetails, profile)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("download_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download / Share PDF", fontWeight = FontWeight.Bold)
                        }

                        // 2. Record Payment / Mark as Paid
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showPaymentDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("record_payment_btn")
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, tint = StatusPaid, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Record Payment", color = Slate900, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            if (invoice.remainingBalance > 0) {
                                Button(
                                    onClick = onMarkAsPaid,
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusPaid),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("mark_as_paid_btn")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark as Paid", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        // 3. Send Reminder
                        OutlinedButton(
                            onClick = {
                                val recipientEmail = client?.email.orEmpty()
                                val clientName = client?.name ?: invoice.clientName
                                val bizName = profile?.businessName ?: "Apex Studio"
                                val balanceDueStr = Formatters.formatCurrency(invoice.remainingBalance, currency)
                                val dueDateStr = Formatters.formatDate(invoice.dueDate)

                                val reminderSubject = "Payment Reminder: Invoice ${invoice.invoiceNumber} ($balanceDueStr due $dueDateStr)"
                                val reminderBody = """
                                    Hi $clientName,

                                    This is a friendly reminder that Invoice ${invoice.invoiceNumber} for $balanceDueStr was due on $dueDateStr.

                                    If payment has already been sent, please disregard this note. Otherwise, kindly remit payment per the instructions on your invoice.

                                    Thank you!
                                    $bizName
                                """.trimIndent()

                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$recipientEmail")
                                    putExtra(Intent.EXTRA_SUBJECT, reminderSubject)
                                    putExtra(Intent.EXTRA_TEXT, reminderBody)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Send Payment Reminder"))
                                } catch (_: Exception) {
                                    // Fallback to general share intent if no mail client
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, reminderSubject)
                                        putExtra(Intent.EXTRA_TEXT, reminderBody)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Send Payment Reminder"))
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("send_reminder_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Slate700, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Reminder to Client", color = Slate700, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Record Payment Dialog
        if (showPaymentDialog) {
            RecordPaymentDialog(
                invoice = invoice,
                currencySymbol = currency,
                onDismiss = { showPaymentDialog = false },
                onConfirmPayment = { amount, date, method, notes ->
                    showPaymentDialog = false
                    onRecordPayment(amount, date, method, notes)
                    Toast.makeText(context, "Payment of ${Formatters.formatCurrency(amount, currency)} recorded!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Invoice", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to permanently delete invoice ${invoice.invoiceNumber}? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteInvoice(invoice)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = Slate700)
                    }
                }
            )
        }
    }
}
