package ui

import java.util.*

class LinesModel(blocks: Iterable<BlockModel>) {
    val lines = getLines(blocks)
    private val offsets: List<TextSpan> = getOffsets(lines)
    val count = lines.size

    fun offsetToLineNumber(textOffset: Int): Int {
        return TextSpan.findSpanStartingAtOrBefore(offsets, textOffset) ?: 0
    }

    fun lineNumberToOffset(lineNumber: Int): TextSpan {
        return offsets[lineNumber]
    }

    private fun getLines(blocks: Iterable<BlockModel>): List<String> {
        return blocks.flatMap {
            it.content
                    .plus(it.padding?.content ?: emptyList())
                    .map { line -> line + it.separator }
        }
    }

    private fun getOffsets(lines: Iterable<String>): ArrayList<TextSpan> {
        val result = arrayListOf<TextSpan>()
        var offset = 0
        for (line in lines) {
            result.add(TextSpan(offset, offset + line.length))
            offset += line.length
        }
        return result
    }
}