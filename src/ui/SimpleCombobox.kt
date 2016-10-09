package ui

import java.awt.Dimension
import javax.swing.JComboBox

class SimpleCombobox<T>(val items: Array<Item<T>>, value: T?, valueChanged: (T) -> Unit) : JComboBox<SimpleCombobox.Item<T>>(items) {

    init {
        prototypeDisplayValue = items.first()
        selectedItem = items.firstOrNull({ it.value == value }) ?: items.first()
        maximumSize = Dimension(200, 40)
        addActionListener {
            i ->
            @Suppress("UNCHECKED_CAST")
            val item = selectedItem as Item<T>
            valueChanged(item.value)
        }
    }

    fun setValue(value: T) {
        selectedItem = items.single({ it.value == value })
    }

    class Item<out T>(val value: T, val description: String = value.toString()) {
        override fun toString(): String {
            return description
        }
    }
}