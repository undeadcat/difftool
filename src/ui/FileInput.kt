package ui

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.JTextField

class FileInput(val fileChosen: (String) -> Unit) {
    val panel = JPanel()
    private val textInput = JTextField()
    private val button = JButton("Open file")
    private val fileChooser = JFileChooser()

    init {
        panel.layout = GridBagLayout()
        panel.add(textInput, GridBagConstraints().apply {
            this.fill = GridBagConstraints.HORIZONTAL
            this.weightx = 1.0
        })

        panel.add(button, GridBagConstraints().apply {
            this.gridy = 0
            this.anchor = GridBagConstraints.LINE_END
        })

        textInput.addActionListener { textInput.transferFocus() }
        textInput.addFocusListener(object : FocusListener {
            override fun focusLost(e: FocusEvent?) {
                selectFile(textInput.text)
            }

            override fun focusGained(e: FocusEvent?) {
            }

        })
        button.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (fileChooser.showOpenDialog(textInput) != JFileChooser.APPROVE_OPTION)
                    return
                selectFile(fileChooser.selectedFile.canonicalPath)
            }
        })
    }

    fun setValue(text: String?) {
        textInput.text = text
        textInput.caretPosition = 0
    }

    private fun selectFile(text: String) {
        if (text.isNullOrEmpty()
                || !File(text).exists())
            return
        fileChosen(text)
        setValue(text)
    }
}