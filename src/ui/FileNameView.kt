package ui

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFileChooser
import javax.swing.JLabel

class FileNameView(fileName: String?, fileChosen: (String) -> Unit) {
    val label = JLabel(GetText(fileName))
    private val fileChooser = JFileChooser()

    init {
        label.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (fileChooser.showOpenDialog(label) != JFileChooser.APPROVE_OPTION)
                    return
                val file = fileChooser.selectedFile
                label.text = GetText(file.canonicalPath)
                fileChosen(file.canonicalPath)
            }

        })

    }

    private fun GetText(fileName: String?): String {
        if (fileName == null)
            return "Click to open file..."
        return fileName
    }

}