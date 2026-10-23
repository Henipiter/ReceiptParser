package com.henipiter.receipt.base

import com.henipiter.receipt.dto.ChargeType

interface BillProductBase {
    var id: String
    var productId: String
    var receiptId: String
    var payerId: String
    var memberName: String
    var finalAmount: Int
    var charge: Int
    var type: ChargeType
}