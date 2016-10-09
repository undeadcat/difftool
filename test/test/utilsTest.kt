package test

import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test
import utils.splitByCount

class utilsTest {

    @Test
    fun test_splitStringByCount() {
        val exactlyEquals = "123123123"
        Assert.assertThat(splitByCount(exactlyEquals, 3), Is.`is`(listOf("123", "123", "123")))
        val remainingAtEnd = "12312312"
        Assert.assertThat(splitByCount(remainingAtEnd, 3), Is.`is`(listOf("123", "123", "12")))
        val lessThanLimit = "12"
        Assert.assertThat(splitByCount(lessThanLimit, 3), Is.`is`(listOf("12")))
    }
}