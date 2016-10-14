package diff

import utils.ProgressIndicator
import utils.pairwise
import utils.sequenceEquals
import utils.throwIfInterrupted
import java.util.*

class PatienceDiffAlgorithm() {

    private val diffAlgorithm = DiffAlgorithm()
    fun <T> getMatches(left: List<T>, right: List<T>, progressIndicator: ProgressIndicator = ProgressIndicator.empty): List<Match> {
        if (left.isEmpty() || right.isEmpty())
            return emptyList()
        if (left.sequenceEquals(right))
            return left.mapIndexed { i, t -> Match(i, i) }

        Thread.currentThread().throwIfInterrupted()
        val matches = getPatienceMatchesOrEmpty(left, right, progressIndicator)

        return if (matches.isEmpty())
            diffAlgorithm.getMatches(left, right, { it -> it }, progressIndicator)
        else matches
    }

    private fun <T> getPatienceMatchesOrEmpty(left: List<T>, right: List<T>, progressIndicator: ProgressIndicator): List<Match> {
        val startMatch = Match(-1, -1)
        val endMatch = Match(left.size, right.size)

        fun getMatchesBetween(previousMatch: Match, nextMatch: Match, myProgressIndicator: ProgressIndicator): ArrayList<Match> {
            val results = arrayListOf<Match>()
            if (previousMatch != startMatch)
                results.add(previousMatch)
            val startIndexLeft = previousMatch.left + 1
            val startIndexRight = previousMatch.right + 1
            val newLeftItems = left.subList(startIndexLeft, nextMatch.left)
            val newRightItems = right.subList(startIndexRight, nextMatch.right)
            val newMatches = getMatches(newLeftItems, newRightItems, myProgressIndicator.createChild(nextMatch.right + nextMatch.left))
            results.addAll(newMatches.map { match -> Match(startIndexLeft + match.left, startIndexRight + match.right) })
            return results
        }

        val leftUnique = getUnique(left)
        val rightUnique = getUnique(right)
        val leftCandidates = getIntersection(leftUnique, rightUnique)
        val rightCandidates = getIntersection(rightUnique, leftUnique)
        if (leftCandidates.isEmpty()|| rightCandidates.isEmpty())
            return emptyList()
        progressIndicator.setMax(100)
        val matches = DiffAlgorithm().getMatches(leftCandidates, rightCandidates, { it.value }, progressIndicator.createChild(50))
                .map { match -> Match(leftCandidates[match.left].index, rightCandidates[match.right].index) }

        val nestedMatchesProgressIndicator = progressIndicator.createChild(100)
        nestedMatchesProgressIndicator.setMax(left.size + right.size.toLong())
        val nestedMatches = arrayListOf(startMatch)
                .plus(matches)
                .plus(endMatch)
                .pairwise()
                .flatMap { pair -> getMatchesBetween(pair.first, pair.second, nestedMatchesProgressIndicator) }
        nestedMatchesProgressIndicator.done()
        progressIndicator.done()
        return nestedMatches
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