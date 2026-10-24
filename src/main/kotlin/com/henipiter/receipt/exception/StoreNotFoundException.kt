package com.henipiter.receipt.exception

import com.henipiter.receipt.fileanalyzer.parser.util.Stores

class StoreNotFoundException : Exception {
    val store: Stores?
    val nip: String

    constructor(store: Stores) : super("Store not found: ${store.storeName} (NIP ${store.nip})") {
        this.store = store
        this.nip = store.nip
    }

    constructor(nip: String) : super("Store not found: NIP $nip") {
        this.store = null
        this.nip = nip
    }
}
