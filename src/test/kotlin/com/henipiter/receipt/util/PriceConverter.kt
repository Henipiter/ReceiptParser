package com.henipiter.receipt.util

import kotlin.math.roundToInt

object PriceConverter {

    fun stringToPrice(string: String): Int {
        val double = string.toDouble() * 100
        return double.roundToInt()
    }

    fun stringToQuantity(string: String): Int {
        val double = string.toDouble() * 1000
        return double.roundToInt()
    }
}