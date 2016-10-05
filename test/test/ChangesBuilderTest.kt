package test

import diff.ChangesBuilder
import diff.DiffItem
import diff.PatienceDiffAlgorithm
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test

class ChangesBuilderTest {

    @Test
    fun simple() {
        val left = listOf("common1", "changed-left", "common2", "deleted")
        val right = listOf("added", "common1", "changed-right", "common2")

        val result = buildDiff(left, right)

        val expected = listOf(
                DiffItem.Changed(emptyList(), listOf("added")),
                DiffItem.Matched(listOf("common1")),
                DiffItem.Changed(listOf("changed-left"), listOf("changed-right")),
                DiffItem.Matched(listOf("common2")),
                DiffItem.Changed(listOf("deleted"), emptyList()))
        Assert.assertThat(result, Is.`is`(expected))
    }

    @Test
    fun noMatches(): Unit {
        val left = listOf("a", "b")
        val right = listOf("c", "d", "e")
        Assert.assertThat(buildDiff(left, right), Is.`is`(listOf<DiffItem<*>>(DiffItem.Changed(left, right))))
    }

    @Test
    fun mergeNeighboringMatches() {
        val left = listOf("a", "b", "c")
        val right = listOf("b", "c")
        Assert.assertThat(buildDiff(left, right), Is.`is`(listOf(
                DiffItem.Changed(listOf("a"), listOf()),
                DiffItem.Matched(listOf("b", "c")))))
    }

    private fun buildDiff(left: List<String>, right: List<String>): List<DiffItem<String>> {
        return ChangesBuilder().build(left, right, PatienceDiffAlgorithm().getMatches(left, right))
    }
}