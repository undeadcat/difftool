package ui

import java.awt.Color
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.text.*

class FileContentPane() {

    private val textPane = JTextPane()
    private var changeOffsets = arrayListOf<Int>()
    val scrollPane = JScrollPane(textPane)


    init {
        textPane.isEditable = false
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

        changeOffsets = arrayListOf<Int>()
        textPane.highlighter.removeAllHighlights()
        for (block in text) {
            if (block.type == BlockType.Added || block.type == BlockType.Deleted)
                changeOffsets.add(document.length)
            if (block.children != null) {
                val startOffset = document.length
                for (child in block.children)
                    appendWordBlock(child)
                val highlighter = getLineHighlighter(block.type)
                if (highlighter != null)
                    textPane.highlighter.addHighlight(startOffset, document.length, highlighter)
            } else
                appendLineBlock(block)
            if (block.padding != null)
                appendLineBlock(block.padding)
        }

        textPane.document = document
    }


    fun scrollToNextChange() {
        doScroll { offsets, originalY -> offsets.firstOrNull { c -> textPane.modelToView(c).y > originalY } }
    }

    fun scrollToPreviousChange() {
        doScroll { offsets, originalY -> offsets.lastOrNull { c -> textPane.modelToView(c).y < originalY } }
    }

    fun doScroll(getOffset: (List<Int>, Int) -> Int?) {
        val viewportSize = textPane.visibleRect.height
        val originalY = scrollPane.verticalScrollBar.value + viewportSize / 2
        val targetTextOffset = getOffset(changeOffsets, originalY)
        if (targetTextOffset == null)
            return
        val newY = (textPane.modelToView(targetTextOffset)).y - viewportSize / 2
        if (newY < 0)
            scrollPane.verticalScrollBar.value = 0
        else
            scrollPane.verticalScrollBar.value = newY
    }

    private fun getLineHighlighter(color: Color): Highlighter.HighlightPainter {
        return Highlighter.HighlightPainter(fun(g, p1, p2, @Suppress("UNUSED_PARAMETER") unused, textComponent) {
            g.color = color
            val startPosition = textComponent.modelToView(p1)
            val endPosition = textComponent.modelToView(p2)
            g.fillRect(startPosition.x, startPosition.y, textComponent.width, endPosition.y - startPosition.y)
        })
    }

    private fun getDocumentAttributes(): AttributeSet {
        return StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.FontFamily, "Lucida Console")
    }

    private val lineAddedHighlighter = getLineHighlighter(Color(234, 255, 234))
    private val lineDeletedHighlighter = getLineHighlighter(Color(255, 234, 234))
    private val paddingHighlighter = getLineHighlighter(Color(240, 240, 240))
    private val contextExcludedHighlighter = getLineHighlighter(Color(243, 246, 250))
    private val wordAddedHighlighter = DefaultHighlighter.DefaultHighlightPainter(Color(170, 255, 170))
    private val wordDeletedHighlighter = DefaultHighlighter.DefaultHighlightPainter(Color(255, 170, 170))

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

