package com.toggl.timer.startedit.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class TextUtilsKtTest {

    @ParameterizedTest
    @MethodSource("queryPickerTheory")
    fun `findTokenAndQueryMatchesForAutocomplete selects the right query`(queryTestData: QueryTestData) {
        val query = queryTestData.query
        val tokens = queryTestData.tokens
        val cursorPosition = queryTestData.cursorPosition

        val result = query.findTokenAndQueryMatchesForAutocomplete(tokens, cursorPosition)

        result shouldBe (queryTestData.expectedToken to queryTestData.expectedQuery)
    }

    companion object {
        @JvmStatic
        fun queryPickerTheory(): Stream<QueryTestData> = Stream.of(
            QueryTestData("012345", charArrayOf('@'), 0, null, "012345"),
            QueryTestData("01234 @7", charArrayOf('@'), 0, null, "01234 @7"),
            QueryTestData("01234 @7", charArrayOf('@'), 6, null, "01234 @7"),
            QueryTestData("01234 @7", charArrayOf('@'), 7, '@', "7"),
            QueryTestData("01234 @7", charArrayOf('@'), 8, '@', "7"),
            QueryTestData("01234 @78901234", charArrayOf('@'), 11, '@', "78901234"),
            QueryTestData("01234 @7", charArrayOf('@'), 9, '@', "7"),
            QueryTestData("01234@#7890", charArrayOf('@', '#'), 6, null, "01234@#7890"),
            QueryTestData("01234@#7890", charArrayOf('@', '#'), 11, null, "01234@#7890"),
            QueryTestData("01234 @23 #7890", charArrayOf('@', '#'), 9, '@', "23 #7890"),
            QueryTestData("01234 @23 #7890", charArrayOf('@', '#'), 12, '#', "7890"),
            QueryTestData("01234 @#7890", charArrayOf('@', '#'), 9, '@', "#7890")
        )
    }

    data class QueryTestData(
        val query: String,
        val tokens: CharArray,
        val cursorPosition: Int,
        val expectedToken: Char?,
        val expectedQuery: String
    )
}