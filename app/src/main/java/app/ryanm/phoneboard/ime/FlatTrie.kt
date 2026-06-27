package app.ryanm.phoneboard.ime

import android.content.Context
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FlatTrie private constructor(
    val firstChild: IntArray,
    val childCount: IntArray,
    val wordOffset: IntArray,
    val frequency: IntArray,
    val edgeChar: IntArray,
    val edgeChild: IntArray,
    val wordBytes: ByteArray
){

    companion object {
        fun loadfromAssets(context: Context, path: String): FlatTrie {
            java.io.DataInputStream(
                context.assets.open("dicts/dict.trie").buffered()
            ).use { input ->
                val magic = ByteArray(4)
                input.readFully(magic)

                require(magic.contentEquals(byteArrayOf(
                    'P'.code.toByte(),
                    'B'.code.toByte(),
                    'T'.code.toByte(),
                    'R'.code.toByte()
                )))

                val version = input.readIntLE()
                val nodeCount = input.readIntLE()
                val edgeCount = input.readIntLE()
                val wordBytesLength = input.readIntLE()

                val firstChild = IntArray(nodeCount) { input.readIntLE() }
                val childCount = IntArray(nodeCount) { input.readIntLE() }
                val wordOffset = IntArray(nodeCount) { input.readIntLE() }
                val frequency = IntArray(nodeCount) { input.readIntLE() }

                val edgeChar = IntArray(edgeCount) { input.readIntLE() }
                val edgeChild = IntArray(edgeCount) { input.readIntLE() }
                val wordBytes = ByteArray(wordBytesLength)
                input.readFully(wordBytes)

                return FlatTrie(
                    firstChild,
                    childCount,
                    wordOffset,
                    frequency,
                    edgeChar,
                    edgeChild,
                    wordBytes
                )
            }
        }

        private fun DataInputStream.readIntLE(): Int {
            val b0 = readUnsignedByte()
            val b1 = readUnsignedByte()
            val b2 = readUnsignedByte()
            val b3 = readUnsignedByte()

            return b0 or
                    (b1 shl 8) or
                    (b2 shl 16) or
                    (b3 shl 24)
        }
    }
}