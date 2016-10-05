package utils

import java.util.*


fun <T> Iterable<T>.pairwise(): Iterable<Pair<T, T>> {
    val count = this.count()
    if (count < 2)
        throw Exception("Sequence contained less than two elements: [$count]")
    val result = ArrayList<Pair<T, T>>()
    var prev = this.first()
    for (el in this.drop(1)) {
        result.add(Pair(prev, el))
        prev = el
    }
    return result
}

fun <TKey, TValue> Iterable<TValue>.groupSequentiallyBy(selector: (TValue) -> TKey)
        : ArrayList<Grouping<TKey, TValue>> {
    val result = arrayListOf<Grouping<TKey, TValue>>()
    for (element in this) {
        val key = selector(element)
        if (result.isEmpty() || result.last().key != key)
            result.add(Grouping(key, arrayListOf(element)))
        else result.last().values.add(element)
    }
    return result
}

data class Grouping<out TKey, TValue>(val key: TKey, val values: ArrayList<TValue>) {

}

fun <T> repeatElement(element: T, count: Int): ArrayList<T> {
    val result = arrayListOf<T>()
    for (i in 0 until count)
        result.add(element)
    return result
}

fun <T> Iterable<T>.sequenceEquals(right: Iterable<T>): Boolean {
    return this.count() == right.count()
            && this.zip(right, { l, r -> l == r })
            .all({ c -> c })
}
