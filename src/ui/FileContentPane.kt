package ui

import java.awt.Color
import java.awt.EventQueue
import java.awt.Graphics2D
import java.awt.Rectangle
import javax.swing.DefaultBoundedRangeModel
import javax.swing.event.CaretListener
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Highlighter

class FileContentPane() {
    private var lineSelected: (Int) -> Unit = {}
    private var selectedHighlightTag: Any? = null
    val lazyTextPane = LazyTextPane()
    private var selectionModel: SelectionModel = SelectionModel(emptyList())
    private val lineSelectionHandler = CaretListener({
        val lineNumber = selectionModel.getLineNumberByOffset(it.dot)
        val span = selectionModel.selectByLineNumber(lineNumber)
        highlightSelectedSpan(span)
        lineSelected(lineNumber)
    })

    fun setContent(text: Iterable<BlockModel>) {
        lazyTextPane.clear()
        lazyTextPane.setScrollModel(createScrollbarModel(text))
        selectionModel = SelectionModel(text)
        EventQueue.invokeLater { lazyTextPane.textPane.removeCaretListener { lineSelectionHandler } }

        fun appendBlock(offset: Int, block: BlockModel, getHighlighter: (BlockType) -> Highlighter.HighlightPainter?): Int {
            var reusultOffset = offset
            val highlighter = getHighlighter(block.type)
            for (line in block.content) {
                val actualContent = line + block.separator
                lazyTextPane.appendLine(actualContent)
                if (highlighter != null)
                    lazyTextPane.enqueueHighlight(reusultOffset, reusultOffset + actualContent.length, highlighter)
                reusultOffset += actualContent.length
            }
            return reusultOffset
        }

        fun appendLineBlock(offset: Int, block: BlockModel): Int {
            return appendBlock(offset, block, { getLineHighlighter(it) })
        }

        var offset = 0
        for (block in text) {
            val blockStartOffset = offset
            if (block.children != null) {
                var wordsStartOffset = offset
                offset = appendLineBlock(offset, block)
                for (child in block.children) {
                    val highlighter = getWordHighlighter(child.type)
                    if (highlighter != null)
                        lazyTextPane.enqueueHighlight(wordsStartOffset, wordsStartOffset + child.getContentString().length, highlighter)
                    wordsStartOffset += child.getContentString().length
                }
                val highlighter = getLineHighlighter(block.type)
                if (highlighter != null)
                    lazyTextPane.enqueueHighlight(blockStartOffset, offset, highlighter)
            } else
                offset = appendLineBlock(offset, block)
            if (block.padding != null)
                offset = appendLineBlock(offset, block.padding)
        }
        EventQueue.invokeLater {
            lazyTextPane.ensureCurrentPageLoaded()
            lazyTextPane.textPane.addCaretListener(lineSelectionHandler)
        }
    }

    fun setLineSelectedListener(listener: (Int) -> Unit) {
        this.lineSelected = listener
    }

    fun selectNextChange() {
        selectChange(selectionModel.selectNextChange())
    }

    fun selectPreviousChange() {
        selectChange(selectionModel.selectPreviousChange())
    }

    fun selectByLineNumber(lineNumber: Int) {
        highlightSelectedSpan(selectionModel.selectByLineNumber(lineNumber))
    }

    private fun selectChange(span: TextSpan?) {
        if (span == null)
            return
        lazyTextPane.ensureLoadedTo(span.end)
        //nasty hacks. we need scroll to happen _ after_
        //recalculations that are enqueued onto the ui thread by updating the document
        EventQueue.invokeLater {
            scrollToOffset(span.start)
            highlightSelectedSpan(span)
        }
    }

    private fun highlightSelectedSpan(span: TextSpan) {
        if (this.selectedHighlightTag != null)
            this.lazyTextPane.textPane.highlighter.removeHighlight(this.selectedHighlightTag)
        this.selectedHighlightTag = lazyTextPane.textPane.highlighter.addHighlight(span.start, span.end, selectedHighlighter)

        this.lazyTextPane.textPane.repaint()
    }

    private fun scrollToOffset(offset: Int) {
        val targetY = lazyTextPane.textPane.modelToView(offset).y
        val newY = targetY - lazyTextPane.textPane.visibleRect.height / 2
        lazyTextPane.scrollPane.verticalScrollBar.value = if (newY < 0) 0 else newY
    }

    private fun getLineBorderHighlighter(color: Color): Highlighter.HighlightPainter {
        return getHighlighter(color, Graphics2D::draw)
    }

    private fun getLineFillHighlighter(color: Color): Highlighter.HighlightPainter {
        return getHighlighter(color, Graphics2D::fill)
    }

    private fun getHighlighter(color: Color, painter: (Graphics2D, Rectangle) -> Unit): Highlighter.HighlightPainter {
        return Highlighter.HighlightPainter(fun(g, p1, p2, @Suppress("UNUSED_PARAMETER") unused, textComponent) {
            if (p2 > textComponent.document.length)
                return
            g.color = color
            val startPosition = textComponent.modelToView(p1)
            val endPosition = textComponent.modelToView(p2)
            val rect = Rectangle(startPosition.x, startPosition.y, textComponent.width, endPosition.y - startPosition.y)
            painter(g as Graphics2D, rect)

        })
    }

    private val lineAddedHighlighter = getLineFillHighlighter(Color(234, 255, 234))
    private val lineDeletedHighlighter = getLineFillHighlighter(Color(255, 234, 234))
    private val paddingHighlighter = getLineFillHighlighter(Color(240, 240, 240))
    private val contextExcludedHighlighter = getLineFillHighlighter(Color(243, 246, 250))
    private val wordAddedHighlighter = DefaultHighlighter.DefaultHighlightPainter(Color(170, 255, 170))
    private val wordDeletedHighlighter = DefaultHighlighter.DefaultHighlightPainter(Color(255, 170, 170))
    private val selectedHighlighter = getLineBorderHighlighter(Color(128, 128, 128))

    private fun getWordHighlighter(type: BlockType): DefaultHighlighter.DefaultHighlightPainter? {
        return when (type) {
            BlockType.Deleted -> wordDeletedHighlighter
            BlockType.Added -> wordAddedHighlighter
            else -> null
        }
    }

    private fun getLineHighlighter(type: BlockType): Highlighter.HighlightPainter? {
        when (type) {

            BlockType.Padding -> return paddingHighlighter
            BlockType.Added -> return lineAddedHighlighter
            BlockType.Deleted -> return lineDeletedHighlighter
            BlockType.ContextExcluded -> return contextExcludedHighlighter
            else -> return null
        }
    }

    private fun createScrollbarModel(blocks: Iterable<BlockModel>): DefaultBoundedRangeModel {
        val lineCount = blocks
                .flatMap { it.content.plus(it.padding?.content ?: emptyList()) }.count()
        return javax.swing.DefaultBoundedRangeModel(0, lazyTextPane.textPane.visibleRect.height, 0,
                lineCount * lazyTextPane.textPane.font.size)
    }
}

data class TextSpan(val start: Int, val end: Int) {

}

