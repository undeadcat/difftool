package ui

import utils.getGridBagConstraints
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

class FileInput(fileName: String?, val fileChosen: (String) -> Unit) {
    val panel = JPanel()
    var currentFileName = fileName
    private val textInput = JTextField(fileName)
    private val button = JButton("Open file")
    private val fileChooser = JFileChooser()

    init {
        panel.layout = GridBagLayout()
        panel.add(textInput, getGridBagConstraints {
            it.fill = GridBagConstraints.HORIZONTAL
            it.weightx = 1.0
            it.gridx = 0
            it.gridy = 0
            it.gridwidth = 1
        })

        panel.add(button, getGridBagConstraints({
            it.gridx = 1
            it.gridwidth = 1
            it.gridy = 0
            it.anchor = GridBagConstraints.LINE_END
        }))

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

    private fun selectFile(text: String) {
        if (text.isNullOrEmpty()
                || currentFileName == text
                || !File(text).exists())
            return
        fileChosen(text)
        currentFileName = text
        textInput.text = text
        textInput.caretPosition = 0
    }
}