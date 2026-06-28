package app.ryanm.phoneboard.ime

import android.content.Context
import java.io.DataInputStream

data class Candidate(
    val word: String,
    val frequency: Int
)
class FlatTrie private constructor(
    val firstChild: IntArray,
    val childCount: IntArray,
    val wordOffset: IntArray,
    val frequency: IntArray,
    val edgeChar: IntArray,
    val edgeChild: IntArray,
    val wordBytes: ByteArray
){
    private fun findChild(node: Int, char: Char): Int {
        val start = firstChild[node]
        val end = start + childCount[node]

        for(i in start until end) {
            if(edgeChar[i] == char.code)
                return edgeChild[i]
        }

        return -1
    }

    private fun nodeForPrefix(prefix: String): Int {
        var node = 0

        for( ch in prefix) {
            node = findChild(node, ch)
            if(node == -1)
                return -1
        }

        return node
    }

    private fun readWord(node: Int): String {
        val start = wordOffset[node]
        var end = start

        while(wordBytes[end].toInt() != 0)
            end++

        return wordBytes.decodeToString(start, end)
    }

    private fun collectWords(startNode: Int): List<Candidate> {
        val words = mutableListOf<Candidate>()

        if(wordOffset[startNode] != -1) {
            words.add(Candidate(
                readWord(startNode),
                frequency[startNode]
            ))
        }

        val start = firstChild[startNode]
        val count = childCount[startNode]

        for (edgeIndex in start until start + count) {
            val childNode = edgeChild[edgeIndex]
            words.addAll(collectWords(childNode))
        }

        return words
    }

    private fun applyCasing(input: String, suggestion: String): String {
        return when {
            input.all { it.isUpperCase() } -> suggestion.uppercase()
            input.firstOrNull()?.isUpperCase() == true -> suggestion.replaceFirstChar { it.uppercase() }
            else -> suggestion
        }
    }

    fun suggest(prefix: String): List<String> {
        val node = nodeForPrefix(prefix.lowercase())
        if(node == -1)
            return emptyList()

        return collectWords(node)
            .sortedByDescending {it.frequency}
            .map {it.word}
            .take(2)
            .map {applyCasing(prefix, it)}
    }
    companion object {
        fun loadfromAssets(context: Context, path: String): FlatTrie {
            DataInputStream(
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