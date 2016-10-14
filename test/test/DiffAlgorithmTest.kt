package test

import diff.*
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import utils.readFile
import utils.time

class DiffAlgorithmTest {
    @Test
    fun sameFiles() {
        val document = listOf("1", "2", "3")
        val actual = getMatches(document, document)
        val expected = document.mapIndexed { i, s -> Match(i, i) }.toList()
        Assert.assertThat(actual, Is.`is`(expected))
    }

    @Test
    fun newFile() {
        val actual = getMatches(emptyList(), listOf("1", "2", "3"))
        Assert.assertThat(actual, Is.`is`(emptyList()))
    }

    @Test
    fun deletedFile() {
        val left = listOf("1", "2", "3")
        Assert.assertThat(getMatches(left, emptyList()), Is.`is`(emptyList()))
    }

    @Test
    fun simple_addAndRemove() {
        val original = listOf("1", "2", "3", "4", "5")
        val changed = listOf("1", "4", "6", "5", "7", "8")
        val expectedMatches = listOf(Match(0, 0), Match(3, 1), Match(4, 3))
        Assert.assertThat(getMatches(original, changed), Is.`is`(expectedMatches))
    }

    @Test
    fun changedLine() {
        val left = listOf("a", "b", "c")
        val right = listOf("a", "zzz", "c")
        val expected = listOf(Match(0, 0), Match(2, 2))
        Assert.assertThat(getMatches(left, right), Is.`is`(expected))
    }

    @Test
    fun completelyDifferentFiles() {
        val left = listOf("z")
        val right = listOf("a", "b")
        Assert.assertThat(getMatches(left, right), Is.`is`(emptyList()))
    }

    @Test fun selectLongestCommonSequence() {
        val left = listOf("a", "b", "c", "d", "e")
        val right = listOf("c", "d", "a", "b", "e")
        val expected = listOf(Match(2, 0), Match(3, 1), Match(4, 4))
        Assert.assertThat(getMatches(left, right), Is.`is`(expected))
    }

    @Test fun fileIsPrefixOfOther() {
        val left = listOf("a", "b", "c")
        val right = listOf("a", "b")
        val expected = listOf(Match(0, 0), Match(1, 1))
        Assert.assertThat(getMatches(left, right), Is.`is`(expected))
    }

    @Test fun fileIsPostfixOfOther() {
        val left = listOf("a", "b")
        val right = listOf("c", "a", "b")
        val expected = listOf(Match(0, 1), Match(1, 2))
        Assert.assertThat(getMatches(left, right), Is.`is`(expected))
    }

    @Test
    fun addMethod_doNotIncludeBracesInDiff() {
        val left = """void func1() {
x += 1
}
void func2() {
x += 2
}"""

        val right = """void func1() {
x += 1
}
void newFunction() {
println("new function")
}
void func2() {
x += 2
}"""
        val result = getDiff(left, right)
        checkEqual(result, listOf(
                DiffItem.Matched(listOf("void func1() {", "x += 1", "}")),
                DiffItem.Changed(emptyList(), listOf("void newFunction() {", "println(\"new function\")", "}")),
                DiffItem.Matched(listOf("void func2() {", "x += 2", "}"))))

    }

    @Test fun moveMethod() {
        val left = """void func1() {
x += 1
}
void movedFunction() {
println("moved function")
}
void func2() {
x += 2
}"""
        val right = """void func1() {
x += 1
}
void func2() {
x += 2
}
void movedFunction() {
println("moved function")
}"""
        checkEqual(getDiff(left, right), listOf(
                DiffItem.Matched(listOf("void func1() {", "x += 1", "}")),
                DiffItem.Changed(listOf("void movedFunction() {", "println(\"moved function\")", "}"), emptyList()),
                DiffItem.Matched(listOf("void func2() {", "x += 2", "}")),
                DiffItem.Changed(emptyList(), listOf("void movedFunction() {", "println(\"moved function\")", "}"))
        ))
    }

    @Test fun renameAndAddLines() {

        val left = """.foo1 {
margin: 0;
}
.bar {
margin: 0;
}"""
        val right = """.bar {
margin: 0;
}
.foo1 {
margin: 0;
color: green;
}"""
        checkEqual(getDiff(left, right), listOf(
                DiffItem.Changed(listOf(".foo1 {", "margin: 0;", "}"), emptyList()),
                DiffItem.Matched(listOf(".bar {", "margin: 0;", "}")),
                DiffItem.Changed(emptyList(), listOf(".foo1 {", "margin: 0;", "color: green;", "}"))))
    }

    @Test
    fun preferMatchingUniqueItems() {
        val commonNonUnique = listOf("aaa", "aaa", "bbb", "bbb", "ccc", "ccc")
        val left = commonNonUnique.plus("unique")
        val right = listOf("unique").plus(commonNonUnique)
        checkEqual(getDiff(left, right), listOf(
                DiffItem.Changed(commonNonUnique, emptyList()),
                DiffItem.Matched(listOf("unique")),
                DiffItem.Changed(emptyList(), commonNonUnique)
        ))
    }

    private fun checkEqual(actual: List<DiffItem<String>>, expected: List<DiffItem<String>>) {
        Assert.assertThat(formatChanges(actual), Is.`is`(formatChanges(expected)))
    }

    private fun getDiff(left: String, right: String): List<DiffItem<String>> {
        val diff = getDiff(toLines(left), toLines(right))
        println(formatChanges(diff))
        return diff
    }

    private fun getDiff(leftLines: List<String>, rightLines: List<String>): List<DiffItem<String>> {
        return ChangesBuilder().build(leftLines, rightLines, getMatches(leftLines, rightLines))
    }

    private fun toLines(str: String): List<String> {
        return str.split("\n")
    }

    private fun getMatches(left: Iterable<String>, right: Iterable<String>): List<Match> {
        return PatienceDiffAlgorithm().getMatches(left.toList(), right.toList())
    }
}
