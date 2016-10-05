package diff

import utils.groupSequentiallyBy

class ChangesBuilder() {
    fun <T> build(left: List<T>, right: List<T>, matches: List<Match>): List<DiffItem<T>> {

        fun getChangeOrNull(first: Match, second: Match): DiffItem<T>? {
            if (second.left - first.left > 1 || second.right - first.right > 1)
                return DiffItem.Changed(left.subList(first.left + 1, second.left), right.subList(first.right + 1, second.right))
            return null
        }


        val result = arrayListOf<DiffItem<T>?>()
        var previousMatch: Match = Match(-1, -1)
        for (match in matches) {
            result.add(getChangeOrNull(previousMatch, match))
            result.add(DiffItem.Matched(listOf(left[match.left])))
            previousMatch = match
        }
        result.add(getChangeOrNull(previousMatch, Match(left.size, right.size)))

        return mergeMatches(result.filterNotNull()).toList()
    }

    private fun <T> mergeMatches(values: Iterable<DiffItem<T>>): Iterable<DiffItem<T>> {
        return values.groupSequentiallyBy({ c -> c.isChanged() })
                .flatMap { grouping ->
                    if (!grouping.key)
                        listOf(DiffItem.Matched(grouping.values.map { it as DiffItem.Matched<T> }.flatMap { c -> c.content }))
                    else grouping.values
                }
    }
}

fun formatChanges(script: Iterable<DiffItem<String>>): String {
    return script.flatMap {
        when (it) {
            is DiffItem.Changed -> it.deletions.orEmpty().map { "-" + it }
                    .plus(it.additions.orEmpty().map { "+" + it })
            is DiffItem.Matched -> it.content
        }

    }.joinToString("\n")
}