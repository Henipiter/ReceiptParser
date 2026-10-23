package com.henipiter.receipt.dto

enum class TransactionType {
    RECEIPT, EXPENSE;

    companion object {
        fun fromName(text: String): TransactionType {
            return when (text) {
                "RECEIPT" -> RECEIPT
                "EXPENSE" -> EXPENSE
                else -> EXPENSE
            }
        }
    }
}
