package ui

class SelectionModel(val lines: LinesModel, blocks: Iterable<BlockModel>) {
    private val changeOffsets: List<TextSpan> = buildChangeOffsets(blocks)
    private var selectedSpan: TextSpan? = null

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
        val selectedSpan = lines.lineNumberToOffset(lineNumber)
        this.selectedSpan = selectedSpan
        return selectedSpan
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
        val changeIndex = TextSpan.findSpanStartingAtOrBefore(changeOffsets, selectedSpan.start)
        if (changeIndex == null)
            return null
        val changeAtIndex = changeOffsets[changeIndex]
        if (TextSpan.contains(changeAtIndex, selectedSpan))
            return changeIndex
        if (direction == Direction.Next)
            return changeIndex
        return changeIndex + 1
    }


    private enum class Direction {
        Next,
        Previous
    }
}