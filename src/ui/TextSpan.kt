package ui

data class TextSpan(val start: Int, val end: Int) {

    companion object {
        fun findSpanStartingAtOrBefore(list: List<TextSpan>, offset: Int): Int? {
            val index = list.binarySearchBy(offset, selector = { it.start })
            if (index >= 0)
                return index
            val insertionPoint = -(index + 1)
            if (insertionPoint == 0)
                return null
            return insertionPoint - 1
        }

        fun contains(parent: TextSpan, child: TextSpan): Boolean {
            return parent.start <= child.start && parent.end >= child.end
        }
    }
}