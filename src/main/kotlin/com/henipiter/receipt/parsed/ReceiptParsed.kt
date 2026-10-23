package com.henipiter.receipt.parsed

import com.henipiter.receipt.base.ReceiptBase
import com.henipiter.receipt.dto.TransactionType

class ReceiptParsed(
    override var id: String = "",
    override var storeId: String = "",
    override var addressId: String = "",
    override var payerId: String = "",
    override var beneficiaryId: String = "",
    override var online: Boolean = false,
    override var pln: Int = 0,
    override var ptu: Int = 0,
    override var date: String = "",
    override var time: String = "",
    override var transactionType: TransactionType = TransactionType.RECEIPT
) : ReceiptBase
