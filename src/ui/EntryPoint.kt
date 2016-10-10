package ui

import diff.ChangesBuilder
import diff.DiffItem
import diff.PatienceDiffAlgorithm
import utils.parseIntOrNull
import utils.throwIfInterrupted
import utils.time
import java.awt.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

class EntryPoint private constructor(newUiConfig: UiConfig) : JFrame() {

    private val diffModeCombobox = createModeCombobox()
    private val contextSizeCombobox = createContextSizeCombobox()
    private val leftFileInput = FileInput({ file ->
        recalculateDiff(this.uiConfig.copy(leftFileName = file))
    })

    private val rightFileInput = FileInput({ file ->
        recalculateDiff(this.uiConfig.copy(rightFileName = file))
    })

    val leftSide = FileContentPane()
    val rightSide = FileContentPane()
    val changesBuilder = ChangesBuilder()
    val diffAlgorithm = PatienceDiffAlgorithm()
    val backgroundExecutor: ExecutorService = Executors.newFixedThreadPool(1)
    val cancellationPanel = CancellationPanel()
    var changes = listOf <DiffItem<String>>()
    var uiConfig = UiConfig(null, null)

    init {
        initUi()
        applyUiConfig(newUiConfig)
        recalculateDiff(newUiConfig)
    }

    private fun recalculateDiff(newConfig: UiConfig) {
        if (newConfig == uiConfig)
            return
        val previousConfig = uiConfig
        uiConfig = newConfig
        cancellationPanel.isVisible = true
        val progressIndicator = cancellationPanel.createProgressIndicator()
        val future = backgroundExecutor.submit<Unit>({ ->

            val theLeftFileName = newConfig.leftFileName
            val theRightFileName = newConfig.rightFileName
            try {
                if (theLeftFileName != previousConfig.leftFileName || theRightFileName != previousConfig.rightFileName) {
                    val leftFile = if (theLeftFileName != null) utils.readFile(theLeftFileName, progressIndicator.createChild(20)) else emptyList()
                    val rightFile = if (theRightFileName != null) utils.readFile(theRightFileName, progressIndicator.createChild(20)) else emptyList()
                    changes = time({ -> changesBuilder.build(leftFile, rightFile, diffAlgorithm.getMatches(leftFile, rightFile, progressIndicator.createChild(90))) }, "build changes")
                }
                Thread.currentThread().throwIfInterrupted()

                val viewModel = time({ -> ViewModelBuilder(newConfig.diffWords, newConfig.contextLimit).build(changes) }, "built viewModel")
                progressIndicator.done()
                Thread.currentThread().throwIfInterrupted()
                time({ ->
                    leftSide.setContent(viewModel.left)
                    rightSide.setContent(viewModel.right)
                }, "set model")
            }
            catch (e: Exception) {
                if (e is InterruptedException) {
                    //user-initiated cancellation. don't do anything
                } else e.printStackTrace()

                EventQueue.invokeLater {
                    uiConfig = previousConfig
                    applyUiConfig(previousConfig)
                }
            }
            finally {
                EventQueue.invokeLater { cancellationPanel.isVisible = false }
            }
        })
        cancellationPanel.registerForCancellation({ future.cancel(true) })
    }

    private fun initUi() {
        title = "DiffTool"
        setLocationRelativeTo(null)
        defaultCloseOperation = EXIT_ON_CLOSE
        extendedState = Frame.MAXIMIZED_BOTH

        leftSide.setLineSelectedListener { rightSide.selectByLineNumber(it) }
        rightSide.setLineSelectedListener { leftSide.selectByLineNumber(it) }
        leftSide.lazyTextPane.scrollPane.horizontalScrollBar.model = rightSide.lazyTextPane.scrollPane.horizontalScrollBar.model
        leftSide.lazyTextPane.scrollPane.verticalScrollBar.model = rightSide.lazyTextPane.scrollPane.verticalScrollBar.model

        val toolbar = CreateToolbar()

        contentPane.layout = GridBagLayout()
        contentPane.add(toolbar, GridBagConstraints().apply {
            this.fill = GridBagConstraints.HORIZONTAL
            this.gridwidth = 2
            this.gridy = 0
        })
        contentPane.add(leftFileInput.panel, GridBagConstraints().apply {
            this.fill = GridBagConstraints.HORIZONTAL
            this.weightx = 0.5
            this.gridy = 1
        })
        contentPane.add(rightFileInput.panel, GridBagConstraints().apply {
            this.fill = GridBagConstraints.HORIZONTAL
            this.weightx = 0.5
            this.gridy = 1
        })
        contentPane.add(leftSide.lazyTextPane.scrollPane, GridBagConstraints().apply {
            this.fill = GridBagConstraints.BOTH
            this.weightx = 0.5
            this.weighty = 0.9
            this.gridy = 2
        })
        contentPane.add(rightSide.lazyTextPane.scrollPane, GridBagConstraints().apply {
            this.fill = GridBagConstraints.BOTH
            this.weightx = 0.5
            this.weighty = 0.9
            this.gridy = 2
        })

        pack()
    }

