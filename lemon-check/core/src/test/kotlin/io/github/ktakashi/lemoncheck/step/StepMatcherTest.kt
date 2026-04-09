package io.github.ktakashi.lemoncheck.step

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for [StepMatcher] pattern compilation and matching.
 */
class StepMatcherTest {
    private val matcher = StepMatcher()

    @Test
    fun `compile pattern without placeholders`() {
        val compiled = matcher.compile("I have pets")

        assertNotNull(matcher.match("I have pets", compiled))
        assertNull(matcher.match("I have no pets", compiled))
    }

    @Test
    fun `match int placeholder`() {
        val compiled = matcher.compile("I have {int} pets")

        val match = matcher.match("I have 5 pets", compiled)
        assertNotNull(match)
        assertEquals(1, match.size)
        assertEquals(5, match[0])
    }

    @Test
    fun `match negative int placeholder`() {
        val compiled = matcher.compile("the temperature is {int} degrees")

        val match = matcher.match("the temperature is -10 degrees", compiled)
        assertNotNull(match)
        assertEquals(1, match.size)
        assertEquals(-10, match[0])
    }

    @Test
    fun `match string placeholder with double quotes`() {
        val compiled = matcher.compile("the pet name is {string}")

        val match = matcher.match("the pet name is \"Fluffy\"", compiled)
        assertNotNull(match)
        assertEquals(1, match.size)
        assertEquals("Fluffy", match[0])
    }

    @Test
    fun `match string placeholder with single quotes`() {
        val compiled = matcher.compile("the pet name is {string}")

        val match = matcher.match("the pet name is 'Whiskers'", compiled)
        assertNotNull(match)
        assertEquals(1, match.size)
        assertEquals("Whiskers", match[0])
    }

    @Test
    fun `match word placeholder`() {
        val compiled = matcher.compile("the status is {word}")

        val match = matcher.match("the status is active", compiled)
        assertNotNull(match)
        assertEquals(1, match.size)
        assertEquals("active", match[0])
    }

    @Test
    fun `match float placeholder`() {
        val compiled = matcher.compile("the price is {float} dollars")

        val match = matcher.match("the price is 19.99 dollars", compiled)
        assertNotNull(match)
        assertEquals(1, match.size)
        assertEquals(19.99, match[0])
    }

    @Test
    fun `match any placeholder`() {
        val compiled = matcher.compile("the description is {any}")

        val match = matcher.match("the description is This is a long description with spaces", compiled)
        assertNotNull(match)
        assertEquals(1, match.size)
        assertEquals("This is a long description with spaces", match[0])
    }

    @Test
    fun `match multiple placeholders`() {
        val compiled = matcher.compile("I have {int} {word} named {string}")

        val match = matcher.match("I have 3 cats named \"Tom\"", compiled)
        assertNotNull(match)
        assertEquals(3, match.size)
        assertEquals(3, match[0])
        assertEquals("cats", match[1])
        assertEquals("Tom", match[2])
    }

    @Test
    fun `no match when pattern differs`() {
        val compiled = matcher.compile("I have {int} pets")

        assertNull(matcher.match("I own 5 pets", compiled))
        assertNull(matcher.match("I have many pets", compiled))
    }

    @Test
    fun `match with special regex characters in literal text`() {
        val compiled = matcher.compile($$"the price is ${int}.{int}")

        val match = matcher.match($$"the price is $19.99", compiled)
        assertNotNull(match)
        assertEquals(2, match.size)
        assertEquals(19, match[0])
        assertEquals(99, match[1])
    }
}
