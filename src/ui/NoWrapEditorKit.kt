package ui

import javax.swing.text.*

//from http://java-sl.com/wrap.html
class NoWrapEditorKit : StyledEditorKit() {

    val myViewFactory = ViewFactory({ elem ->
        createElement(elem)
    })

    private fun createElement(elem: Element): View {
        val kind = elem.name
        if (kind != null) {
            if (kind == AbstractDocument.ContentElementName) {
                return LabelView(elem)
            } else if (kind == AbstractDocument.ParagraphElementName) {
                return NoWrapParagraphView(elem)
            } else if (kind == AbstractDocument.SectionElementName) {
                return BoxView(elem, View.Y_AXIS)
            } else if (kind == StyleConstants.ComponentElementName) {
                return ComponentView(elem)
            } else if (kind == StyleConstants.IconElementName) {
                return IconView(elem)
            }
        }

        return LabelView(elem)
    }

    override fun getViewFactory(): ViewFactory {
        return myViewFactory
    }

    class NoWrapParagraphView(elem: Element) : ParagraphView(elem) {

        override fun layout(width: Int, height: Int) {
            super.layout(Int.MAX_VALUE, height)
        }

        override fun getMinimumSpan(axis: Int): Float {
            return super.getPreferredSpan(axis)
        }
    }

}