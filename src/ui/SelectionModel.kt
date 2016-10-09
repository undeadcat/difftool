package ui

import java.util.*

class SelectionModel(blocks: Iterable<BlockModel>) {
    private val lineOffsets: List<TextSpan> = buildLineOffsets(blocks)
    private val changeOffsets: List<TextSpan> = buildChangeOffsets(blocks)
    private var selectedSpan: TextSpan? = null

    private fun buildLineOffsets(blocks: Iterable<BlockModel>): ArrayList<TextSpan> {
        val result = arrayListOf<TextSpan>()
        var offset = 0
        for (line in blocks.flatMap { it.content.plus(it.padding?.content ?: emptyList()) }) {
            result.add(TextSpan(offset, offset + line.length + 1))
            offset += line.length + 1
        }
        return result
    }

    private fun buildChangeOffsets(blocks: Iterable<BlockModel>): List<TextSpan> {
        val result = arrayListOf<TextSpan>()
        var offset = 0
        for (block in blocks) {
            val contentString = block.getContentString() + (block.padding?.getContentString() ?: "")
            if (block.type == BlockType.Added || block.type == BlockType.Deleted)
                result.add(TextSpan(offset, offset + contentString.length))
            offset += contentString.length
        }
        return result
    }

    fun selectNextChange(): TextSpan? {
        return updateSelection(Direction.Next)
    }

    fun selectPreviousChange(): TextSpan? {
        return updateSelection(Direction.Previous)
    }

    fun selectByLineNumber(lineNumber: Int): TextSpan {
        val selectedSpan = lineOffsets[lineNumber]
        this.selectedSpan = selectedSpan
        return selectedSpan
    }

    fun getLineNumberByOffset(textOffset: Int): Int {
        return findSpanStartingAtOrBeforeOrNull(lineOffsets, textOffset) ?: 0
    }

    private fun updateSelection(direction: Direction): TextSpan? {
        val selectedChangeIndex = getSelectedChangeIndexOrNull(selectedSpan, direction)

        val newIndex =
                if (selectedChangeIndex == null)
                    if (direction == Direction.Next) 0 else null
                else {
                    if (direction == Direction.Next) selectedChangeIndex + 1
                    else selectedChangeIndex - 1
                }
        if (newIndex == null)
            return null
        if (newIndex >= 0 && newIndex < changeOffsets.size) {
            selectedSpan = changeOffsets[newIndex]
            return changeOffsets[newIndex]
        }
        return null
    }

    private fun getSelectedChangeIndexOrNull(selectedSpan: TextSpan?, direction: Direction): Int? {
        if (selectedSpan == null)
            return null
        val changeIndex = findSpanStartingAtOrBeforeOrNull(changeOffsets, selectedSpan.start)
        if (changeIndex == null)
            return null
        val changeAtIndex = changeOffsets[changeIndex]
        if (contains(changeAtIndex, selectedSpan))
            return changeIndex
        if (direction == Direction.Next)
            return changeIndex
        return changeIndex + 1
    }

    private fun findSpanStartingAtOrBeforeOrNull(list: List<TextSpan>, offset: Int): Int? {
        val index = list.binarySearchBy(offset, selector = { it.start })
        if (index >= 0)
            return index
        val insertionPoint = -(index + 1)
        if (insertionPoint == 0)
            return null
        return insertionPoint - 1
    }

    private fun contains(parent: TextSpan, child: TextSpan): Boolean {
        return parent.start <= child.start && parent.end >= child.end
    }

    private enum class Direction {
        Next,
        Previous
    }
}