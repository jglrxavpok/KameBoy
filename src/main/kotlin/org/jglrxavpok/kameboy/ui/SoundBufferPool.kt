package org.jglrxavpok.kameboy.ui

import java.util.concurrent.LinkedBlockingQueue

class SoundBufferPool {

    private val internalList = LinkedBlockingQueue<Int>()

    fun free(bufferID: Int) {
        internalList.put(bufferID)
    }

    /**
     * Returns null if empty pool
     */
    fun getOrNull(): Int? {
        if(internalList.isEmpty())
            return null
        return internalList.take()
    }
}