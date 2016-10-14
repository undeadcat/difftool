package diff

import java.util.*

class BucketQueue<T>(maxPriority: Int) {
    private val buckets = Array(maxPriority + 1, { ArrayDeque<T>() })
    var minPriority: Int? = null

    fun add(item: T, priority: Int) {
        val bucket = buckets[priority]
        bucket.addLast(item)
        val theMinPriority = minPriority
        if (theMinPriority == null || priority < theMinPriority)
            minPriority = priority
    }

    fun delete(priority: Int, item: T) {
        val bucket = buckets[priority]
        bucket.remove(item)
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
        if (bucket.isEmpty())
            updatePriority()
        return node
    }

    private fun updatePriority() {
        val indexOfFirst = buckets.indexOfFirst { bucket -> !bucket.isEmpty() }
        minPriority = if (indexOfFirst < 0) null else indexOfFirst
    }
}