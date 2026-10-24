package com.henipiter.receipt.util

import java.io.File
import java.net.URL

object ResourceFileLoader {

    fun getResource(relativePath: String): URL =
        Thread.currentThread().contextClassLoader.getResource(relativePath)
            ?: error("Nie znaleziono zasobu: $relativePath")

    fun readTextUtf8(relativePath: String): String =
        File(getResource(relativePath).toURI()).readText(Charsets.UTF_8)

    fun readTextAnsi(relativePath: String): String =
        File(getResource(relativePath).toURI()).readText(charset("windows-1250"))

}