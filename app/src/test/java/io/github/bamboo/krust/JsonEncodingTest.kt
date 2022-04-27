package io.github.bamboo.krust

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class JsonEncodingTest {

    @Test
    fun `commands use internally tagged encoding`() {
        val value: Command = Command.Ping("foo")
        val string = Json.encodeToString(value)
        assertEquals(
            """{"type":"Ping","payload":"foo"}""",
            string
        )
        assertEquals(
            value,
            Json.decodeFromString<Command>(string)
        )
    }
}