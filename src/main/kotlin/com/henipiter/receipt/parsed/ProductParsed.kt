package com.henipiter.receipt.parsed

import com.henipiter.receipt.base.ProductBase
import com.henipiter.receipt.dto.ItemType

class ProductParsed(
    override var id: String = "",
    override var receiptId: String = "",
    override var name: String = "",
    override var categoryId: String = "",
    override var beneficiaryId: String = "",
    override var quantity: Int = 0,
    override var unitPrice: Int = 0,
    override var subtotalPrice: Int = 0,
    override var discount: Int = 0,
    override var finalPrice: Int = 0,
    override var ptuType: String = "",
    override var raw: String = "",
    override var validPrice: Boolean = false,
    override var itemType: ItemType = ItemType.PRODUCT
) : ProductBase
