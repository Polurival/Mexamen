package dev.polurival.mexamen.domain

/** Какую модель сервиса спрашивать: [apiName] — алиас в registry.json сервиса. */
enum class ModelVariant(val apiName: String) {
    TUNED("tuned"),
    BASE("base"),
}
