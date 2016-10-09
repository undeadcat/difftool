package ui

import java.awt.FlowLayout
import java.util.concurrent.Future
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar

class CancellationPanel : JPanel(FlowLayout(FlowLayout.TRAILING, 5, 0)) {
    private var registeredFutures = arrayListOf<Future<*>>()

    init {
        val progressBar = JProgressBar()
        progressBar.isIndeterminate = true
        add(JLabel("Loading diff..."))
        add(progressBar)
        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener {
            synchronized(this, {
                val futures = registeredFutures
                registeredFutures = arrayListOf<Future<*>>()
                for (future in futures) {
                    future.cancel(true)
                }
            })
            isVisible = false
        }
        add(cancelButton)
        isVisible = false
    }

    fun registerForCancellation(future: Future<*>) {
        synchronized(this, {
            registeredFutures.add(future)
        })
    }
}