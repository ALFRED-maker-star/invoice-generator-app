package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.InvoiceStatus
import com.example.data.model.InvoiceWithDetails
import com.example.data.model.UserProfile
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun generateAndShareInvoicePdf(
        context: Context,
        invoiceWithDetails: InvoiceWithDetails,
        userProfile: UserProfile?
    ) {
        try {
            val file = generateInvoicePdf(context, invoiceWithDetails, userProfile)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${invoiceWithDetails.invoice.invoiceNumber} from ${userProfile?.businessName ?: "Freelancer"}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Hi ${invoiceWithDetails.client?.name ?: "Client"},\n\nPlease find attached Invoice ${invoiceWithDetails.invoice.invoiceNumber} for ${Formatters.formatCurrency(invoiceWithDetails.invoice.totalAmount, userProfile?.currency ?: "$")}.\n\nThank you for your business!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Invoice PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to generate PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateInvoicePdf(
        context: Context,
        invoiceWithDetails: InvoiceWithDetails,
        userProfile: UserProfile?
    ): File {
        val invoice = invoiceWithDetails.invoice
        val client = invoiceWithDetails.client
        val items = invoiceWithDetails.items
        val currency = userProfile?.currency ?: "$"

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        // Top Accent Banner (Slate 700: #334155)
        paint.color = Color.rgb(51, 65, 85)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, paint)

        var y = 55f

        // Business Name / Logo
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val bizName = userProfile?.businessName?.ifBlank { "InvoiceIt Freelancer" } ?: "InvoiceIt Freelancer"
        canvas.drawText(bizName, 40f, y, paint)

        // "INVOICE" Title right aligned
        paint.textSize = 24f
        paint.color = Color.rgb(51, 65, 85)
        val titleText = "INVOICE"
        val titleWidth = paint.measureText(titleText)
        canvas.drawText(titleText, (pageWidth - 40 - titleWidth), y, paint)

        y += 20f

        // Business details (Left) & Invoice meta (Right)
        paint.textSize = 10f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val ownerName = userProfile?.ownerName.orEmpty()
        if (ownerName.isNotBlank()) {
            canvas.drawText(ownerName, 40f, y, paint)
        }
        val invoiceNumText = "Invoice #: ${invoice.invoiceNumber}"
        val numWidth = paint.measureText(invoiceNumText)
        canvas.drawText(invoiceNumText, (pageWidth - 40 - numWidth), y, paint)

        y += 15f
        val bizEmail = userProfile?.email.orEmpty()
        if (bizEmail.isNotBlank()) {
            canvas.drawText(bizEmail, 40f, y, paint)
        }
        val issueDateText = "Date: ${Formatters.formatDate(invoice.issueDate)}"
        val issueWidth = paint.measureText(issueDateText)
        canvas.drawText(issueDateText, (pageWidth - 40 - issueWidth), y, paint)

        y += 15f
        val bizPhone = userProfile?.phone.orEmpty()
        if (bizPhone.isNotBlank()) {
            canvas.drawText(bizPhone, 40f, y, paint)
        }
        val dueDateText = "Due Date: ${Formatters.formatDate(invoice.dueDate)}"
        val dueWidth = paint.measureText(dueDateText)
        canvas.drawText(dueDateText, (pageWidth - 40 - dueWidth), y, paint)

        y += 15f
        val bizTax = userProfile?.taxId.orEmpty()
        if (bizTax.isNotBlank()) {
            canvas.drawText("Tax/Reg: $bizTax", 40f, y, paint)
        }

        // Status Badge (Right)
        val status = invoice.computedStatus
        val (statusColor, statusBg) = when (status) {
            InvoiceStatus.PAID -> Pair(Color.rgb(34, 197, 94), Color.rgb(220, 252, 231))
            InvoiceStatus.PENDING -> Pair(Color.rgb(245, 158, 11), Color.rgb(254, 243, 199))
            InvoiceStatus.OVERDUE -> Pair(Color.rgb(239, 68, 68), Color.rgb(254, 226, 226))
            InvoiceStatus.DRAFT -> Pair(Color.rgb(100, 116, 139), Color.rgb(241, 245, 249))
        }

        val badgeText = status.label.uppercase()
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val badgeTextWidth = paint.measureText(badgeText)
        val badgeBoxWidth = badgeTextWidth + 18f
        val badgeBoxLeft = pageWidth - 40f - badgeBoxWidth
        val badgeBoxTop = y - 12f
        val badgeBoxBottom = y + 4f

        paint.color = statusBg
        canvas.drawRoundRect(badgeBoxLeft, badgeBoxTop, badgeBoxLeft + badgeBoxWidth, badgeBoxBottom, 4f, 4f, paint)
        paint.color = statusColor
        canvas.drawText(badgeText, badgeBoxLeft + 9f, y - 1f, paint)

        y += 28f

        // Horizontal Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, paint)

        y += 24f

        // "BILL TO" Box
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILL TO", 40f, y, paint)

        y += 15f
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(client?.name ?: invoice.clientName, 40f, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.rgb(71, 85, 105)

        val company = client?.company?.ifBlank { invoice.clientCompany } ?: invoice.clientCompany
        if (company.isNotBlank()) {
            y += 14f
            canvas.drawText(company, 40f, y, paint)
        }
        if (!client?.email.isNullOrBlank()) {
            y += 14f
            canvas.drawText(client.email, 40f, y, paint)
        }
        if (!client?.address.isNullOrBlank()) {
            y += 14f
            canvas.drawText(client.address, 40f, y, paint)
        }

        y += 28f

        // Table Header
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(40f, y - 12f, (pageWidth - 40).toFloat(), y + 14f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val colDesc = 50f
        val colQty = 340f
        val colPrice = 410f
        val colTotal = 490f

        canvas.drawText("DESCRIPTION", colDesc, y + 2f, paint)
        canvas.drawText("QTY", colQty, y + 2f, paint)
        canvas.drawText("UNIT PRICE", colPrice, y + 2f, paint)
        canvas.drawText("TOTAL", colTotal, y + 2f, paint)

        y += 22f

        // Table Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        for ((index, item) in items.withIndex()) {
            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(40f, y - 10f, (pageWidth - 40).toFloat(), y + 10f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            // Truncate long descriptions if needed
            val desc = if (item.description.length > 50) item.description.take(47) + "..." else item.description
            canvas.drawText(desc, colDesc, y, paint)

            paint.color = Color.rgb(71, 85, 105)
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            canvas.drawText(qtyStr, colQty, y, paint)
            canvas.drawText(Formatters.formatCurrency(item.unitPrice, currency), colPrice, y, paint)

            paint.color = Color.rgb(15, 23, 42)
            canvas.drawText(Formatters.formatCurrency(item.lineTotal, currency), colTotal, y, paint)

            paint.color = Color.rgb(241, 245, 249)
            paint.strokeWidth = 0.5f
            canvas.drawLine(40f, y + 10f, (pageWidth - 40).toFloat(), y + 10f, paint)

            y += 22f
        }

        y += 15f

        // Totals Box (Right aligned)
        val totalsX = 350f
        paint.textSize = 9.5f
        paint.color = Color.rgb(100, 116, 139)

        fun drawTotalLine(label: String, amountStr: String, isBold: Boolean = false, isHighlight: Boolean = false) {
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = if (isHighlight) Color.rgb(15, 23, 42) else Color.rgb(100, 116, 139)
            canvas.drawText(label, totalsX, y, paint)

            paint.color = if (isHighlight) Color.rgb(15, 23, 42) else Color.rgb(51, 65, 85)
            val textWidth = paint.measureText(amountStr)
            canvas.drawText(amountStr, (pageWidth - 40 - textWidth), y, paint)
            y += 18f
        }

        drawTotalLine("Subtotal:", Formatters.formatCurrency(invoice.subtotal, currency))

        if (invoice.discountAmount > 0) {
            drawTotalLine("Discount (${invoice.discountRate}%):", "-${Formatters.formatCurrency(invoice.discountAmount, currency)}")
        }

        if (invoice.taxAmount > 0) {
            drawTotalLine("Tax (${invoice.taxRate}%):", Formatters.formatCurrency(invoice.taxAmount, currency))
        }

        paint.color = Color.rgb(203, 213, 225)
        paint.strokeWidth = 1f
        canvas.drawLine(totalsX, y - 8f, (pageWidth - 40).toFloat(), y - 8f, paint)

        // Total
        paint.textSize = 12f
        drawTotalLine("Total:", Formatters.formatCurrency(invoice.totalAmount, currency), isBold = true, isHighlight = true)

        if (invoice.amountPaid > 0) {
            paint.textSize = 9.5f
            drawTotalLine("Amount Paid:", Formatters.formatCurrency(invoice.amountPaid, currency))
            paint.textSize = 11f
            drawTotalLine("Balance Due:", Formatters.formatCurrency(invoice.remainingBalance, currency), isBold = true)
        }

        y += 20f

        // Payment Notes & Instructions (Bottom Left)
        val instructions = invoice.notes.ifBlank { userProfile?.paymentInstructions.orEmpty() }
        if (instructions.isNotBlank()) {
            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("PAYMENT INSTRUCTIONS & NOTES", 40f, y, paint)

            y += 14f
            paint.color = Color.rgb(100, 116, 139)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f

            // Split into lines
            val lines = instructions.chunked(75)
            for (line in lines) {
                canvas.drawText(line, 40f, y, paint)
                y += 12f
            }
        }

        // Footer Thank You
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val footerText = "Thank you for your business!"
        val footerWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (pageWidth - footerWidth) / 2f, (pageHeight - 35).toFloat(), paint)

        pdfDocument.finishPage(page)

        // Save PDF to cache dir
        val outputDir = File(context.cacheDir, "invoices")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "Invoice_${invoice.invoiceNumber}.pdf")
        val outputStream = FileOutputStream(outputFile)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return outputFile
    }
}
