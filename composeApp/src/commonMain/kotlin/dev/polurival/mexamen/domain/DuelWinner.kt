package dev.polurival.mexamen.domain

/** Кто выиграл дуэль по итоговому [DuelScore]; NONE — модель ответила не на все вопросы. */
enum class DuelWinner { USER, MODEL, DRAW, NONE }
