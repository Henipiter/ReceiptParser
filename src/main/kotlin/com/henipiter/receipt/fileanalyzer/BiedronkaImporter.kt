package com.henipiter.receipt.fileanalyzer

import com.henipiter.receipt.fileanalyzer.parser.util.Stores

object BiedronkaImporter : StoreImporter {
    override val store = Stores.BIEDRONKA
    override val startUrl = "https://moja.biedronka.pl/panel/paragons"

    override val allowedHosts = setOf("biedronka.pl")

    override val loginHosts = emptySet<String>()

    override val scriptAsset = "fileanalyzer/biedronka.js"

    override fun shouldInject(url: String) = url.contains("/panel/paragons")
}