package diff

import java.util.*

class BucketQueue<T>(maxPriority: Int) {
    private val buckets = Array(maxPriority + 1, { i -> LinkedList<T>() })
    private val itemToNode = HashMap<T, Node<T>>()
    var minPriority: Int? = null

    fun add(item: T, priority: Int) {
        val bucket = buckets[priority]
        val node = Node(null, null, item, priority)
        itemToNode[item] = node
        bucket.addLast(node)
        val theMinPriority = minPriority
        if (theMinPriority == null || priority < theMinPriority)
            minPriority = priority
    }

    fun delete(item: T) {
        val node = itemToNode[item]
        if (node == null)
            throw Exception("Item ${item} is not in queue")
        val bucket = buckets[node.priority]
        bucket.remove(node)
        itemToNode.remove(item)
        if (bucket.isEmpty())
            updatePriority()
    }

    fun dequeueMin(): T? {
        val thePriority = minPriority
        if (thePriority == null)
            return null
        val bucket = buckets[thePriority]
        if (bucket.isEmpty())
            return null
        val node = bucket.first()
        bucket.remove(node)
        itemToNode.remove(node.element)
        if (bucket.isEmpty())
            updatePriority()
        return node.element
    }

    private fun updatePriority() {
        val indexOfFirst = buckets.indexOfFirst { bucket -> !bucket.isEmpty() }
        minPriority = if (indexOfFirst < 0) null else indexOfFirst
    }

    private class LinkedList<T> {
        var head: Node<T>? = null
        private var tail: Node<T>? = null

        fun first(): Node<T> {
            val theHead = head
            if (theHead == null)
                throw Exception("List is empty")
            return theHead
        }

        fun isEmpty(): Boolean {
            return head == null && tail == null
        }

        fun addLast(node: Node<T>) {
            node.previous = tail
            if (isEmpty()) {
                head = node
                tail = node
            } else {
                tail!!.next = node
                tail = node

            }
        }

        fun remove(node: Node<T>) {
            val previous = node.previous
            val next = node.next
            if (previous != null)
                previous.next = next
            else
                head = next
            if (next != null)
                next.previous = previous
            else
                tail = previous
        }

    }

    data private class Node<T>(var previous: Node<T>?, var next: Node<T>?, val element: T, val priority: Int) {

    }
}
