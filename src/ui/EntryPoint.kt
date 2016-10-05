package ui

import diff.ChangesBuilder
import diff.DiffItem
import diff.PatienceDiffAlgorithm
import utils.parseIntOrNull
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Frame
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.swing.*

class EntryPoint(var uiConfig: UiConfig) : JFrame() {

    val leftSide = FileContentPane()
    val rightSide = FileContentPane()
    val changesBuilder = ChangesBuilder()
    val diffAlgorithm = PatienceDiffAlgorithm()
    var changes = listOf <DiffItem<String>>()
    val backgroundExecutor: ExecutorService = Executors.newFixedThreadPool(1)

    init {
        initUi()
        updateView(uiConfig)
    }

    private fun updateView(newConfig: UiConfig) {
        backgroundExecutor.submit({ ->
            val startTime = System.nanoTime()
            val previousLeftFile = uiConfig.leftFileName
            val previousRightFile = uiConfig.rightFileName

            uiConfig = newConfig
            val theLeftFileName = uiConfig.leftFileName
            val theRightFileName = uiConfig.rightFileName
            try {

                if (theLeftFileName != previousLeftFile || theRightFileName != previousRightFile) {
                    val leftFile = if (theLeftFileName != null) utils.readFile(theLeftFileName) else emptyList()
                    val rightFile = if (theRightFileName != null) utils.readFile(theRightFileName) else emptyList()
                    changes = changesBuilder.build(leftFile, rightFile, diffAlgorithm.getMatches(leftFile, rightFile))
                }

                val viewModel = ViewModelBuilder(uiConfig.diffWords, uiConfig.contextLimit).build(changes)

                EventQueue.invokeLater { setModel(viewModel) }
            } catch (e: Exception) {
                print("Exception building diff between [$theLeftFileName] and [$theRightFileName]: ")
                e.printStackTrace()
            }
            val endTime = System.nanoTime();
            println("Built diff in ${(endTime - startTime)/1000000} ms")

        })
    }

    private fun initUi() {
        title = "DiffTool"
        setLocationRelativeTo(null)
        defaultCloseOperation = EXIT_ON_CLOSE
        extendedState = Frame.MAXIMIZED_BOTH

        leftSide.scrollPane.preferredSize = Dimension(contentPane.width / 2, contentPane.height)
        rightSide.scrollPane.preferredSize = Dimension(contentPane.width / 2, contentPane.height)
        leftSide.scrollPane.verticalScrollBar.model = rightSide.scrollPane.verticalScrollBar.model
        leftSide.scrollPane.horizontalScrollBar.model = rightSide.scrollPane.horizontalScrollBar.model

        val toolbar = CreateToolbar()
        val leftFileNameView = FileNameView(uiConfig.leftFileName, { file ->
            updateView(uiConfig.copy(leftFileName = file))
        })
        val rightFileNameView = FileNameView(uiConfig.rightFileName, { file ->
            updateView(uiConfig.copy(rightFileName = file))
        })

        val groupLayout = GroupLayout(contentPane)
        contentPane.layout = groupLayout
        groupLayout.autoCreateGaps = true

        groupLayout.setVerticalGroup(
                groupLayout.createSequentialGroup()
                        .addComponent(toolbar)
                        .addGroup(groupLayout.createParallelGroup()
                                .addComponent(leftFileNameView.label)
                                .addComponent(rightFileNameView.label))
                        .addGroup(groupLayout.createParallelGroup()
                                .addComponent(leftSide.scrollPane)
                                .addComponent(rightSide.scrollPane)))
        groupLayout.setHorizontalGroup(groupLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(toolbar)
                .addGroup(groupLayout.createSequentialGroup()
                        .addComponent(leftFileNameView.label, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Int.MAX_VALUE)
                        .addComponent(rightFileNameView.label, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Int.MAX_VALUE))
                .addGroup(groupLayout.createSequentialGroup()
                        .addComponent(leftSide.scrollPane)
                        .addComponent(rightSide.scrollPane)))

        pack()
    }

    private fun setModel(viewModel: ViewModel) {
        leftSide.setContent(viewModel.left)
        rightSide.setContent(viewModel.right)
    }

    private fun CreateToolbar(): JToolBar {
        val toolbar = JToolBar()
        toolbar.isFloatable = false
        toolbar.isRollover = false
        toolbar.isBorderPainted = false
        val nextChangeButton = JButton("next")
        nextChangeButton.addActionListener { i ->
            leftSide.scrollToNextChange()
        }
        val prevChangeButton = JButton("prev")
        prevChangeButton.addActionListener { i ->
            leftSide.scrollToPreviousChange()
        }
        toolbar.add(prevChangeButton)
        toolbar.add(nextChangeButton)

        val contextPanel = JPanel()
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
                    updateView(uiConfig.copy(contextLimit = newValue))
                })
        contextPanel.add(contextLabel)
        contextPanel.add(contextCombobox)
        val modeItems = arrayOf(
                ComboboxItem(false, "Show diff lines"),
                ComboboxItem(true, "Show diff words"))
        val diffModeButtonCombobox = CreateCombobox(modeItems,
                uiConfig.diffWords,
                { newValue ->
                    updateView(uiConfig.copy(diffWords = newValue))
                })
        toolbar.add(contextLabel)
        toolbar.add(contextCombobox)
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

