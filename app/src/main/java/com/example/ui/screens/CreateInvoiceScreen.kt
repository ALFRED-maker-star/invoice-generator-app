package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceStatus
import com.example.data.model.InvoiceWithDetails
import com.example.data.model.UserProfile
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusOverdue
import com.example.util.Formatters
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class EditableLineItem(
    var description: String = "",
    var quantityText: String = "1",
    var unitPriceText: String = ""
) {
    val quantity: Double
        get() = quantityText.toDoubleOrNull() ?: 0.0

    val unitPrice: Double
        get() = unitPriceText.toDoubleOrNull() ?: 0.0

    val lineTotal: Double
        get() = quantity * unitPrice
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(
    invoiceToEdit: InvoiceWithDetails?,
    clients: List<Client>,
    profile: UserProfile?,
    nextSuggestedInvoiceNumber: String,
    onBack: () -> Unit,
    onAddNewClient: () -> Unit,
    onSaveInvoice: (Invoice, List<InvoiceItem>) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currency = profile?.currency ?: "$"

    var invoiceNumber by remember { mutableStateOf(invoiceToEdit?.invoice?.invoiceNumber ?: nextSuggestedInvoiceNumber) }
    var selectedClient by remember { mutableStateOf(invoiceToEdit?.client ?: clients.firstOrNull()) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    var issueDate by remember { mutableLongStateOf(invoiceToEdit?.invoice?.issueDate ?: System.currentTimeMillis()) }
    var dueDate by remember { mutableLongStateOf(invoiceToEdit?.invoice?.dueDate ?: (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(15))) }

    val lineItems = remember {
        mutableStateListOf<EditableLineItem>().apply {
            if (invoiceToEdit != null && invoiceToEdit.items.isNotEmpty()) {
                addAll(invoiceToEdit.items.map {
                    EditableLineItem(
                        description = it.description,
                        quantityText = if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else it.quantity.toString(),
                        unitPriceText = if (it.unitPrice % 1.0 == 0.0) it.unitPrice.toInt().toString() else it.unitPrice.toString()
                    )
                })
            } else {
                add(EditableLineItem(description = "", quantityText = "1", unitPriceText = ""))
            }
        }
    }

    var taxRateText by remember { mutableStateOf(if (invoiceToEdit != null && invoiceToEdit.invoice.taxRate > 0) invoiceToEdit.invoice.taxRate.toString() else "0") }
    var discountRateText by remember { mutableStateOf(if (invoiceToEdit != null && invoiceToEdit.invoice.discountRate > 0) invoiceToEdit.invoice.discountRate.toString() else "0") }
    var notes by remember {
        mutableStateOf(
            invoiceToEdit?.invoice?.notes ?: (profile?.paymentInstructions.orEmpty())
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Computations
    val subtotal = lineItems.sumOf { it.lineTotal }
    val taxRate = taxRateText.toDoubleOrNull() ?: 0.0
    val discountRate = discountRateText.toDoubleOrNull() ?: 0.0

    val discountAmount = (subtotal * (discountRate / 100.0)).coerceAtLeast(0.0)
    val taxableAmount = (subtotal - discountAmount).coerceAtLeast(0.0)
    val taxAmount = (taxableAmount * (taxRate / 100.0)).coerceAtLeast(0.0)
    val grandTotal = (taxableAmount + taxAmount).coerceAtLeast(0.0)

    // Date picker dialog helper
    fun showDatePicker(initialTimestamp: Long, onDateSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialTimestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                onDateSelected(newCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

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

            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("create_invoice_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate900)
                    }
                    Text(
                        text = if (invoiceToEdit != null) "Edit Invoice" else "New Invoice",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Form container
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Client Selection Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Client (Bill To) *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            TextButton(
                                onClick = onAddNewClient,
                                modifier = Modifier.testTag("inline_add_client_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Client", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (clients.isEmpty()) {
                            Text(
                                text = "No clients created yet. Please tap '+ New Client' to add your first client.",
                                color = StatusOverdue,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = clientDropdownExpanded,
                                onExpandedChange = { clientDropdownExpanded = !clientDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedClient?.let { "${it.name}${if (it.company.isNotBlank()) " (${it.company})" else ""}" } ?: "Select Client",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Slate500) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("client_selector_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = clientDropdownExpanded,
                                    onDismissRequest = { clientDropdownExpanded = false }
                                ) {
                                    clients.forEach { client ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(text = client.name, fontWeight = FontWeight.Bold, color = Slate900)
                                                    if (client.company.isNotBlank()) {
                                                        Text(text = client.company, style = MaterialTheme.typography.bodySmall, color = Slate500)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedClient = client
                                                clientDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Invoice Meta & Dates Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Invoice Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = { invoiceNumber = it },
                            label = { Text("Invoice Number") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_number_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Issue Date
                            OutlinedTextField(
                                value = Formatters.formatDate(issueDate),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Issue Date") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showDatePicker(issueDate) { issueDate = it }
                                    }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = Slate700)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDatePicker(issueDate) { issueDate = it } }
                                    .testTag("issue_date_input")
                            )

                            // Due Date
                            OutlinedTextField(
                                value = Formatters.formatDate(dueDate),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Due Date") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showDatePicker(dueDate) { dueDate = it }
                                    }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Due Date", tint = Slate700)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDatePicker(dueDate) { dueDate = it } }
                                    .testTag("due_date_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Term Presets
                        Text(
                            text = "Quick Due Date Presets:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                0L to "Receipt",
                                15L to "Net 15",
                                30L to "Net 30",
                                60L to "Net 60"
                            ).forEach { (days, label) ->
                                Surface(
                                    color = Slate100,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .clickable {
                                            dueDate = issueDate + TimeUnit.DAYS.toMillis(days)
                                        }
                                        .testTag("preset_$label")
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Line Items Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Line Items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "${lineItems.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        lineItems.forEachIndexed { index, item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = item.description,
                                        onValueChange = {
                                            lineItems[index] = item.copy(description = it)
                                        },
                                        label = { Text("Item ${index + 1} Description *") },
                                        placeholder = { Text("e.g. Website Design, Consulting...") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("item_desc_$index")
                                    )

                                    if (lineItems.size > 1) {
                                        IconButton(
                                            onClick = { lineItems.removeAt(index) },
                                            modifier = Modifier.testTag("remove_item_$index")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = StatusOverdue)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = item.quantityText,
                                        onValueChange = {
                                            lineItems[index] = item.copy(quantityText = it)
                                        },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("item_qty_$index")
                                    )

                                    OutlinedTextField(
                                        value = item.unitPriceText,
                                        onValueChange = {
                                            lineItems[index] = item.copy(unitPriceText = it)
                                        },
                                        label = { Text("Price ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .testTag("item_price_$index")
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text(text = "Total", style = MaterialTheme.typography.labelSmall, color = Slate500)
                                        Text(
                                            text = Formatters.formatCurrency(item.lineTotal, currency),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Slate100)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                lineItems.add(EditableLineItem(description = "", quantityText = "1", unitPriceText = ""))
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_line_item_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Line Item", color = Slate700, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Financial Summary Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Subtotal", color = Slate600)
                            Text(Formatters.formatCurrency(subtotal, currency), fontWeight = FontWeight.SemiBold, color = Slate900)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = discountRateText,
                                onValueChange = { discountRateText = it },
                                label = { Text("Discount %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("discount_rate_input")
                            )

                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                Text("Discount Amount", style = MaterialTheme.typography.labelSmall, color = Slate500)
                                Text("-${Formatters.formatCurrency(discountAmount, currency)}", color = Slate700, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = taxRateText,
                                onValueChange = { taxRateText = it },
                                label = { Text("Tax Rate %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tax_rate_input")
                            )

                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                Text("Tax Amount", style = MaterialTheme.typography.labelSmall, color = Slate500)
                                Text(Formatters.formatCurrency(taxAmount, currency), color = Slate700, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Slate200)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Grand Total",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = Formatters.formatCurrency(grandTotal, currency),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                        }
                    }
                }

                // Notes & Payment Instructions Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Notes & Payment Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Bank account details, wire instructions, terms...") },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_notes_input")
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Action Buttons: "Save as Draft" & "Send Invoice"
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            validateAndSave(
                                isDraft = true,
                                selectedClient = selectedClient,
                                invoiceNumber = invoiceNumber,
                                issueDate = issueDate,
                                dueDate = dueDate,
                                lineItems = lineItems,
                                subtotal = subtotal,
                                taxRate = taxRate,
                                taxAmount = taxAmount,
                                discountRate = discountRate,
                                discountAmount = discountAmount,
                                grandTotal = grandTotal,
                                notes = notes,
                                existingInvoice = invoiceToEdit?.invoice,
                                onError = { errorMessage = it },
                                onSave = onSaveInvoice
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_as_draft_btn")
                    ) {
                        Text("Save as Draft", color = Slate700, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            validateAndSave(
                                isDraft = false,
                                selectedClient = selectedClient,
                                invoiceNumber = invoiceNumber,
                                issueDate = issueDate,
                                dueDate = dueDate,
                                lineItems = lineItems,
                                subtotal = subtotal,
                                taxRate = taxRate,
                                taxAmount = taxAmount,
                                discountRate = discountRate,
                                discountAmount = discountAmount,
                                grandTotal = grandTotal,
                                notes = notes,
                                existingInvoice = invoiceToEdit?.invoice,
                                onError = { errorMessage = it },
                                onSave = onSaveInvoice
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("send_invoice_btn")
                    ) {
                        Text("Send Invoice", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

private fun validateAndSave(
    isDraft: Boolean,
    selectedClient: Client?,
    invoiceNumber: String,
    issueDate: Long,
    dueDate: Long,
    lineItems: List<EditableLineItem>,
    subtotal: Double,
    taxRate: Double,
    taxAmount: Double,
    discountRate: Double,
    discountAmount: Double,
    grandTotal: Double,
    notes: String,
    existingInvoice: Invoice?,
    onError: (String) -> Unit,
    onSave: (Invoice, List<InvoiceItem>) -> Unit
) {
    if (selectedClient == null) {
        onError("Please select a client for this invoice.")
        return
    }
    if (invoiceNumber.isBlank()) {
        onError("Please provide an invoice number.")
        return
    }
    val validItems = lineItems.filter { it.description.isNotBlank() }
    if (validItems.isEmpty() && !isDraft) {
        onError("Please add at least one line item with a description.")
        return
    }

    val status = if (isDraft) {
        InvoiceStatus.DRAFT.name
    } else {
        if (existingInvoice != null && existingInvoice.amountPaid >= grandTotal && grandTotal > 0.0) {
            InvoiceStatus.PAID.name
        } else {
            InvoiceStatus.PENDING.name
        }
    }

    val invoice = Invoice(
        id = existingInvoice?.id ?: 0L,
        invoiceNumber = invoiceNumber.trim(),
        clientId = selectedClient.id,
        clientName = selectedClient.name,
        clientCompany = selectedClient.company,
        issueDate = issueDate,
        dueDate = dueDate,
        status = status,
        subtotal = subtotal,
        taxRate = taxRate,
        taxAmount = taxAmount,
        discountRate = discountRate,
        discountAmount = discountAmount,
        totalAmount = grandTotal,
        amountPaid = existingInvoice?.amountPaid ?: 0.0,
        notes = notes.trim(),
        createdAt = existingInvoice?.createdAt ?: System.currentTimeMillis()
    )

    val dbItems = validItems.map {
        InvoiceItem(
            invoiceId = invoice.id,
            description = it.description.trim(),
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            lineTotal = it.lineTotal
        )
    }

    onSave(invoice, dbItems)
}
