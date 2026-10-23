package com.henipiter.receipt.dto

enum class ChargeType {
    AMOUNT, PART, PERCENT;

    companion object {
        fun fromName(text: String): ChargeType {
            return when (text) {
                "PART" -> PART
                "PERCENT" -> PERCENT
                else -> AMOUNT
            }
        }
    }
}
