package ui

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.text.*

class FileContentPane {

    private var selectedHighlightTag: Any? = null
    private val textPane = JTextPane()
    private var selectedSpan: TextSpan? = null
    private var changeOffsets = arrayListOf<TextSpan>()
    val scrollPane = JScrollPane(textPane)

    init {
        textPane.isEditable = false
        textPane.caret.isVisible = true
        textPane.caret.isSelectionVisible = true
        textPane.editorKit = NoWrapEditorKit()
    }

    fun setContent(text: Iterable<BlockModel>) {
        val document = DefaultStyledDocument()
        val attributes = getDocumentAttributes()

        fun appendLineBlock(block: BlockModel) {
            val startOffset = document.length
            document.insertString(startOffset, block.content, attributes)
            val highlighter = getLineHighlighter(block.type)
            if (highlighter != null)
                textPane.highlighter.addHighlight(startOffset, startOffset + block.content.length, highlighter)
        }

        fun appendWordBlock(block: BlockModel) {
            val startOffset = document.length
            document.insertString(startOffset, block.content, attributes)
            val highlighter = when (block.type) {
                BlockType.Deleted -> wordDeletedHighlighter
                BlockType.Added -> wordAddedHighlighter
                else -> null
            }
            if (highlighter != null)
                textPane.highlighter.addHighlight(startOffset, startOffset + block.content.length, highlighter)
        }

        changeOffsets = arrayListOf<TextSpan>()
        textPane.highlighter.removeAllHighlights()
        for (block in text) {
            val blockStartOffset = document.length
            if (block.children != null) {
                for (child in block.children)
                    appendWordBlock(child)
                val highlighter = getLineHighlighter(block.type)
                if (highlighter != null)
                    textPane.highlighter.addHighlight(blockStartOffset, document.length, highlighter)
            } else
                appendLineBlock(block)
            if (block.padding != null)
                appendLineBlock(block.padding)
            if (block.type == BlockType.Added || block.type == BlockType.Deleted)
                changeOffsets.add(TextSpan(blockStartOffset, document.length))
        }

        textPane.document = document
    }

    fun selectNextChange() {
        val currentOffset = selectedSpan?.start ?: 0
        changeSelection(changeOffsets.firstOrNull() { c -> c.start > currentOffset })
    }

    fun selectPreviousChange() {
        val currentOffset = selectedSpan?.start ?: 0
        changeSelection(changeOffsets.lastOrNull { c -> c.start < currentOffset })
    }

    private fun changeSelection(span: TextSpan?) {
        if (span == null)
            return
        val targetY = textPane.modelToView(span.start).y
        val newY = targetY - textPane.visibleRect.height / 2
        scrollPane.verticalScrollBar.value = if (newY < 0) 0 else newY
        if (this.selectedHighlightTag != null)
            this.textPane.highlighter.removeHighlight(this.selectedHighlightTag)
        this.selectedHighlightTag = textPane.highlighter.addHighlight(span.start, span.end, selectedHighlighter)
        selectedSpan = span

        this.textPane.repaint()
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

    private fun getLineHighlighter(type: BlockType): Highlighter.HighlightPainter? {
        when (type) {

            BlockType.Padding -> return paddingHighlighter
            BlockType.Added -> return lineAddedHighlighter
            BlockType.Deleted -> return lineDeletedHighlighter
            BlockType.ContextExcluded -> return contextExcludedHighlighter
            else -> return null
        }
    }

    data private class TextSpan(val start: Int, val end: Int) {

    }
}