    private fun CreateToolbar(): JPanel {
        val toolbar = JPanel()
        toolbar.layout = FlowLayout(FlowLayout.LEFT, 5, 5)
        val nextChangeButton = JButton("next")
        nextChangeButton.addActionListener { i ->
            rightSide.selectNextChange()
            leftSide.selectNextChange()
        }
        val prevChangeButton = JButton("prev")
        prevChangeButton.addActionListener { i ->
            rightSide.selectPreviousChange()
            leftSide.selectPreviousChange()
        }
        toolbar.add(prevChangeButton)
        toolbar.add(nextChangeButton)

        val contextLabel = JLabel("Context size:")

        toolbar.add(contextLabel)
        toolbar.add(contextSizeCombobox)
        toolbar.add(diffModeCombobox)
        toolbar.add(cancellationPanel)
        return toolbar
    }

    private fun applyUiConfig(uiConfig: UiConfig) {
        contextSizeCombobox.setValue(uiConfig.contextLimit)
        diffModeCombobox.setValue(uiConfig.diffWords)
        leftFileInput.setValue(uiConfig.leftFileName)
        rightFileInput.setValue(uiConfig.rightFileName)
    }

    private fun createContextSizeCombobox(): SimpleCombobox<Int?> {
        val contextItems = arrayOf(SimpleCombobox.Item(null, "Unlimited"),
                SimpleCombobox.Item(1),
                SimpleCombobox.Item(2),
                SimpleCombobox.Item(4),
                SimpleCombobox.Item(8))
        return SimpleCombobox(contextItems, null,
                {
                    newValue ->
                    recalculateDiff(this.uiConfig.copy(contextLimit = newValue))
                })
    }

    private fun createModeCombobox(): SimpleCombobox<Boolean> {
        val modeItems = arrayOf(
                SimpleCombobox.Item(false, "Show diff lines"),
                SimpleCombobox.Item(true, "Show diff words"))
        return SimpleCombobox(modeItems,
                false,
                { newValue ->
                    recalculateDiff(this.uiConfig.copy(diffWords = newValue))
                })
    }

    data class UiConfig(
            val leftFileName: String?,
            val rightFileName: String?,
            val diffWords: Boolean = false,
            val contextLimit: Int? = null) {
    }

    companion object {

        @JvmStatic fun main(args: Array<String>) {
            if (args.elementAtOrElse(0, { i -> "" }) == "--h") {
                print("""Usage: difftool <left> <right> <options>

Options are:

-c <n>      limit context to n lines
-w          getMatches words in changed lines""")
                return
            }

            val uiConfig = toUiConfig(args)
            if (uiConfig.leftFileName != null && !File(uiConfig.leftFileName).exists())
                throw Exception("File not found: $uiConfig.leftFileName")
            if (uiConfig.rightFileName != null && !File(uiConfig.rightFileName).exists())
                throw Exception("File not found: $uiConfig.rightFileName")


            EventQueue.invokeAndWait({
                val entryPoint = EntryPoint(uiConfig)
                entryPoint.isVisible = true
            })

        }

        private fun toUiConfig(args: Array<String>): UiConfig {
            val uiConfig = UiConfig(args.elementAtOrNull(0), args.elementAtOrNull(1))
            val options = utils.parseCommandLineArgs(args.drop(2))
            val diffWords = options.getOrDefault("w", false) as Boolean
            val contextLinesStr = options.getOrDefault("c", null) as? String?
            if (contextLinesStr != null) {
                val contextLines = parseIntOrNull(contextLinesStr)
                        ?: throw Exception("Could not parse int from context lines (-c) parameter value: [$contextLinesStr]")
                return uiConfig.copy(diffWords = diffWords, contextLimit = contextLines)

            }
            return uiConfig
        }
    }
}

