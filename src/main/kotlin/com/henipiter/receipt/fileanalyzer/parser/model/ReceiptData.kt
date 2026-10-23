package com.henipiter.receipt.fileanalyzer.parser.model

import com.henipiter.receipt.base.Address
import com.henipiter.receipt.base.Product
import com.henipiter.receipt.base.Receipt
import com.henipiter.receipt.base.Store

data class ReceiptData(
    var address: Address,
    var store: Store,
    var receipt: Receipt,
    val products: List<Product>
)