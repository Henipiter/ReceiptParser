package com.henipiter.receipt.dto

enum class ReceiverType {
    STORE, RECIPIENT;

    companion object {
        fun fromName(text: String): ReceiverType {
            return when (text) {
                "RECIPIENT" -> RECIPIENT
                "STORE" -> STORE
                else -> STORE
            }
        }
    }
}
