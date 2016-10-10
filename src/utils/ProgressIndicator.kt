package utils

open class ProgressIndicator(private val percentReached: ((Int) -> Unit) = {}) {
    var value = 0L
    private var maxValue: Long? = null
    fun setMax(maxValue: Long) {
        this.maxValue = maxValue
        this.value = 0
    }

    open fun report(value: Long) {
        val myMaxValue = maxValue
        if (myMaxValue == null)
            throw Exception("Max value is not set")
        if (value > myMaxValue)
            return
        if (value < this.value)
            throw Exception("Attempted to set value $value below existing ${this.value}")
        val previousValue = this.value
        this.value = value
        if (myMaxValue == 0L) {
            percentReached(100)
        } else {
            val previousPercent = (previousValue / myMaxValue.toDouble() * 100).toInt()
            val currentPercent = (value / myMaxValue.toDouble() * 100).toInt()
            if (currentPercent != previousPercent)
                percentReached(currentPercent)
        }
    }

    open fun done() {
        val myMaxValue = maxValue
        if (myMaxValue == null)
            throw Exception("Max value is not set")
        report(myMaxValue)
    }

    open fun createChild(childMaxValue: Int): ProgressIndicator {
        val initialValue = value
        val onePercent = (childMaxValue - initialValue).toDouble() / 100
        return ProgressIndicator(percentReached = {
            percent ->
            report(initialValue + (onePercent * percent).toLong())
        })
    }

    private class EmptyProgressIndicator : ProgressIndicator() {
        override fun done() {

        }

        override fun report(value: Long) {

        }

        override fun createChild(childMaxValue: Int): ProgressIndicator {
            return empty
        }
    }

    companion object Statics {
        val empty: ProgressIndicator = EmptyProgressIndicator()
    }
}