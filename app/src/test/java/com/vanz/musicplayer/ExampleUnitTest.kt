package com.vanz.musicplayer

import com.vanz.musicplayer.data.model.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testLrcParser_validTimestamp() {
        val sampleLrc = """
            [00:12.50] Hello world
            [00:15.80] This is Vanz Music
            [01:05.12] Synchronized lyrics line
        """.trimIndent()

        val parsed = LrcParser.parse(sampleLrc)
        assertEquals(3, parsed.size)
        assertEquals(12500L, parsed[0].timestampMs)
        assertEquals("Hello world", parsed[0].text)
        assertEquals(15800L, parsed[1].timestampMs)
        assertEquals("This is Vanz Music", parsed[1].text)
        assertEquals(65120L, parsed[2].timestampMs)
        assertEquals("Synchronized lyrics line", parsed[2].text)
    }

    @Test
    fun testLrcParser_emptyString() {
        val parsed = LrcParser.parse(null)
        assertTrue(parsed.isEmpty())
    }
}
