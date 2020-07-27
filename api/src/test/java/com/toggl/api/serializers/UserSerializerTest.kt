package com.toggl.api.serializers

import com.google.gson.JsonParser
import com.toggl.models.domain.User
import com.toggl.models.validation.ApiToken
import com.toggl.models.validation.Email
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("The User deserializer")
class UserSerializerTest {

    @Test
    fun `properly deserializes the user`() {
        val json = JsonParser.parseString(validUserJson).asJsonObject
        val jsonSerializer = UserDeserializer()
        val user = jsonSerializer.deserialize(json, mockk(), mockk())

        user shouldBe User(
            id = 9000,
            apiToken = ApiToken.from("1971800d4d82861d8f2c1651fea4d212") as ApiToken.Valid,
            email = Email.from("johnt@swift.com") as Email.Valid,
            name = "John Swift",
            defaultWorkspaceId = 777
        )
    }

    companion object {
        private const val validUserJson = "{\"id\":9000,\"api_token\":\"1971800d4d82861d8f2c1651fea4d212\",\"default_workspace_id\":777,\"email\":\"johnt@swift.com\",\"fullname\":\"John Swift\",\"beginning_of_week\":0,\"language\":\"en_US\",\"image_url\":\"https://www.toggl.com/system/avatars/9000/small/open-uri20121116-2767-b1qr8l.png\",\"timezone\":\"Europe/Zagreb\",\"updated_at\":\"2013-03-06T12:18:42+00:00\"}"
        private const val validUserJsonWithNoDefaultWorkspace = "{\"id\":9000,\"api_token\":\"1971800d4d82861d8f2c1651fea4d212\",\"default_workspace_id\":null,\"email\":\"johnt@swift.com\",\"fullname\":\"John Swift\",\"beginning_of_week\":0,\"language\":\"en_US\",\"image_url\":\"https://www.toggl.com/system/avatars/9000/small/open-uri20121116-2767-b1qr8l.png\",\"timezone\":\"Europe/Zagreb\",\"updated_at\":\"2013-03-06T12:18:42+00:00\"}"
    }
}