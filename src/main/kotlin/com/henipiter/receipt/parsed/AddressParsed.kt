package com.henipiter.receipt.parsed

import com.henipiter.receipt.base.AddressBase

data class AddressParsed(
    override var id: String = "",
    override var storeId: String = "",
    override var city: String = "",
    override var postalCode: String = "",
    override var streetName: String = "",
    override var autoSelect: Boolean = false,
    override var favourite: Boolean = false
) : AddressBase
