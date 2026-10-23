package com.henipiter.receipt.parsed

import com.henipiter.receipt.base.ProductTagBase

class ProductTagParsed(
    override var id: String = "",
    override var productId: String = "",
    override var tagId: String = "",
) : ProductTagBase
