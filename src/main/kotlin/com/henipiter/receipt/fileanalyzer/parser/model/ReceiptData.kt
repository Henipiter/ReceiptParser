package com.henipiter.receipt.fileanalyzer.parser.model

import com.henipiter.receipt.base.AddressBase
import com.henipiter.receipt.base.ProductBase
import com.henipiter.receipt.base.ReceiptBase
import com.henipiter.receipt.base.StoreBase

data class ReceiptData(
    var address: AddressBase,
    var store: StoreBase,
    var receipt: ReceiptBase,
    val products: List<ProductBase>
)