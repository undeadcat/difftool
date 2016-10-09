package ui

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.event.CaretListener
import javax.swing.text.*

class FileContentPane() {
    private var lineSelected: (Int) -> Unit = {}
    private var selectedHighlightTag: Any? = null
    private val textPane = JTextPane()
    val scrollPane = JScrollPane(textPane)
    private var selectionModel: SelectionModel = SelectionModel(emptyList())
    private val lineSelectionHandler = CaretListener({
        val lineNumber = selectionModel.getLineNumberByOffset(it.dot)
        val span = selectionModel.selectByLineNumber(lineNumber)
        highlightSelectedSpan(span)
        lineSelected(lineNumber)
    })

    init {
        textPane.isEditable = false
        textPane.caret = DefaultCaret().apply {
            updatePolicy = DefaultCaret.NEVER_UPDATE
        }
        textPane.caret.isVisible = true
        textPane.caret.isSelectionVisible = true
        textPane.editorKit = NoWrapEditorKit()
    }

    fun setContent(text: Iterable<BlockModel>) {
        val document = DefaultStyledDocument()
        textPane.removeCaretListener(lineSelectionHandler)
        textPane.highlighter.removeAllHighlights()
        selectionModel = SelectionModel(text)
        val attributes = getDocumentAttributes()

        fun appendBlock(block: BlockModel, getHighlighter: (BlockType) -> Highlighter.HighlightPainter?) {
            val startOffset = document.length
            val contentString = block.getContentString()
            document.insertString(startOffset, contentString, attributes)
            val highlighter = getHighlighter(block.type)
            if (highlighter != null)
                textPane.highlighter.addHighlight(startOffset, startOffset + contentString.length, highlighter)
        }

        fun appendLineBlock(block: BlockModel) {
            appendBlock(block, { getLineHighlighter(it) })
        }

        for (block in text) {
            val blockStartOffset = document.length
            if (block.children != null) {
                for (child in block.children)
                    appendBlock(child, { getWordHighlighter(it) })
                val highlighter = getLineHighlighter(block.type)
                if (highlighter != null)
                    textPane.highlighter.addHighlight(blockStartOffset, document.length, highlighter)
            } else
                appendLineBlock(block)
            if (block.padding != null)
                appendLineBlock(block.padding)
        }
        textPane.document = document
        textPane.addCaretListener(lineSelectionHandler)
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
        scrollToOffset(span.start)
        highlightSelectedSpan(span)
    }

    private fun highlightSelectedSpan(span: TextSpan) {
        if (this.selectedHighlightTag != null)
            this.textPane.highlighter.removeHighlight(this.selectedHighlightTag)
        this.selectedHighlightTag = textPane.highlighter.addHighlight(span.start, span.end, selectedHighlighter)

        this.textPane.repaint()
    }

    private fun scrollToOffset(offset: Int) {
        val targetY = textPane.modelToView(offset).y
        val newY = targetY - textPane.visibleRect.height / 2
        scrollPane.verticalScrollBar.value = if (newY < 0) 0 else newY
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

    private fun getDocumentAttributes(): AttributeSet {
        return StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.FontFamily, "Lucida Console")
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

data class TextSpan(val start: Int, val end: Int) {

}
