package com.henipiter.receipt.base

import com.henipiter.receipt.dto.ReceiverType

class Store(
    var id: String = "",
    var nip: String = "",
    var name: String = "",
    var receiverType: ReceiverType = ReceiverType.STORE,
    var addressId: String = "",
    var defaultCategoryId: String = "",
)
