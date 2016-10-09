package diff

import utils.throwIfInterrupted
import java.util.*

class DiffAlgorithm() {

    fun <T, TComparisonKey> getMatches(left: List<T>, right: List<T>, comparisonFunc: (T) -> TComparisonKey): List<Match> {

        val leftNodesSet = left.map(comparisonFunc).toSet()
        val rightNodesSet = right.map(comparisonFunc).toSet()
        val leftNonUnique = left.withIndex().filter { rightNodesSet.contains(comparisonFunc(it.value)) }
        val rightNonUnique = right.withIndex().filter { leftNodesSet.contains(comparisonFunc(it.value)) }
        val startNode = Node(0, 0)
        val end = Node(leftNonUnique.size, rightNonUnique.size)

        fun getNeighbors(node: Node): List<Node> {
            val result = arrayListOf<Node>()
            if (node.x < leftNonUnique.size) {
                result.add(Node(node.x + 1, node.y))
                if (node.y < rightNonUnique.size
                        && comparisonFunc(leftNonUnique[node.x].value) == comparisonFunc(rightNonUnique[node.y].value))
                    result.add(Node(node.x + 1, node.y + 1))
            }
            if (node.y < rightNonUnique.size) {
                result.add(Node(node.x, node.y + 1))
            }
            return result
        }

        val previousNodes = dijkstra(startNode, end, leftNonUnique.size + rightNonUnique.size, ::getNeighbors, { l, r -> getDistance(l, r) })
        val matches = toMatches(previousNodes, startNode, end)
        return matches.map { Match(leftNonUnique[it.left].index, rightNonUnique[it.right].index) }
    }

    private fun toMatches(previousNodes: HashMap<Node, Node>, start: Node, end: Node): List<Match> {
        val result = arrayListOf<Match>()
        var current = end
        while (current != start) {
            val previous = previousNodes[current]
            if (previous == null)
                break
            if (getDistance(previous, current) == 0)
                result.add(Match(previous.x, previous.y))
            current = previous
        }
        return result.reversed()
    }

    private fun getDistance(one: Node, two: Node): Int {
        if (Math.abs(two.x - one.x) == 1 && Math.abs(two.y - one.y) == 1)
            return 0
        return 1
    }

    private fun <T> dijkstra(start: T, end: T, maxPriority: Int, getNeighbors: (T) -> List<T>, getDistance: (T, T) -> Int): HashMap<T, T> {
        val distances = HashMap<T, Int>()
        val previous = HashMap<T, T>()
        val visited = HashSet<T>()
        val unvisited = BucketQueue<T>(maxPriority)
        unvisited.add(start, 0)
        distances[start] = 0
        var iterationCount = 0
        while (true) {
            if (iterationCount % 10000 == 0)
                Thread.currentThread().throwIfInterrupted()
            val current = unvisited.dequeueMin()
            if (current == null || current == end)
                return previous
            for (node in getNeighbors(current).filter { node -> !visited.contains(node) }) {
                val distance = getDistance(current, node)
                val oldDistance = distances[node]
                val toCurrent = distances[current]!!
                val viaCurrent = toCurrent + distance
                if (oldDistance == null || viaCurrent < oldDistance) {
                    distances[node] = viaCurrent
                    previous[node] = current
                    if (oldDistance != null)
                        unvisited.delete(current)
                    unvisited.add(node, viaCurrent)

                }
            }
            visited.add(current)
            iterationCount++
        }
    }

    data private class Node(val x: Int, val y: Int) {

    }
}


data class Match(val left: Int, val right: Int) {

}
