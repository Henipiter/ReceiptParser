package com.henipiter.receipt.parsed

import com.henipiter.receipt.base.BillProductBase
import com.henipiter.receipt.dto.ChargeType

class BillProductParsed(
    override var id: String = "",
    override var productId: String = "",
    override var receiptId: String = "",
    override var payerId: String = "",
    override var memberName: String = "",
    override var finalAmount: Int = 0,
    override var charge: Int = 0,
    override var type: ChargeType = ChargeType.AMOUNT
) : BillProductBase
