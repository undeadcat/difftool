package diff

import utils.pairwise
import utils.sequenceEquals
import java.util.*

class PatienceDiffAlgorithm() {

    private val diffAlgorithm = DiffAlgorithm()
    fun <T> getMatches(left: List<T>, right: List<T>): List<Match> {
        if (left.isEmpty() || right.isEmpty())
            return emptyList()
        if (left.sequenceEquals(right))
            return left.mapIndexed { i, t -> Match(i, i) }

        val matches = getPatienceMatchesOrEmpty(left, right)

        if (matches.isEmpty())
            return diffAlgorithm.getMatches(left, right, { it -> it })
        else return matches
    }

    private fun <T> getPatienceMatchesOrEmpty(left: List<T>, right: List<T>): List<Match> {
        val startMatch = Match(-1, -1)
        val endMatch = Match(left.size, right.size)

        fun getMatchesBetween(previousMatch: Match, nextMatch: Match): ArrayList<Match> {
            val results = arrayListOf<Match>()
            if (previousMatch != startMatch)
                results.add(previousMatch)
            val startIndexLeft = previousMatch.left + 1
            val startIndexRight = previousMatch.right + 1
            val newLeftItems = left.subList(startIndexLeft, nextMatch.left)
            val newRightItems = right.subList(startIndexRight, nextMatch.right)
            val newMatches = getMatches(newLeftItems, newRightItems)
            results.addAll(newMatches.map { match -> Match(startIndexLeft + match.left, startIndexRight + match.right) })
            return results
        }

        val leftUnique = getUnique(left)
        val rightUnique = getUnique(right)
        val leftCandidates = getIntersection(leftUnique, rightUnique)
        val rightCandidates = getIntersection(rightUnique, leftUnique)
        val matches = DiffAlgorithm().getMatches(leftCandidates, rightCandidates, { it.value })
                .map { match -> Match(leftCandidates[match.left].index, rightCandidates[match.right].index) }
        if (matches.isEmpty())
            return matches

        return arrayListOf(startMatch)
                .plus(matches)
                .plus(endMatch)
                .pairwise()
                .flatMap { pair -> getMatchesBetween(pair.first, pair.second) }
    }

    private fun <T> getIntersection(thisSide: Map<T, IndexedValue<T>>, opposite: Map<T, IndexedValue<T>>): List<IndexedValue<T>> {
        return thisSide
                .filter { opposite.containsKey(it.key) }
                .values.sortedBy { it.index }.toList()
    }

    private fun <T> getUnique(source: List<T>): Map<T, IndexedValue<T>> {
        return source.withIndex()
                .groupBy { c -> c.value }
                .filter { c -> c.value.size == 1 }
                .mapValues { c -> c.value.single() }
    }
}