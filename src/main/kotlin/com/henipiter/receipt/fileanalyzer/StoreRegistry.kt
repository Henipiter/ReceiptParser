package com.henipiter.receipt.fileanalyzer

import com.henipiter.receipt.fileanalyzer.parser.util.Stores

object StoreRegistry {
    private val stores: List<StoreImporter> = listOf(
        LidlImporter,
        BiedronkaImporter,
        IntercityImporter
    )

    fun all(): List<StoreImporter> = stores

    fun get(store: Stores): StoreImporter =
        stores.first { it.store == store }
}