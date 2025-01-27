package events.boudicca.search.query.evaluator

import events.boudicca.SemanticKeys
import events.boudicca.search.service.query.evaluator.EvaluatorUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EvaluatorUtilTest {

    @Test
    fun testEmpty() {
        Assertions.assertEquals(
            0.0,
            EvaluatorUtil.getDuration(
                mapOf(
                )
            )
        )
    }

    @Test
    fun testNoStart() {
        Assertions.assertEquals(
            0.0,
            EvaluatorUtil.getDuration(
                mapOf(
                    SemanticKeys.ENDDATE to "2024-05-31T01:00:00Z",
                )
            )
        )
    }

    @Test
    fun testNoEnd() {
        Assertions.assertEquals(
            0.0,
            EvaluatorUtil.getDuration(
                mapOf(
                    SemanticKeys.STARTDATE to "2024-05-31T01:00:00Z",
                )
            )
        )
    }

    @Test
    fun testSimple() {
        Assertions.assertEquals(
            1.0,
            EvaluatorUtil.getDuration(
                mapOf(
                    SemanticKeys.STARTDATE to "2024-05-31T00:00:00Z",
                    SemanticKeys.ENDDATE to "2024-05-31T01:00:00Z",
                )
            )
        )
    }

    @Test
    fun testNegative() {
        Assertions.assertEquals(
            -1.0,
            EvaluatorUtil.getDuration(
                mapOf(
                    SemanticKeys.STARTDATE to "2024-05-31T01:00:00Z",
                    SemanticKeys.ENDDATE to "2024-05-31T00:00:00Z",
                )
            )
        )
    }

    @Test
    fun testFraction() {
        Assertions.assertEquals(
            0.5,
            EvaluatorUtil.getDuration(
                mapOf(
                    SemanticKeys.STARTDATE to "2024-05-31T00:00:00Z",
                    SemanticKeys.ENDDATE to "2024-05-31T00:30:00Z",
                )
            )
        )
    }

}