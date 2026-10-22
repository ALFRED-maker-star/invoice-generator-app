package com.example.util

import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceStatus
import com.example.data.model.Payment
import com.example.data.model.UserProfile
import java.util.concurrent.TimeUnit

object SampleData {
    val defaultProfile = UserProfile(
        id = 1,
        businessName = "Apex Studio & Design",
        ownerName = "Alex Morgan",
        email = "alex@apexstudio.design",
        phone = "+1 (555) 382-9012",
        address = "742 Evergreen Terrace, Suite 300, Austin, TX 78701",
        currency = "$",
        taxId = "US-EIN 84-9214730",
        paymentInstructions = "Direct Wire: Bank of Austin, Routing: 111000025, Acct: 9876543210. Or Stripe / PayPal: pay@apexstudio.design",
        isLoggedIn = true
    )

    fun getSampleClients(): List<Client> {
        val now = System.currentTimeMillis()
        return listOf(
            Client(
                id = 1L,
                name = "Sarah Jenkins",
                company = "Lumina Tech Innovations",
                email = "sarah.j@luminatech.io",
                phone = "+1 (415) 890-1234",
                address = "100 Market St, Suite 500, San Francisco, CA 94105",
                notes = "Prefers bi-weekly billing. Key contact for design projects.",
                createdAt = now - TimeUnit.DAYS.toMillis(45)
            ),
            Client(
                id = 2L,
                name = "Marcus Vance",
                company = "Horizon Media Agency",
                email = "marcus@horizonmedia.com",
                phone = "+1 (212) 555-7890",
                address = "450 Lexington Ave, 18th Floor, New York, NY 10017",
                notes = "Net 15 payment terms agreed upon.",
                createdAt = now - TimeUnit.DAYS.toMillis(30)
            ),
            Client(
                id = 3L,
                name = "Elena Rostova",
                company = "Nordic Commerce Ltd",
                email = "elena@nordiccommerce.eu",
                phone = "+44 20 7946 0912",
                address = "25 Old Broad St, London EC2N 1HN, United Kingdom",
                notes = "Cross-border client; requires VAT ID referenced on all documents.",
                createdAt = now - TimeUnit.DAYS.toMillis(60)
            )
        )
    }

    data class SampleInvoiceBundle(
        val invoice: Invoice,
        val items: List<InvoiceItem>,
        val payments: List<Payment>
    )

