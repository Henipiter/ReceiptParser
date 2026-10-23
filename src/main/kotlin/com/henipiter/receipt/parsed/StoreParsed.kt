package com.henipiter.receipt.parsed

import com.henipiter.receipt.base.StoreBase
import com.henipiter.receipt.dto.ReceiverType

class StoreParsed(
    override var id: String = "",
    override var nip: String = "",
    override var name: String = "",
    override var receiverType: ReceiverType = ReceiverType.STORE,
    override var addressId: String = "",
    override var defaultCategoryId: String = "",
) : StoreBase
