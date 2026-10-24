package com.henipiter.receipt.dto

data class ReceiptExtras(
    val payerId: String,
    val beneficiaryId: String,
    val categoryId: String?
)