    fun getSampleInvoiceBundles(): List<SampleInvoiceBundle> {
        val now = System.currentTimeMillis()

        // 1. Paid Invoice
        val inv1 = Invoice(
            id = 1L,
            invoiceNumber = "INV-2026-001",
            clientId = 1L,
            clientName = "Sarah Jenkins",
            clientCompany = "Lumina Tech Innovations",
            issueDate = now - TimeUnit.DAYS.toMillis(25),
            dueDate = now - TimeUnit.DAYS.toMillis(10),
            status = InvoiceStatus.PAID.name,
            subtotal = 5400.0,
            taxRate = 8.25,
            taxAmount = 445.50,
            discountRate = 0.0,
            discountAmount = 0.0,
            totalAmount = 5845.50,
            amountPaid = 5845.50,
            notes = "Thank you for partnering with Apex Studio! All deliverables deployed to production.",
            createdAt = now - TimeUnit.DAYS.toMillis(25)
        )
        val items1 = listOf(
            InvoiceItem(invoiceId = 1L, description = "Brand Identity Design & Design Guidelines", quantity = 1.0, unitPrice = 2400.0, lineTotal = 2400.0),
            InvoiceItem(invoiceId = 1L, description = "Mobile App UI/UX Wireframes & Prototypes (40 hrs)", quantity = 40.0, unitPrice = 75.0, lineTotal = 3000.0)
        )
        val payments1 = listOf(
            Payment(invoiceId = 1L, invoiceNumber = "INV-2026-001", amount = 5845.50, paymentDate = now - TimeUnit.DAYS.toMillis(12), paymentMethod = "Bank Transfer", referenceNotes = "Full wire received Ref #WT-94821")
        )

        // 2. Pending Invoice (Partially Paid)
        val inv2 = Invoice(
            id = 2L,
            invoiceNumber = "INV-2026-002",
            clientId = 2L,
            clientName = "Marcus Vance",
            clientCompany = "Horizon Media Agency",
            issueDate = now - TimeUnit.DAYS.toMillis(8),
            dueDate = now + TimeUnit.DAYS.toMillis(7),
            status = InvoiceStatus.PENDING.name,
            subtotal = 4700.0,
            taxRate = 5.0,
            taxAmount = 235.0,
            discountRate = 0.0,
            discountAmount = 0.0,
            totalAmount = 4935.0,
            amountPaid = 2000.0,
            notes = "Net 15 terms. First installment paid upon sprint kickoff.",
            createdAt = now - TimeUnit.DAYS.toMillis(8)
        )
        val items2 = listOf(
            InvoiceItem(invoiceId = 2L, description = "Full-Stack Web App Sprint 1 Development", quantity = 1.0, unitPrice = 3500.0, lineTotal = 3500.0),
            InvoiceItem(invoiceId = 2L, description = "API Integrations & Payment Gateway Setup", quantity = 1.0, unitPrice = 1200.0, lineTotal = 1200.0)
        )
        val payments2 = listOf(
            Payment(invoiceId = 2L, invoiceNumber = "INV-2026-002", amount = 2000.0, paymentDate = now - TimeUnit.DAYS.toMillis(6), paymentMethod = "Credit Card", referenceNotes = "Initial deposit via Stripe")
        )

        // 3. Overdue Invoice
        val inv3 = Invoice(
            id = 3L,
            invoiceNumber = "INV-2026-003",
            clientId = 3L,
            clientName = "Elena Rostova",
            clientCompany = "Nordic Commerce Ltd",
            issueDate = now - TimeUnit.DAYS.toMillis(28),
            dueDate = now - TimeUnit.DAYS.toMillis(14),
            status = InvoiceStatus.OVERDUE.name,
            subtotal = 2750.0,
            taxRate = 0.0,
            taxAmount = 0.0,
            discountRate = 0.0,
            discountAmount = 0.0,
            totalAmount = 2750.0,
            amountPaid = 0.0,
            notes = "Please submit payment to avoid late fees. European VAT reverse charged.",
            createdAt = now - TimeUnit.DAYS.toMillis(28)
        )
        val items3 = listOf(
            InvoiceItem(invoiceId = 3L, description = "E-Commerce Checkout Funnel Optimization", quantity = 1.0, unitPrice = 1800.0, lineTotal = 1800.0),
            InvoiceItem(invoiceId = 3L, description = "Core Web Vitals Performance Audit", quantity = 1.0, unitPrice = 950.0, lineTotal = 950.0)
        )

        // 4. Draft Invoice
        val inv4 = Invoice(
            id = 4L,
            invoiceNumber = "INV-2026-004",
            clientId = 1L,
            clientName = "Sarah Jenkins",
            clientCompany = "Lumina Tech Innovations",
            issueDate = now,
            dueDate = now + TimeUnit.DAYS.toMillis(30),
            status = InvoiceStatus.DRAFT.name,
            subtotal = 1600.0,
            taxRate = 8.25,
            taxAmount = 132.0,
            discountRate = 5.0,
            discountAmount = 80.0,
            totalAmount = 1652.0,
            amountPaid = 0.0,
            notes = "Draft invoice for upcoming Q4 design retainer.",
            createdAt = now
        )
        val items4 = listOf(
            InvoiceItem(invoiceId = 4L, description = "Design System Maintenance & Asset Delivery (20 hrs)", quantity = 20.0, unitPrice = 80.0, lineTotal = 1600.0)
        )

        return listOf(
            SampleInvoiceBundle(inv1, items1, payments1),
            SampleInvoiceBundle(inv2, items2, payments2),
            SampleInvoiceBundle(inv3, items3, emptyList()),
            SampleInvoiceBundle(inv4, items4, emptyList())
        )
    }
}
