package ui

import utils.splitByCount
import java.awt.EventQueue
import java.util.*
import javax.swing.BoundedRangeModel
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.text.*

class LazyTextPane {
    val textPane = JTextPane()
    val scrollPane = JScrollPane(textPane)//, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    private val pendingHighlights = ArrayDeque<HighlightInfo>()
    private val pendingStrings = ArrayDeque<String>()
//    private val myScrollBar = JScrollBar()

    init {
//        scrollPane.layout.addLayoutComponent(ScrollPaneConstants.VERTICAL_SCROLLBAR, myScrollBar)
//        myScrollBar.size = Dimension(800,20)
        textPane.isEditable = false
        textPane.editorKit = NoWrapEditorKit()
        textPane.caret = DefaultCaret().apply { this.updatePolicy = DefaultCaret.NEVER_UPDATE }
        textPane.caret.isVisible = true
        textPane.caret.isSelectionVisible = true
        scrollPane.verticalScrollBar.addAdjustmentListener { ensureCurrentPageLoaded() }
    }

    fun clear() {
        pendingStrings.clear()
        pendingHighlights.clear()
        highlightedOffset = 0
        EventQueue.invokeLater {
            textPane.highlighter.removeAllHighlights()
            textPane.document.remove(0, textPane.document.length)
        }
    }

    fun setScrollModel(model: BoundedRangeModel) {
//        myScrollBar.model = model
    }

    fun appendLine(content: String) {
        pendingStrings.addLast(content)
    }

    fun enqueueHighlight(startOffset: Int, endOffset: Int, highlighter: Highlighter.HighlightPainter) {
        pendingHighlights.offer(HighlightInfo(startOffset, endOffset, highlighter))
    }

    fun ensureLoadedTo(textOffset: Int) {
        ensureLoaded {
            textPane.document.length >= textOffset
                    && getLoadedViewSize() > textOffsetToView(textOffset) + textPane.visibleRect.height
        }
    }

    fun ensureCurrentPageLoaded() {
        val loadTarget = textPane.visibleRect.y + textPane.visibleRect.height * 2
        ensureLoaded {
            getLoadedViewSize() > loadTarget
        }
    }

    private fun textOffsetToView(textOffset: Int): Int {
        return textPane.modelToView(textOffset)?.y ?: 0
    }

    private fun getLoadedViewSize(): Int {
        return textPane.modelToView(textPane.document.length)?.y ?: 0
    }

    private var highlightedOffset: Int = 0

    private fun ensureLoaded(condition: () -> Boolean) {
        var loadedTextOffset = textPane.document.length

        val attributes = getDocumentAttributes()
        while (!pendingStrings.isEmpty() && !condition()) {

            val s = pendingStrings.removeFirst()
            textPane.document.insertString(loadedTextOffset, s, attributes)
            loadedTextOffset += s.length
        }

        do {
            val highlight = pendingHighlights.peek()
            if (highlight == null || highlight.endOffset >= loadedTextOffset - 1)
                break
            pendingHighlights.remove()
            textPane.highlighter.addHighlight(highlight.startOffset, highlight.endOffset, highlight.highlighter)
            highlightedOffset = highlight.endOffset

        } while (highlightedOffset < loadedTextOffset)
    }

    private fun getDocumentAttributes(): AttributeSet {
        return StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.FontFamily, "Lucida Console")
    }

    private data class HighlightInfo(val startOffset: Int, val endOffset: Int, val highlighter: Highlighter.HighlightPainter) {

    }
}
