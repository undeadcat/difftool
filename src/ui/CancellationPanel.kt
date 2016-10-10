package ui

import utils.ProgressIndicator
import java.awt.EventQueue
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar

class CancellationPanel : JPanel(FlowLayout(FlowLayout.TRAILING, 5, 0)) {
    private var registeredForCancellation = arrayListOf<() -> Unit>()
    val progressBar = JProgressBar()

    init {
        add(JLabel("Loading diff..."))
        add(progressBar)
        progressBar.minimum = 0
        progressBar.maximum = 100
        progressBar.isIndeterminate = false
        progressBar.isStringPainted = true
        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener {
            synchronized(this, {
                val registrations = registeredForCancellation
                this.registeredForCancellation = arrayListOf<() -> Unit>()
                for (cancellable in registrations)
                    cancellable()
            })
            isVisible = false
        }
        add(cancelButton)
        isVisible = false
    }

    fun registerForCancellation(action: () -> Unit) {
        synchronized(this, {
            registeredForCancellation.add(action)
        })
    }

    fun createProgressIndicator(): ProgressIndicator {
        progressBar.maximum = 0
        progressBar.maximum = 100
        progressBar.value = 0
        val progressIndicator = ProgressIndicator({
            percentComplete ->
            invokeOnDispatchThread { progressBar.value = percentComplete }
        })
        progressIndicator.setMax(100)
        return progressIndicator
    }

    private fun invokeOnDispatchThread(callback: () -> Unit) {
        if (EventQueue.isDispatchThread())
            callback()
        else EventQueue.invokeLater(callback)
    }
}