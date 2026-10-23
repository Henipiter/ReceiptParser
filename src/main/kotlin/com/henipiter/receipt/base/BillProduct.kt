package com.henipiter.receipt.base

import com.henipiter.receipt.dto.ChargeType

class BillProduct(
    var id: String = "",
    var productId: String = "",
    var receiptId: String = "",
    var payerId: String = "",
    var memberName: String = "",
    var finalAmount: Int = 0,
    var charge: Int = 0,
    var type: ChargeType = ChargeType.AMOUNT
)
