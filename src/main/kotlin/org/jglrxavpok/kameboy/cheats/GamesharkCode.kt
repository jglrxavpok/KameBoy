package org.jglrxavpok.kameboy.cheats

import org.jglrxavpok.kameboy.helpful.asAddress
import org.jglrxavpok.kameboy.helpful.asUnsigned8
import org.jglrxavpok.kameboy.helpful.toHexStringWithPadding

data class GamesharkCode(val code: String) {
    val isValid: Boolean
    /* format: ABCDEFGH
      AB    External RAM bank number
      CD    New Data
      GHEF  Memory Address (internal or external RAM, A000-DFFF)
     */
    // ex: 01XXFFD1 changes your overflow character sprite in Pkmn Gold (XX must be a multiple of 0x0C to look correct)
    val externalRamBankNumber: Int
    val newData: Int
    val memoryAddress: Int
    var isActive = true
    init {
        isValid = checkValidity()
        if(isValid) {
            externalRamBankNumber = code.take(2).toInt(16)
            newData = code.drop(2).take(2).toInt(16).asUnsigned8()
            val memoryAddressLow = code.drop(4).take(2).toInt(16)
            val memoryAddressHigh = code.drop(6).take(2).toInt(16)
            memoryAddress = memoryAddressLow or (memoryAddressHigh shl 8)
        } else {
            externalRamBankNumber = 0
            newData = 0
            memoryAddress = 0
        }
    }

    private fun checkValidity(): Boolean {
        if(code.length != 8)
            return false
        val validHexa = code.chars().allMatch {
            it.toChar() in '0'..'9' || it.toChar() in 'A'..'F' || it.toChar() in 'a'..'f'
        }
        return validHexa
    }

    override fun toString(): String {
        return code
    }

    fun withData(newData: Int): GamesharkCode {
        val dataPart = newData.asUnsigned8().toHexStringWithPadding(2)
        val newCode = code.take(2) + dataPart + code.drop(4)
        return GamesharkCode(newCode)
    }

    fun withAddress(newAddress: Int): GamesharkCode {
        val dataPart = newAddress.asAddress().toHexStringWithPadding(4)
        val low = dataPart.drop(2).take(2)
        val high = dataPart.take(2)
        val newCode = code.take(4) + low + high
        return GamesharkCode(newCode)
    }
}