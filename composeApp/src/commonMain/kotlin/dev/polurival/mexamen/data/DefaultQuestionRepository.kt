package dev.polurival.mexamen.data

import mexamen.composeapp.generated.resources.Res

/**
 * Продакшн-репозиторий: читает датасет из composeResources/files.
 */
fun defaultQuestionRepository(): QuestionRepository = QuestionRepository(
    readQuestions = { Res.readBytes("files/questions.json") },
    readEmbeddings = { Res.readBytes("files/embeddings.bin") },
)
