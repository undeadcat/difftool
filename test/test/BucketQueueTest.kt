package test

import diff.BucketQueue
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test
import java.util.*

class BucketQueueTest {

    @Test
    fun simple() {
        val q = BucketQueue<String>(3)
        q.add("a", 3)
        q.add("b", 2)
        q.add("c", 1)
        Assert.assertThat(drain(q), Is.`is`(arrayListOf("c", "b", "a")))
    }

    @Test
    fun canUpdatePriority() {
        val q = BucketQueue<String>(6)
        q.add("a", 3)
        q.add("b", 1)
        q.delete("a")
        q.add("a", 5)
        Assert.assertThat(drain(q), Is.`is`(arrayListOf("b", "a")))
    }

    @Test
    fun samePriority_dequeueInFifoOrder() {
        val q = BucketQueue<Int>(3)
        q.add(1, 1)
        q.add(2, 1)
        q.add(3, 1)
        Assert.assertThat(drain(q), Is.`is`(arrayListOf(1, 2, 3)))
    }

    private fun <T> drain(queue: BucketQueue<T>): ArrayList<T> {
        val result = arrayListOf<T>()
        while (true) {
            val node = queue.dequeueMin()
            if (node != null)
                result.add(node)
            else return result
        }
    }
}