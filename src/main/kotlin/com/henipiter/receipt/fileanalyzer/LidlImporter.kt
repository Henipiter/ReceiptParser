package com.henipiter.receipt.fileanalyzer

import com.henipiter.receipt.fileanalyzer.parser.util.Stores


object LidlImporter : StoreImporter {
    override val store = Stores.LIDL
    override val startUrl = "https://www.lidl.pl/mre/purchase-history"
    override val loginHosts = setOf("accounts.lidl.com")

    override val allowedHosts = setOf("lidl.pl")

    override val scriptAsset = "fileanalyzer/lidl.js"

    override fun shouldInject(url: String) = url.contains("purchase-history")
}