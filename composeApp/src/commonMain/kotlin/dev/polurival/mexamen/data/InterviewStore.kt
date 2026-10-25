package dev.polurival.mexamen.data

import kotlinx.serialization.Serializable

@Serializable
data class InterviewQuestion(
    val id: String,
    val question: String,
    val category: String
)

val interviewQuestions = listOf(
    // Información Personal y Antecedentes
    InterviewQuestion("p1", "¿Cuántos años tiene?", "Información Personal y Antecedentes"),
    InterviewQuestion("p2", "¿De dónde es originario?", "Información Personal y Antecedentes"),
    InterviewQuestion("p3", "¿Cuál es su estado civil?", "Información Personal y Antecedentes"),
    InterviewQuestion("p4", "¿A qué se dedicaba antes y a qué se dedica ahora?", "Información Personal y Antecedentes"),

    // Inmigración y Residencia
    InterviewQuestion("i1", "¿Cuándo llegó a México?", "Inmigración y Residencia"),
    InterviewQuestion("i2", "¿Cuánto tiempo / Cuántos años lleva viviendo en México?", "Inmigración y Residencia"),
    InterviewQuestion("i3", "¿En qué ciudad vive?", "Inmigración y Residencia"),
    InterviewQuestion("i4", "¿Qué trajo con usted a México?", "Inmigración y Residencia"),

    // Motivación y Opiniones
    InterviewQuestion("m1", "¿Por qué quiere obtener la nacionalidad mexicana?", "Motivación y Opiniones"),
    InterviewQuestion("m2", "¿Qué le gusta o no le gusta de México?", "Motivación y Opiniones"),

    // Conocimiento Cultural y Integración
    InterviewQuestion("c1", "¿Qué lugares ha visitado en México?", "Conocimiento Cultural y Integración"),
    InterviewQuestion("c2", "¿Conoce algún \"Pueblo Mágico\"?", "Conocimiento Cultural y Integración"),
    InterviewQuestion("c3", "¿Qué tradiciones conoce?", "Conocimiento Cultural y Integración"),
    InterviewQuestion("c4", "¿Qué festividades conoce?", "Conocimiento Cultural y Integración"),
    InterviewQuestion("c5", "¿Qué platillos mexicanos conoce?", "Conocimiento Cultural y Integración"),
    InterviewQuestion("c6", "¿Qué gestos mexicanos conoce?", "Conocimiento Cultural y Integración")
)

interface InterviewStore {
    suspend fun readAllAnswers(): Map<String, String> // questionId to answer
    suspend fun saveAnswer(questionId: String, answer: String)
}

expect fun defaultInterviewStore(): InterviewStore
