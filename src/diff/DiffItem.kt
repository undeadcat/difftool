package diff

import utils.sequenceEquals

sealed class DiffItem<T> {
    class Changed<T>(val deletions: List<T>, val additions: List<T>) : DiffItem<T>() {
        override fun toString(): String {
            return "Deletions: [${deletions.joinToString(",")}], Additions: [${additions.joinToString(",")}]"
        }

        override fun equals(other: Any?): Boolean {
            when (other) {
                is Changed<*> -> return other.deletions.sequenceEquals(deletions)
                        && other.additions.sequenceEquals(additions)
                else -> return false
            }
        }

        override fun hashCode(): Int {
            var result = deletions.hashCode()
            result = 31 * result + additions.hashCode()
            return result
        }
    }

    class Matched<T>(val content: List<T>) : DiffItem<T>() {
        override fun toString(): String {
            return "Matched: [${content.joinToString(",")}]"
        }

        override fun equals(other: Any?): Boolean {
            when (other) {
                is Matched<*> -> return other.content.sequenceEquals(content)
                else -> return false
            }
        }

        override fun hashCode(): Int {
            return content.hashCode()
        }

    }

    fun isChanged(): Boolean {
        return when (this) {
            is Changed<*> -> true
            is Matched<*> -> false
        }
    }

}