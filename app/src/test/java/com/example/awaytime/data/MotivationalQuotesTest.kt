package com.example.awaytime.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotivationalQuotesTest {

    @Test
    fun `getRandomQuote returns non empty uppercase quote`() {
        val quote = MotivationalQuotes.getRandomQuote()
        assertNotNull(quote)
        assertTrue(quote.isNotBlank())
        assertEquals(quote, quote.uppercase())
    }

    @Test
    fun `consecutive calls do not return the exact same quote`() {
        var previous = MotivationalQuotes.getRandomQuote()
        for (i in 0 until 20) {
            val current = MotivationalQuotes.getRandomQuote()
            assertNotNull(current)
            assertTrue(current.isNotBlank())
            assertNotEquals(previous, current)
            previous = current
        }
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
