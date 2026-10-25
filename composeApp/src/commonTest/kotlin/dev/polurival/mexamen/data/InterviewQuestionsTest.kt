package dev.polurival.mexamen.data

import kotlin.test.Test
import kotlin.test.assertTrue

class InterviewQuestionsTest {
    /** Регрессия #11: в испанском тексте вопросов интервью не должно быть кириллицы (напр. «и» вместо «y»). */
    @Test
    fun interviewQuestionsContainNoCyrillicCharacters() {
        val withCyrillic = interviewQuestions.filter { q ->
            (q.question + q.category).any { it in 'Ѐ'..'ӿ' }
        }
        assertTrue(withCyrillic.isEmpty(), "Cyrillic found in: ${withCyrillic.map { it.id }}")
    }
}
