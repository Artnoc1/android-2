package com.toggl.settings.domain

import com.toggl.api.feedback.FeedbackApiClient
import com.toggl.models.domain.FeedbackData
import com.toggl.models.domain.PlatformInfo
import com.toggl.models.domain.User
import com.toggl.models.validation.ApiToken
import com.toggl.models.validation.Email
import com.toggl.settings.common.CoroutineTest
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The SendFeedbackEffect effect")
class SendFeedbackEffectTest : CoroutineTest() {

    @Test
    fun `should call the feedback api with the provided user email, message and platformInfo`() = runBlockingTest {
        val feedbackApi = mockk<FeedbackApiClient>()
        val feedbackDataBuilder = mockk<FeedbackDataBuilder>()
        val expectedFeedbackData = mockk<FeedbackData>()
        coEvery { feedbackApi.sendFeedback(any(), any(), any(), any()) }.returns(Unit)
        coEvery { feedbackDataBuilder.assembleFeedbackData() }.returns(expectedFeedbackData)
        val expectedMessage = "expected feedback message"
        val expectedPlatformInfo: PlatformInfo = mockk()
        val expectedEmail = Email.from("expected@email.com") as Email.Valid
        val expectedUser = User(
            id = 0,
            apiToken = ApiToken.from("12345678901234567890123456789012") as ApiToken.Valid,
            email = expectedEmail,
            name = "",
            defaultWorkspaceId = 1L
        )

        SendFeedbackEffect(
            expectedMessage, expectedUser, expectedPlatformInfo, feedbackDataBuilder, feedbackApi, dispatcherProvider
        ).execute()

        coVerify { feedbackApi.sendFeedback(expectedUser, expectedMessage, expectedPlatformInfo, expectedFeedbackData) }
    }

    @Test
    fun `should return the SetSendFeedbackError action with the right throwable when something goes wrong`() =
        runBlockingTest {
            val feedbackApi = mockk<FeedbackApiClient>()
            val feedbackDataBuilder = mockk<FeedbackDataBuilder>()
            coEvery { feedbackDataBuilder.assembleFeedbackData() }.returns(mockk())
            val expectedError = mockk<Exception>()
            coEvery { feedbackApi.sendFeedback(any(), any(), any(), any()) }.throws(expectedError)

            val resultAction = SendFeedbackEffect(
                "whatever", mockk(relaxed = true), mockk(), feedbackDataBuilder, feedbackApi, dispatcherProvider
            ).execute()

            resultAction.shouldBeTypeOf<SettingsAction.SetSendFeedbackError> { it.throwable shouldBe expectedError }
        }

    @Test
    fun `should return the FeedbackSent action with the right throwable when something goes wrong`() = runBlockingTest {
        val feedbackApi = mockk<FeedbackApiClient>()
        val feedbackDataBuilder = mockk<FeedbackDataBuilder>()
        coEvery { feedbackDataBuilder.assembleFeedbackData() }.returns(mockk())
        coEvery { feedbackApi.sendFeedback(any(), any(), any(), any()) }.returns(Unit)

        val resultAction = SendFeedbackEffect(
            "whatever", mockk(relaxed = true), mockk(), feedbackDataBuilder, feedbackApi, dispatcherProvider
        ).execute()

        resultAction.shouldBeTypeOf<SettingsAction.FeedbackSent>()
    }
}