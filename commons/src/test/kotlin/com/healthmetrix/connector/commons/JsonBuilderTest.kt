package com.healthmetrix.connector.commons

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JsonBuilderTest {

    @Test
    fun `ensure empty object is created`() {
        assertThat(json {}.toString()).isEqualTo("{}")
    }

    @Test
    fun `ensure empty array is created`() {
        assertThat(array {}.toString()).isEqualTo("[]")
    }

    @Test
    fun `ensure that nested json object is created`() {
        assertThat(
            json {
                "key" to json {}
            }.toString(),
        ).isEqualTo("{\"key\":{}}")
    }

    @Test
    fun `ensure that an array can have objects`() {
        assertThat(
            array {
                json {}
                json {}
            }.toString(),
        ).isEqualTo("[{},{}]")
    }

    @Test
    fun `ensure a structure similar to mailjet request body is possible`() {
        assertThat(
            array {
                json {
                    "from" to json {
                        "address" to "email@example.com"
                    }
                    "message" to "hello world"
                }
            }.toString(),
        ).isEqualTo("[{\"from\":{\"address\":\"email@example.com\"},\"message\":\"hello world\"}]")
    }
}
