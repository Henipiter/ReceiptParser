package com.henipiter.receipt.base

import com.henipiter.receipt.dto.TransactionType

class Receipt (
    var id: String = "",
    var storeId: String = "",
    var addressId: String = "",
    var payerId: String = "",
    var beneficiaryId: String = "",
    var online: Boolean = false,
    var pln: Int = 0,
    var ptu: Int = 0,
    var date: String = "",
    var time: String = "",
    var transactionType: TransactionType = TransactionType.RECEIPT
)
