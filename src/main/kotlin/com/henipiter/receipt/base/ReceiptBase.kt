package com.henipiter.receipt.base

import com.henipiter.receipt.dto.TransactionType

interface ReceiptBase {
    var id: String
    var storeId: String
    var addressId: String
    var payerId: String
    var beneficiaryId: String
    var online: Boolean
    var pln: Int
    var ptu: Int
    var date: String
    var time: String
    var transactionType: TransactionType
}
