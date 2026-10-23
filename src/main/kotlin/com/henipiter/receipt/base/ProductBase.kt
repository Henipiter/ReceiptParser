package com.henipiter.receipt.base

import com.henipiter.receipt.dto.ItemType

interface ProductBase {
    var id: String
    var receiptId: String
    var name: String
    var categoryId: String
    var beneficiaryId: String
    var quantity: Int
    var unitPrice: Int
    var subtotalPrice: Int
    var discount: Int
    var finalPrice: Int
    var ptuType: String
    var raw: String
    var validPrice: Boolean
    var itemType: ItemType
}