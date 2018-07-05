package org.jglrxavpok.kameboy.helpful

fun Int.toHexString() = Integer.toHexString(this)

fun Int.toHexStringWithPadding(charCount: Int): String {
    var hex = toHexString().toUpperCase()
    while(hex.length < charCount)
        hex = "0$hex"
    return hex
}