package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class InvoiceWithDetails(
    @Embedded val invoice: Invoice,
    @Relation(
        parentColumn = "clientId",
        entityColumn = "id"
    )
    val client: Client?,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItem> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val payments: List<Payment> = emptyList()
)
