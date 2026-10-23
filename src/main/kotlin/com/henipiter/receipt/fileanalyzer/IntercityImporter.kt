package com.henipiter.receipt.fileanalyzer

import com.henipiter.receipt.fileanalyzer.parser.util.Stores

object IntercityImporter : StoreImporter {
    override val store = Stores.INTERCITY
    override val startUrl = "https://ebilet.intercity.pl/profil/historiapodrozy"

    override val allowedHosts = setOf("ebilet.intercity.pl")
    override val loginHosts = setOf("sso.intercity.pl")

    override val scriptAsset = "fileanalyzer/intercity.js"

    override fun shouldInject(url: String) = true
}