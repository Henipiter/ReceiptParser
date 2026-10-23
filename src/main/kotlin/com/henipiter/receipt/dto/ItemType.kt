package com.henipiter.receipt.dto

enum class ItemType {
    PRODUCT, DISCOUNT;

    companion object {
        fun fromName(text: String): ItemType {
            return when (text) {
                "PRODUCT" -> PRODUCT
                "DISCOUNT" -> DISCOUNT
                else -> PRODUCT
            }
        }
    }

    fun toggle(): ItemType {
        return when (this) {
            DISCOUNT -> PRODUCT
            PRODUCT -> DISCOUNT
        }
    }
}
