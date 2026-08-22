package dev.polurival.mexamen.data

import mexamen.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Продакшн-репозиторий: читает датасет из composeResources/files.
 */
@OptIn(ExperimentalResourceApi::class)
fun defaultQuestionRepository(): QuestionRepository = QuestionRepository(
    readQuestions = { Res.readBytes("files/questions.json") },
    readEmbeddings = { Res.readBytes("files/embeddings.bin") },
)
