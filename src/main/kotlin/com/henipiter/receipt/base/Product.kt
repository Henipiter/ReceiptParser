package com.henipiter.receipt.base

import com.henipiter.receipt.dto.ItemType

class Product(
    var id: String = "",
    var receiptId: String = "",
    var name: String = "",
    var categoryId: String = "",
    var beneficiaryId: String = "",
    var quantity: Int = 0,
    var unitPrice: Int = 0,
    var subtotalPrice: Int = 0,
    var discount: Int = 0,
    var finalPrice: Int = 0,
    var ptuType: String = "",
    var raw: String = "",
    var validPrice: Boolean = false,
    var itemType: ItemType = ItemType.PRODUCT
)
