package ui

import diff.ChangesBuilder
import diff.DiffItem
import diff.PatienceDiffAlgorithm
import utils.repeatElement

class ViewModelBuilder(private val diffWords: Boolean = false, private val contextLimit: Int? = null) {
    private val diffAlgorithm = PatienceDiffAlgorithm()
    private val changesBuilder = ChangesBuilder()
    private val wordBoundaryRegex = Regex("\\b")

    fun build(diffItems: List<DiffItem<String>>): ViewModel {
        val left = arrayListOf<BlockModel>()
        val right = arrayListOf<BlockModel>()

        for (change in diffItems.withIndex()) {
            val diffItem = change.value
            when (diffItem) {
                is DiffItem.Matched -> {
                    val blocks = applyContextLimit(diffItem.content, change.index, diffItems.size)
                    left.addAll(blocks)
                    right.addAll(blocks)
                }
                is DiffItem.Changed -> {
                    val deletions = diffItem.deletions
                    val additions = diffItem.additions
                    val paddingSize = Math.max(deletions.size, additions.size) - Math.min(deletions.size, additions.size)
                    val paddingBlock = if (paddingSize > 0) blockFromLines(BlockType.Padding, repeatElement("", paddingSize)) else null
                    val deletedContent = toContent(deletions)
                    val addedContent = toContent(additions)
                    val wordsDiff = if (deletions.size > 0 && additions.size > 0 && diffWords)
                        getWordBlocks(deletedContent, addedContent)
                    else Pair(null, null)
                    left.add(BlockModel(BlockType.Deleted, deletions, "\n",
                            children = wordsDiff.first,
                            padding = if (deletions.size < additions.size) paddingBlock else null))
                    right.add(BlockModel(BlockType.Added, additions, "\n",
                            children = wordsDiff.second,
                            padding = if (additions.size < deletions.size) paddingBlock else null))
                }
            }
        }
        return ViewModel(left, right)
    }

    private fun getWordBlocks(left: String, right: String): Pair<List<BlockModel>, List<BlockModel>> {
        val leftWords = left.split(wordBoundaryRegex)
        val rightWords = right.split(wordBoundaryRegex)
        val diffItems = changesBuilder.build(leftWords, rightWords, diffAlgorithm.getMatches(leftWords, rightWords))
        val leftWordBlocks = diffItems.map {
            when (it) {
                is DiffItem.Changed -> BlockModel(BlockType.Deleted, it.deletions, "")
                is DiffItem.Matched -> BlockModel(BlockType.Matching, it.content, "")
            }
        }
        val rightWordBlocks = diffItems.map {
            when (it) {
                is DiffItem.Changed -> BlockModel(BlockType.Added, it.additions, "")
                is DiffItem.Matched -> BlockModel(BlockType.Matching, it.content, "")
            }
        }
        return Pair(leftWordBlocks, rightWordBlocks)
    }

    private fun applyContextLimit(content: List<String>, blockIndex: Int, blockCount: Int): Iterable<BlockModel> {

        if (contextLimit == null || content.size <= contextLimit)
            return listOf(blockFromLines(BlockType.Matching, content))
        val contextLimit = contextLimit
        val isFirstBlock = blockIndex != 0
        val isLastBlock = blockIndex != blockCount - 1
        if (!isFirstBlock && !isLastBlock && content.size < contextLimit * 2)
            return listOf(blockFromLines(BlockType.Matching, content))
        val result = arrayListOf<BlockModel>()

        if (isFirstBlock)
            result.add(blockFromLines(BlockType.Matching, content.take(contextLimit)))
        result.add(blockFromLines(BlockType.ContextExcluded, listOf("")))
        if (isLastBlock)
            result.add(blockFromLines(BlockType.Matching, content.takeLast(contextLimit)))
        return result
    }

    private fun blockFromLines(type: BlockType, lines: List<String>): BlockModel {
        return BlockModel(type, lines, "\n")
    }

    private fun toContent(lines: List<String>): String {
        if (lines.isEmpty())
            return ""
        return lines.joinToString("\n") + "\n"
    }

}