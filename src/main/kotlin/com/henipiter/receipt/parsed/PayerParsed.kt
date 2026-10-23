package com.henipiter.receipt.parsed

import com.henipiter.receipt.base.PayerBase

class PayerParsed(
    override var id: String = "",
    override var name: String = "",
    override var defaultBeneficiaryId: String = "",
    override var defaultPayer: Boolean = false
) : PayerBase
