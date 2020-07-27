package com.toggl.timer.startedit.domain

import com.toggl.models.domain.Tag
import com.toggl.repository.interfaces.TagRepository
import com.toggl.timer.common.CoroutineTest
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
internal class CreateTagEffectTest : CoroutineTest() {

    @Test
    fun `it should call the repository to create a tag`() = runBlockingTest {
        val tagRepository: TagRepository = mockk()
        coEvery { tagRepository.createTag(any()) } returns mockk()
        val effect = CreateTagEffect(dispatcherProvider, tagRepository, "expected tag name", 1)

        effect.execute()

        coVerify(exactly = 1) {
            tagRepository.createTag(match { it.name == "expected tag name" && it.workspaceId == 1L })
        }
    }

    @Test
    fun `it should fire an StartEditAction TagCreated with the tag that was created`() = runBlockingTest {
        val tagRepository: TagRepository = mockk()
        val mockTag: Tag = mockk()
        coEvery { tagRepository.createTag(any()) } returns mockTag
        val effect = CreateTagEffect(dispatcherProvider, tagRepository, "expected tag name", 1)

        val result = effect.execute()

        result shouldBe StartEditAction.TagCreated(mockTag)
    }
}