package com.toggl.api.serializers

import com.toggl.api.network.FeedbackBody
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FeedbackBody serializer tests")
class FeedbackBodySerializerTest {
    @Test
    fun `the FeedbackBody is properly serialized`() {
        val expectedEmail = "such@email.com"
        val expectedMessage = "expected message"
        val data = mapOf(
            "device" to "SomePhone",
            "some random key" to "some also random value"
        )
        val feedbackBody = FeedbackBody(expectedEmail, expectedMessage, data)
        val jsonSerializer = FeedbackBodySerializer()

        val json = jsonSerializer.serialize(feedbackBody, mockk(), mockk())

        json.toString() shouldBe "{\"email\":\"$expectedEmail\",\"message\":\"$expectedMessage\",\"data\":[{\"key\":\"device\",\"value\":\"${data["device"]}\"},{\"key\":\"some random key\",\"value\":\"${data["some random key"]}\"}]}"
    }
}