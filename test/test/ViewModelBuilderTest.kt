package test

import diff.ChangesBuilder
import diff.PatienceDiffAlgorithm
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test
import ui.BlockModel
import ui.BlockType
import ui.ViewModel
import ui.ViewModelBuilder

class ViewModelBuilderTest {

    @Test
    fun applyContextLimit() {
        val leftLines = listOf("a", "c", "d", "e", "f", "g", "h")
        val rightLines = listOf("a", "1", "c", "d", "e", "3", "f", "g", "h")
        val viewModel = buildModel(leftLines, rightLines, contextLimit = 1)
                .right
                .map { Pair(it.type, it.content) }
                .toList()

        Assert.assertThat(viewModel, Is.`is`(listOf(
                Pair(BlockType.Matching, lines("a")),
                Pair(BlockType.Added, lines("1")),
                Pair(BlockType.Matching, lines("c")),
                Pair(BlockType.ContextExcluded, lines("")),
                Pair(BlockType.Matching, lines("e")),
                Pair(BlockType.Added, lines("3")),
                Pair(BlockType.Matching, lines("f")),
                Pair(BlockType.ContextExcluded, lines(""))
        )))
    }

    @Test
    fun replaceWithDifferentNumberOfLines_addPadding() {
        val left = listOf("a", "b", "c")
        val right = listOf("a", "new1", "new2", "new3", "c")
        val viewModel = buildModel(left, right)

        val changedBlockLeft = viewModel.left.single { it.type == BlockType.Deleted }
        val changedBlockRight = viewModel.right.single { it.type == BlockType.Added }
        Assert.assertNotNull(changedBlockLeft.padding)
        Assert.assertThat((changedBlockLeft.padding as BlockModel).content, Is.`is`(lines("", "")))

        Assert.assertNull(changedBlockRight.padding)
    }

    @Test
    fun addWordsDiffToChangedBlocksOnly() {
        val left = listOf("a", "word1 word2", "word3 deletedWord", "b")
        val right = listOf("added", "a", "changedWord word2", "word3 addedWord", "b")
        val viewModel = buildModel(left, right, diffWords = true)
        Assert.assertNull(viewModel.right[0].children)
        Assert.assertNull(viewModel.left[0].children)
        val changedBlockLeft = viewModel.left[2].children
        val changedBlockRight = viewModel.right[2].children
        Assert.assertNotNull(changedBlockLeft)
        Assert.assertNotNull(changedBlockRight)

        Assert.assertThat(changedBlockLeft, Is.`is`(listOf(
                BlockModel(BlockType.Deleted, "word1"),
                BlockModel(BlockType.Matching, " word2\nword3 "),
                BlockModel(BlockType.Deleted, "deletedWord"),
                BlockModel(BlockType.Matching, "\n")
        )))


        Assert.assertThat(changedBlockRight, Is.`is`(listOf(
                BlockModel(BlockType.Added, "changedWord"),
                BlockModel(BlockType.Matching, " word2\nword3 "),
                BlockModel(BlockType.Added, "addedWord"),
                BlockModel(BlockType.Matching, "\n")
        )))

    }

    private fun buildModel(left: List<String>, right: List<String>,
                           diffWords: Boolean = false, contextLimit: Int? = null): ViewModel {
        val matches = PatienceDiffAlgorithm().getMatches(left, right)
        val diffItems = ChangesBuilder().build(left, right, matches)
        return ViewModelBuilder(diffWords, contextLimit).build(diffItems)
    }

    private fun lines(vararg lines: String): String {
        return lines.joinToString("\n") + "\n"
    }
}