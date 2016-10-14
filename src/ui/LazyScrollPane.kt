package ui

import java.awt.Point
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.util.*
import javax.swing.BoundedRangeModel
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.event.ChangeListener
import javax.swing.plaf.basic.BasicScrollPaneUI
import javax.swing.text.*

class LazyScrollPane(private val textPane: JTextPane) {
    private val scrollPane = JScrollPane(textPane)
    private val pendingHighlights = ArrayDeque<HighlightInfo>()
    private val pendingStrings = ArrayDeque<String>()
    private var loadedLines = 0
    private var linesModel = LinesModel(emptyList())

    val rootPane: JComponent = scrollPane
    var verticalScrollModel: BoundedRangeModel
        get() = scrollPane.verticalScrollBar.model
        set(value) {
            scrollPane.verticalScrollBar.model = value
        }
    var horizontalScrollModel: BoundedRangeModel
        get() = scrollPane.horizontalScrollBar.model
        set(value) {
            scrollPane.horizontalScrollBar.model = value
        }

    init {
        scrollPane.ui = MyUi(this)
    }

    fun reset(linesModel: LinesModel) {
        pendingStrings.clear()
        pendingHighlights.clear()
        pendingStrings.addAll(linesModel.lines)
        loadedLines = 0
        this.linesModel = linesModel
        textPane.highlighter.removeAllHighlights()
        textPane.document.remove(0, textPane.document.length)
        updateScrollbarModel(linesModel.count)
    }

    fun getLinesPerScreen() = textPane.visibleRect!!.height / textPane.font.size

    fun enqueueHighlight(startOffset: Int, endOffset: Int, highlighter: Highlighter.HighlightPainter) {
        pendingHighlights.offer(HighlightInfo(startOffset, endOffset, highlighter))
    }

    fun ensureCurrentPageLoaded() {
        ensureLoadedToLineCount(verticalScrollModel.value)
    }

    fun ensureLoaded(span: TextSpan) {
        ensureLoaded { textPane.document.length >= span.end }
    }

    fun ensureLoadedToLineCount(scrollbarValue: Int) {
        val targetLines = scrollbarValue + verticalScrollModel.extent * 2
        ensureLoaded {
            loadedLines > targetLines
        }
    }

    private fun ensureLoaded(condition: () -> Boolean) {
        var loadedTextOffset = textPane.document.length
        val attributes = getDocumentAttributes()
        fun removeBatch() = (0..verticalScrollModel.extent).map { pendingStrings.poll() }.filterNotNull().toList()

        while (!condition()) {
            val batch = removeBatch()
            if (batch.isEmpty())
                break
            val s = batch.joinToString("")
            loadedLines += batch.size
            textPane.document.insertString(loadedTextOffset, s, attributes)
            loadedTextOffset += s.length
        }

        fun applyHighlight(highlight: HighlightInfo) {
            textPane.highlighter.addHighlight(highlight.startOffset, highlight.endOffset, highlight.highlighter)
        }

        val highlights = pendingHighlights
                .takeWhile { it.endOffset < loadedTextOffset }
                .map({ pendingHighlights.poll() })
        var previousHighlight: HighlightInfo? = null
        for (highlight in highlights) {

            if (previousHighlight == null)
                previousHighlight = highlight
            else if (previousHighlight.endOffset == highlight.startOffset
                    && previousHighlight.highlighter == highlight.highlighter)
                previousHighlight = previousHighlight.copy(endOffset = highlight.endOffset)
            else {
                applyHighlight(previousHighlight)
                previousHighlight = null
                applyHighlight(highlight)
            }
        }
        if (previousHighlight != null)
            applyHighlight(previousHighlight)
    }

    private fun updateScrollbarModel(lineCount: Int) {
        val extentSize = Math.min(getLinesPerScreen(), lineCount)
        verticalScrollModel.setRangeProperties(0, extentSize, 0, lineCount, false)
    }

    private fun getDocumentAttributes(): AttributeSet {
        return StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.FontFamily, "Lucida Console")
    }

    private data class HighlightInfo(val startOffset: Int, val endOffset: Int, val highlighter: Highlighter.HighlightPainter) {

    }

    private class MyUi(val lazyTextPane: LazyScrollPane) : BasicScrollPaneUI() {
        private val textPane = lazyTextPane.textPane
        private val viewport = lazyTextPane.scrollPane.viewport
        private val linesPerWheelUnit = 3

        override fun syncScrollPaneWithViewport() {
            val rect = textPane.visibleRect
            val scrollModel = lazyTextPane.verticalScrollModel
            if (rect.height < 1)
                return
            val screenStartOffset = textPane.viewToModel(rect.location)
            val screenEndOffset = textPane.viewToModel(Point(rect.x + rect.width, rect.y + rect.height))
            if (screenEndOffset < 0
                    || screenStartOffset < 0
                    || scrollModel.value >= scrollModel.maximum - scrollModel.extent)
                return
            val lines = lazyTextPane.linesModel
            val updatedExtent = lines.offsetToLineNumber(screenEndOffset) - lines.offsetToLineNumber(screenStartOffset)
            if (updatedExtent <= 0)
                return

            lazyTextPane.verticalScrollModel.extent = Math.min(updatedExtent, lines.count)
        }

        override fun createMouseWheelListener(): MouseWheelListener {
            val baseListener = super.createMouseWheelListener()
            return MouseWheelListener { e ->
                if (e.isShiftDown)
                    baseListener.mouseWheelMoved(e)
                else
                    handleScrollEvent(e)
            }
        }

        private fun handleScrollEvent(e: MouseWheelEvent) {
            e.consume()

            val toScroll = lazyTextPane.verticalScrollModel
            val direction = if (e.wheelRotation < 0) -1 else 1
            val oldValue = toScroll.value
            val scrollMin = toScroll.minimum
            val scrollMax = toScroll.maximum - toScroll.extent

            var newValue = if (e.scrollType == MouseWheelEvent.WHEEL_UNIT_SCROLL)
                oldValue + e.wheelRotation * linesPerWheelUnit
            else
                oldValue + lazyTextPane.getLinesPerScreen() * direction
            if (newValue < scrollMin)
                newValue = scrollMin
            if (newValue > scrollMax)
                newValue = scrollMax
            toScroll.value = newValue
        }

        override fun createVSBChangeListener(): ChangeListener {
            return ChangeListener {
                val model = it.source as BoundedRangeModel
                val lineCount = model.value
                val lines = lazyTextPane.linesModel
                lazyTextPane.ensureLoadedToLineCount(lineCount)

                val newPosition =
                        if (lines.count > 0) {
                            val textOffset = lines.lineNumberToOffset(lineCount).start
                            val viewPosition = textPane.modelToView(textOffset)
                            viewPosition.y
                        } else
                            0
                viewport.viewPosition = Point(viewport.viewPosition.x, newPosition)
            }
        }
    }
}
