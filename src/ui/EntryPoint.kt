package ui

import diff.ChangesBuilder
import diff.DiffItem
import diff.PatienceDiffAlgorithm
import utils.getGridBagConstraints
import utils.parseIntOrNull
import java.awt.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.swing.*

class EntryPoint(newUiConfig: UiConfig) : JFrame() {

    var uiConfig = UiConfig(null, null)
    val leftSide = FileContentPane()
    val rightSide = FileContentPane()
    val changesBuilder = ChangesBuilder()
    val diffAlgorithm = PatienceDiffAlgorithm()
    var changes = listOf <DiffItem<String>>()
    val backgroundExecutor: ExecutorService = Executors.newFixedThreadPool(1)

    init {
        initUi(newUiConfig)
        updateView(newUiConfig)
    }

    private fun updateView(newConfig: UiConfig) {
        backgroundExecutor.submit({ ->
            val startTime = System.nanoTime()
            val previousLeftFile = uiConfig.leftFileName
            val previousRightFile = uiConfig.rightFileName

            uiConfig = newConfig
            val theLeftFileName = newConfig.leftFileName
            val theRightFileName = newConfig.rightFileName
            try {

                if (theLeftFileName != previousLeftFile || theRightFileName != previousRightFile) {
                    val leftFile = if (theLeftFileName != null) utils.readFile(theLeftFileName) else emptyList()
                    val rightFile = if (theRightFileName != null) utils.readFile(theRightFileName) else emptyList()
                    changes = changesBuilder.build(leftFile, rightFile, diffAlgorithm.getMatches(leftFile, rightFile))
                }

                val viewModel = ViewModelBuilder(newConfig.diffWords, newConfig.contextLimit).build(changes)

                EventQueue.invokeLater { setModel(viewModel) }
            } catch (e: Exception) {
                print("Exception building diff between [$theLeftFileName] and [$theRightFileName]: ")
                e.printStackTrace()
            }
            val endTime = System.nanoTime()
            println("Built diff in ${(endTime - startTime) / 1000000} ms")
        })
    }

    private fun initUi(newConfig: UiConfig) {
        title = "DiffTool"
        setLocationRelativeTo(null)
        defaultCloseOperation = EXIT_ON_CLOSE
        extendedState = Frame.MAXIMIZED_BOTH

        leftSide.scrollPane.horizontalScrollBar.model = rightSide.scrollPane.horizontalScrollBar.model
        leftSide.scrollPane.verticalScrollBar.model = rightSide.scrollPane.verticalScrollBar.model

        val toolbar = CreateToolbar(newConfig)
        val leftFileInput = FileInput(newConfig.leftFileName, { file ->
            updateView(this.uiConfig.copy(leftFileName = file))
        })
        val rightFileInput = FileInput(newConfig.rightFileName, { file ->
            updateView(this.uiConfig.copy(rightFileName = file))
        })

        contentPane.layout = GridBagLayout()
        contentPane.add(toolbar, getGridBagConstraints {
            it.fill = GridBagConstraints.HORIZONTAL
            it.gridwidth = 2
            it.gridy = 0
        })
        contentPane.add(leftFileInput.panel, getGridBagConstraints {
            it.fill = GridBagConstraints.HORIZONTAL
            it.weightx = 0.5
            it.gridy = 1
        })
        contentPane.add(rightFileInput.panel, getGridBagConstraints {
            it.fill = GridBagConstraints.HORIZONTAL
            it.weightx = 0.5
            it.gridy = 1
        })
        contentPane.add(leftSide.scrollPane, getGridBagConstraints {
            it.fill = GridBagConstraints.BOTH
            it.weightx = 0.5
            it.weighty = 0.9
            it.gridy = 2
        })
        contentPane.add(rightSide.scrollPane, getGridBagConstraints {
            it.fill = GridBagConstraints.BOTH
            it.weightx = 0.5
            it.weighty = 0.9
            it.gridy = 2
        })

        pack()
    }

    private fun setModel(viewModel: ViewModel) {
        leftSide.setContent(viewModel.left)
        rightSide.setContent(viewModel.right)
    }

    private fun CreateToolbar(uiConfig: UiConfig): JToolBar {
        val toolbar = JToolBar()
        toolbar.isFloatable = false
        toolbar.isRollover = false
        toolbar.isBorderPainted = false
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
        toolbar.add(Box.createHorizontalStrut(10))
        toolbar.add(nextChangeButton)
        toolbar.add(Box.createHorizontalStrut(10))

        val contextLabel = JLabel("Context size:")
        val contextItems = arrayOf(ComboboxItem(null, "Unlimited"),
                ComboboxItem(1),
                ComboboxItem(2),
                ComboboxItem(4),
                ComboboxItem(8))
        val contextLimit = uiConfig.contextLimit
        val contextCombobox = CreateCombobox(contextItems,
                if (contextLimit == null)
                    null
                else contextItems.first({ it.value != null && contextLimit > it.value }).value,
                {
                    newValue ->
                    updateView(this.uiConfig.copy(contextLimit = newValue))
                })
        val modeItems = arrayOf(
                ComboboxItem(false, "Show diff lines"),
                ComboboxItem(true, "Show diff words"))
        val diffModeButtonCombobox = CreateCombobox(modeItems,
                uiConfig.diffWords,
                { newValue ->
                    updateView(this.uiConfig.copy(diffWords = newValue))
                })
        toolbar.add(contextLabel)
        toolbar.add(contextCombobox)
        toolbar.add(Box.createHorizontalStrut(10))
        toolbar.add(diffModeButtonCombobox)
        return toolbar
    }

    private fun <T> CreateCombobox(items: Array<ComboboxItem<T>>, value: T?, valueChanged: (T) -> Unit): JComboBox<ComboboxItem<T>> {
        val combobox = JComboBox(items)
        combobox.prototypeDisplayValue = items.first()
        combobox.selectedItem = if (value == null)
            items.first()
        else items.last({ item -> item.value == value })
        combobox.maximumSize = Dimension(200, 40)
        combobox.addActionListener {
            i ->
            @Suppress("UNCHECKED_CAST")
            val item = combobox.selectedItem as ComboboxItem<T>
            valueChanged(item.value)
        }
        return combobox
    }

    private class ComboboxItem<out T>(val value: T, val description: String = value.toString()) {
        override fun toString(): String {
            return description
        }
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

