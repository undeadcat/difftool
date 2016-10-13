package ui

import java.awt.Color
import java.awt.EventQueue
import java.awt.Graphics2D
import java.awt.Rectangle
import javax.swing.JTextPane
import javax.swing.event.CaretListener
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Highlighter

class FileContentPane() {
    private var lineSelected: (Int) -> Unit = {}
    private var selectedHighlightTag: Any? = null
    private val textPane = JTextPane()
    val lazyTextPane = LazyScrollPane(textPane)
    private var selectionModel: SelectionModel = SelectionModel(LinesModel(emptyList()), emptyList())
    private val lineSelectionHandler = CaretListener({
        val lineNumber = selectionModel.lines.offsetToLineNumber(it.dot)
        EventQueue.invokeLater { selectByLineNumber(lineNumber) }
        lineSelected(lineNumber)
    })

    init {
        textPane.isEditable = false
        textPane.editorKit = NoWrapEditorKit()
        textPane.caret = DefaultCaret().apply { this.updatePolicy = DefaultCaret.NEVER_UPDATE }
        textPane.caret.isVisible = true
        textPane.caret.isSelectionVisible = true
    }

    fun setContent(text: Iterable<BlockModel>) {
        val linesModel = LinesModel(text)
        selectionModel = SelectionModel(linesModel, text)
        lazyTextPane.reset(linesModel)
        textPane.removeCaretListener { lineSelectionHandler }

        fun highlightBlock(offset: Int, block: BlockModel, getHighlighter: (BlockType) -> Highlighter.HighlightPainter?): Int {
            var resultOffset = offset
            val highlighter = getHighlighter(block.type)
            for (line in block.content) {
                val contentLength = line.length + block.separator.length
                if (highlighter != null)
                    lazyTextPane.enqueueHighlight(resultOffset, resultOffset + contentLength, highlighter)
                resultOffset += contentLength
            }
            return resultOffset
        }

        fun highlightLineBlock(offset: Int, block: BlockModel)
                = highlightBlock(offset, block, { getLineHighlighter(it) })

        fun highlightWordBlock(offset: Int, block: BlockModel)
                = highlightBlock(offset, block, { getWordHighlighter(it) })

        var offset = 0
        for (block in text) {
            val blockStartOffset = offset
            if (block.children != null) {
                for (child in block.children)
                    offset = highlightWordBlock(offset, child)
            }
            offset = highlightLineBlock(blockStartOffset, block)
            if (block.padding != null)
                offset = highlightLineBlock(offset, block.padding)
        }
        lazyTextPane.ensureCurrentPageLoaded()
        textPane.addCaretListener(lineSelectionHandler)
    }

    fun setLineSelectedListener(listener: (Int) -> Unit) {
        this.lineSelected = listener
    }

    fun selectNextChange() {
        selectSpan(selectionModel.selectNextChange())
    }

    fun selectPreviousChange() {
        selectSpan(selectionModel.selectPreviousChange())
    }

    fun selectByLineNumber(lineNumber: Int) {
        selectionModel.selectByLineNumber(lineNumber)
        selectSpan(selectionModel.lines.lineNumberToOffset(lineNumber))
    }

    private fun selectSpan(span: TextSpan?) {
        if (span == null)
            return
        val scrollModel = lazyTextPane.verticalScrollModel
        val lineNumber = selectionModel.lines.offsetToLineNumber(span.start)
        lazyTextPane.ensureLoaded(span)
        if (lineNumber >= scrollModel.value + scrollModel.extent
                || lineNumber <= scrollModel.value)
            scrollToLineNumber(lineNumber)
        highlightSelectedSpan(span)
    }

    private fun highlightSelectedSpan(span: TextSpan) {
        if (this.selectedHighlightTag != null)
            textPane.highlighter.removeHighlight(this.selectedHighlightTag)
        this.selectedHighlightTag = textPane.highlighter.addHighlight(span.start, span.end, selectedHighlighter)

        textPane.repaint()
    }

    private fun scrollToLineNumber(lineNumber: Int) {
        val scrollbar = lazyTextPane.verticalScrollModel
        val newValue = Math.max(0, lineNumber - scrollbar.extent / 2)
        scrollbar.value = newValue
    }

    private fun getLineBorderHighlighter(color: Color): Highlighter.HighlightPainter {
        return getHighlighter(color, Graphics2D::draw)
    }

    private fun getLineFillHighlighter(color: Color): Highlighter.HighlightPainter {
        return getHighlighter(color, Graphics2D::fill)
    }

    private fun getHighlighter(color: Color, painter: (Graphics2D, Rectangle) -> Unit): Highlighter.HighlightPainter {
        return Highlighter.HighlightPainter(fun(g, p1, p2, @Suppress("UNUSED_PARAMETER") unused, textComponent) {
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
}

